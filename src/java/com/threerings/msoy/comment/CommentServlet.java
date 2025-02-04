//
// $Id$

package com.threerings.msoy.comment.server;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.inject.Inject;

import com.samskivert.util.StringUtil;

import com.samskivert.depot.DatabaseException;

import com.threerings.gwt.util.ExpanderResult;
import com.threerings.web.gwt.ServiceException;

import com.threerings.msoy.comment.data.all.Comment;
import com.threerings.msoy.comment.data.all.CommentType;
import com.threerings.msoy.comment.gwt.CommentService;
import com.threerings.msoy.comment.server.persist.CommentRecord;
import com.threerings.msoy.comment.server.persist.CommentRepository;
import com.threerings.msoy.game.server.persist.GameInfoRecord;
import com.threerings.msoy.game.server.persist.MsoyGameRepository;
import com.threerings.msoy.item.server.ItemLogic;
import com.threerings.msoy.item.server.persist.CatalogRecord;
import com.threerings.msoy.item.server.persist.ItemRecord;
import com.threerings.msoy.item.server.persist.ItemRepository;
import com.threerings.msoy.notify.server.MsoyNotificationManager;
import com.threerings.msoy.person.gwt.FeedMessage;
import com.threerings.msoy.person.gwt.FeedMessageType;
import com.threerings.msoy.person.server.FeedLogic;
import com.threerings.msoy.room.data.MsoySceneModel;
import com.threerings.msoy.room.server.persist.MsoySceneRepository;
import com.threerings.msoy.room.server.persist.SceneRecord;
import com.threerings.msoy.server.StatLogic;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.underwire.server.SupportLogic;
import com.threerings.msoy.web.gwt.Activity;
import com.threerings.msoy.web.gwt.MemberCard;
import com.threerings.msoy.web.gwt.Pages;
import com.threerings.msoy.web.gwt.ServiceCodes;
import com.threerings.msoy.web.server.MsoyServiceServlet;

import static com.threerings.msoy.Log.log;

/**
 * Provides the server implementation of {@link CommentService}.
 */
public class CommentServlet extends MsoyServiceServlet
    implements CommentService
{
    // from interface CommentService
    public ExpanderResult<Activity> loadComments (
        CommentType etype, int eid, long beforeTime, int count)
        throws ServiceException
    {
        // Sanity check
        if (count > 100) {
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }

        // no authentication required to view comments
        return _feedLogic.loadComments(etype, eid, beforeTime, count);
    }

    // from interface CommentService
    public ExpanderResult<Activity> loadReplies (
        CommentType etype, int eid, long replyTo, long beforeTime, int count)
        throws ServiceException
    {
        // Sanity check
        if (count > 100) {
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }

        return _feedLogic.loadReplies(etype, eid, replyTo, beforeTime, count);
    }

    // from interface CommentService
    public Comment postComment (CommentType etype, int eid, long replyTo, String text)
        throws ServiceException
    {
        MemberRecord mrec = requireValidatedUser();

        // validate the entity type and id (sort of; we can't *really* validate the id without a
        // bunch of entity specific befuckery which I don't particularly care to do)
        if (!etype.isValid() || eid == 0) {
            log.warning("Refusing to post comment on illegal entity", "type", etype, "id", eid,
                        "who", mrec.who(), "text",  StringUtil.truncate(text, 40, "..."));
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }

        // find out the owner id of and the entity name for the entity that was commented on
        int ownerId = 0;
        String entityName = null;

        FeedMessageType feedType = null;
        List<Object> feedArgs = null;

        // if this is a comment on a user room, post a self feed message
        if (etype.forRoom()) {
            SceneRecord scene = _sceneRepo.loadScene(eid);
            if (scene.ownerType == MsoySceneModel.OWNER_TYPE_MEMBER) {
                ownerId = scene.ownerId;
                entityName = scene.name;
            }
            feedType = FeedMessageType.SELF_ROOM_COMMENT;
            feedArgs = ImmutableList.of(scene.sceneId, scene.name, scene.getSnapshotThumb());

        } else if (etype.forProfileWall()) {
            MemberRecord wallOwner = _memberRepo.loadMember(eid);
            ownerId = eid;
            
            if(! (_memberRepo.isMuted(wallOwner.memberId, mrec.memberId)) ) { //Check if the player is NOT muted by the muter
            feedType = FeedMessageType.SELF_PROFILE_COMMENT;
            feedArgs = ImmutableList.of((Object) ownerId, wallOwner.name);
            } else {
            return null;
            }

        // comment on an item
        } else  if (etype.isItemType()) {
            try {
                ItemRepository<?> repo = _itemLogic.getRepository(etype.toItemType());
                CatalogRecord listing = repo.loadListing(eid, true);
                if (listing != null) {
                    ItemRecord item = listing.item;
                    if (item != null) {
                        ownerId = item.creatorId;
                        entityName = item.name;

                        feedType = FeedMessageType.SELF_ITEM_COMMENT;
                        feedArgs = ImmutableList.of(
                            item.getType().toByte(), listing.catalogId, item.name,
                            item.getThumbMediaDesc());
                    }
                }

            } catch (ServiceException se) {
                // this is merely a failure to send the notification, don't let this exception
                // make it back to the commenter
                log.warning("Unable to locate repository for apparent item type", "type", etype);

            } catch (DatabaseException de) {
                log.warning("Unable to load comment target item", "type", etype, "id", eid, de);
            }
        } else if (etype.forGame()) {
            GameInfoRecord game = _msoyGameRepo.loadGame(eid);
            if (game != null) {
                ownerId = game.creatorId;
                entityName = game.name;
                feedType = FeedMessageType.SELF_GAME_COMMENT;
                feedArgs = ImmutableList.of(eid, game.name, game.getThumbMedia());
            }
        }

        // If this is a reply, send feed messages to everyone involved
        // TODO(bruno): Send flash notifications too
        if (replyTo != 0) {
            CommentRecord subject = _commentRepo.loadComment(etype.toByte(), eid, replyTo);

            if (subject.memberId != mrec.memberId) {
                // Notify the original subject poster of a reply to their comment
                _feedLogic.publishSelfMessage(subject.memberId, mrec.memberId, feedType,
                    Iterables.toArray(Iterables.concat(feedArgs,
                        Collections.singleton(FeedMessage.COMMENT_REPLIED)),
                        Object.class));
            }

            Set<CommentRecord> recentReplies = _commentRepo.loadReplies(
                etype.toByte(), eid, replyTo, System.currentTimeMillis(), 5).replies;
            Set<Integer> notified = Sets.newHashSet();
            for (CommentRecord reply : recentReplies) {
                // Notify other repliers in this thread
                boolean shouldNotify = reply.memberId != mrec.memberId
                    && reply.memberId != subject.memberId;
                if (shouldNotify && notified.add(reply.memberId)) {
                    _feedLogic.publishSelfMessage(reply.memberId, mrec.memberId, feedType,
                        Iterables.toArray(Iterables.concat(feedArgs,
                            Collections.singleton(FeedMessage.COMMENT_FOLLOWED_UP)),
                            Object.class));
                }
            }
        }

        // notify the item creator that a comment was made
        if (ownerId > 0 && ownerId != mrec.memberId) {
            if (!etype.forProfileWall()) {
                _feedLogic.publishSelfMessage(ownerId, mrec.memberId, feedType, feedArgs.toArray());
            }
            _notifyMan.notifyEntityCommented(ownerId, etype, eid, entityName);
        }

        // record the comment to the data-ma-base
        CommentRecord crec = _commentRepo.postComment(
            etype.toByte(), eid, replyTo, mrec.memberId, text);

        // convert the record to a runtime record to return to the caller
        Map<Integer, MemberCard> map = Maps.newHashMap();
        map.put(mrec.memberId, _memberRepo.loadMemberCard(mrec.memberId, false));
        return crec.toComment(map);
    }

    // from interface CommentService
    public int rateComment (CommentType etype, int eid, long posted, boolean rating)
        throws ServiceException
    {
        MemberRecord mrec = requireValidatedUser();
        return _commentRepo.rateComment(etype.toByte(), eid, posted, mrec.memberId, rating);
    }

    // from interface CommentService
    public int deleteComments (CommentType etype, int eid, Collection<Long> stamps)
        throws ServiceException
    {
        MemberRecord mrec = requireAuthedUser();
        int deleted = 0;
        for (Long posted : stamps) {
            // if we're not support personnel, ensure that we are the poster of this comment
            if (!mrec.isSupport()) {
                CommentRecord record = _commentRepo.loadComment(etype.toByte(), eid, posted);
                if (record == null ||
                        !Comment.canDelete(etype, eid, record.memberId, mrec.memberId)) {
                    continue;
                }
            }
            _commentRepo.deleteComment(etype.toByte(), eid, posted);
            deleted ++;
        }
        return deleted;
    }

    // from interface CommentService
    public void complainComment (String subject, CommentType etype, int eid, long posted)
        throws ServiceException
    {
        MemberRecord mrec = requireValidatedUser();
        CommentRecord record = _commentRepo.loadComment(etype.toByte(), eid, posted);
        if (record == null) {
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }

        String link;
		if (etype.isItemType()) {
            link = Pages.SHOP.makeURL("l", etype.toByte(), eid);
        } else if (etype.forRoom()) {
            link = Pages.ROOMS.makeURL("room", eid);
        } else if (etype.forProfileWall()) {
            link = Pages.PEOPLE.makeURL(eid);
        } else if (etype.forGame()) {
            link = Pages.GAMES.makeURL("d", eid, "c");
        } else {
            link = null;
        }

        String text = "[" + new Date(posted) + "]\n" + record.text;
        _supportLogic.addMessageComplaint(
            mrec.getName(), record.memberId, text, subject, link);
    }

    // our dependencies
    @Inject protected CommentRepository _commentRepo;
    @Inject protected FeedLogic _feedLogic;
    @Inject protected ItemLogic _itemLogic;
    @Inject protected MsoyGameRepository _msoyGameRepo;
    @Inject protected MsoyNotificationManager _notifyMan;
    @Inject protected MsoySceneRepository _sceneRepo;
    @Inject protected StatLogic _statLogic;
    @Inject protected SupportLogic _supportLogic;
}
