//
// $Id$

package com.threerings.msoy.fora.data;

import java.util.Date;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.threerings.msoy.data.all.MemberName;

/**
 * Contains information on a forum thread.
 */
public class ForumThread
    implements IsSerializable
{
    /** The length in characters of the longest allowable subject. */
    public static final int MAX_SUBJECT_LENGTH = 128;

    /** A flag indicating this is an announcement thread. */
    public static final int FLAG_ANNOUNCEMENT = 0x1 << 0;

    /** A flag indicating this is a sticky thread. */
    public static final int FLAG_STICKY = 0x1 << 1;

    /** A flag indicating this is a locked thread. */
    public static final int FLAG_LOCKED = 0x2 << 1;

    /** A unique identifier for this forum thread. */
    public int threadId;

    /** The id of the group to which this forum thread belongs. */
    public int groupId;

    /** Flags indicating attributes of this thread: {@link #FLAG_ANNOUNCEMENT}, etc. */
    public int flags;

    /** The subject of this thread. */
    public String subject;

    /** The id of the most recent message posted to this thread. */
    public int mostRecentPostId;

    /** The time at which the most recent message was posted to this thread. */
    public Date mostRecentPostTime;

    /** The author of the message most recently posted to this thread. */
    public MemberName mostRecentPoster;

    /**
     * Returns true if this is an announcement thread.
     */
    public boolean isAnnouncement ()
    {
        return (flags & FLAG_ANNOUNCEMENT) != 0;
    }

    /**
     * Returns true if this is a sticky thread.
     */
    public boolean isSticky ()
    {
        return (flags & FLAG_STICKY) != 0;
    }

    /**
     * Returns true if this thread is locked.
     */
    public boolean isLocked ()
    {
        return (flags & FLAG_LOCKED) != 0;
    }
}
