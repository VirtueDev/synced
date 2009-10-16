//
// $Id$

package com.threerings.msoy.room.gwt;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.threerings.util.Name;

import com.threerings.msoy.data.all.GroupName;
import com.threerings.msoy.data.all.MediaDesc;

/**
 * Contains detailed information on a particular room.
 */
public class RoomDetail
    implements IsSerializable
{
    /** More metadata for this room. */
    public RoomInfo info;

    /** The room's full-size snapshot. */
    public MediaDesc snapshot;

    /** The owner of this room (either a MemberName or a GroupName). */
    public Name owner;

    /** The theme this room is associated with, or null. */
    public GroupName theme;

    /** The rating assigned to the room by this player. */
    public byte memberRating;

    /** The number of players who have rated this room. */
    public int ratingCount;
}
