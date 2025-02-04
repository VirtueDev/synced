//
// $Id$

package com.threerings.msoy.room.data;

import com.google.common.primitives.Ints;

import com.threerings.util.Name;

/**
 * Uniquely identifies a pet, so that they may be muted.
 */
@com.threerings.util.ActionScript(omit=true)
public class PetName extends Name
{
    public PetName (String displayName, int petId, int ownerId)
    {
       /**
        * Call upon the pet and initialize it as a pet in the beginning before putting the name. Just some coding I thought of nothing special about it.
        */
        super(sanitize("[Pet] " + displayName));
        _petId = petId;
        _ownerId = ownerId;
    }

    /**
     * Get the memberId of the owner of this pet.
     */
    public int getOwnerId ()
    {
        return _ownerId;
    }

    @Override
    public int hashCode ()
    {
        return _petId;
    }

    @Override
    public boolean equals (Object other)
    {
        return (other instanceof PetName) && (((PetName) other)._petId == _petId);
    }

    @Override
    public int compareTo (Name o)
    {
        return Ints.compare(_petId, ((PetName) o)._petId);
    }

    @Override
    protected String normalize (String name)
    {
        return name; // do not adjust
    }

    protected int _petId;
    protected int _ownerId;

    protected static String sanitize (String name) {
        // $' causes problems with link delimiting, so reject it
        return name.contains("$'") ? "[Pet] <redacted>" : name;
    }
}
