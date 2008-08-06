//
// $Id$

package client.world;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.FlowPanel;

import com.threerings.gwt.ui.InlineLabel;
import com.threerings.gwt.ui.SmartTable;

import com.threerings.msoy.comment.gwt.Comment;
import com.threerings.msoy.data.all.GroupName;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.world.gwt.RoomInfo;
import com.threerings.msoy.world.gwt.WorldService;
import com.threerings.msoy.world.gwt.WorldServiceAsync;

import client.comment.CommentsPanel;
import client.ui.StyledTabPanel;
import client.util.Link;
import client.util.MsoyCallback;
import client.util.ServiceUtil;

/**
 * Displays information about a room, allows commenting.
 */
public class RoomPanel extends SmartTable
{
    public RoomPanel (int sceneId)
    {
        super("roomPanel", 0, 5);

        _worldsvc.loadRoomInfo(sceneId, new MsoyCallback<RoomInfo>() {
            public void onSuccess (RoomInfo info) {
                init(info);
            }
        });
    }

    protected void init (RoomInfo info)
    {
        if (info == null) {
            setText(0, 0, "That room does not exist.");
            return;
        }
        CWorld.frame.setTitle(info.name);

        FlowPanel obits = new FlowPanel();
        obits.add(new InlineLabel(_msgs.owner(), false, false, true));
        if (info.owner instanceof MemberName) {
            MemberName name = (MemberName)info.owner;
            obits.add(Link.memberView(name.toString(), name.getMemberId()));
        } else if (info.owner instanceof GroupName) {
            GroupName name = (GroupName)info.owner;
            obits.add(Link.groupView(name.toString(), name.getGroupId()));
        }
        addWidget(obits, 1, null);

        StyledTabPanel tabs = new StyledTabPanel();
        tabs.add(new CommentsPanel(Comment.TYPE_ROOM, info.sceneId), _msgs.tabComments());
        addWidget(tabs, 1, null);
        tabs.selectTab(0);
    }

    protected static final WorldMessages _msgs = GWT.create(WorldMessages.class);
    protected static final WorldServiceAsync _worldsvc = (WorldServiceAsync)
        ServiceUtil.bind(GWT.create(WorldService.class), WorldService.ENTRY_POINT);
}
