//
// $Id$

package com.threerings.msoy.data.all {

import com.threerings.io.ObjectOutputStream;
import com.threerings.io.ObjectInputStream;
import com.threerings.presents.dobj.DSet_Entry;

import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.data.all.MediaDesc;

/**
 * Represents a friend connection.
 */
public class PlayerEntry
    implements DSet_Entry
{
    /** The display name of the friend. */
    public var name :VizMemberName;

    public function PlayerEntry (name :VizMemberName = null)
    {
        this.name = name;
    }

    // from Hashable
    public function hashCode () :int
    {
        return this.name.hashCode();
    }

    // from Hashable
    public function equals (other :Object) :Boolean
    {
        return (other is PlayerEntry) &&
            (this.name.getMemberId() == (other as PlayerEntry).name.getMemberId());
    }

    public function toString () :String
    {
        return "PlayerEntry[" + name + "]";
    }

    // from interface DSet_Entry
    public function getKey () :Object
    {
        return this.name.getKey();
    }

    // from interface Streamable
    public function readObject (ins :ObjectInputStream) :void
    {
        name = VizMemberName(ins.readObject());
    }

    // from interface Streamable
    public function writeObject (out :ObjectOutputStream) :void
    {
        out.writeObject(name);
    }
}
}
