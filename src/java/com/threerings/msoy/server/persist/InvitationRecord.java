//
// $Id$

package com.threerings.msoy.server.persist;

import com.samskivert.jdbc.depot.Key;
import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.annotation.*; // for Depot annotations
import com.samskivert.jdbc.depot.expression.ColumnExp;

import java.sql.Date;
import java.sql.Timestamp;

import com.samskivert.util.StringUtil;

/**
 * Contains persistent data stored for every member of MetaSOY.
 */
@Entity(indices={ @Index(columns={"inviterId"}), @Index(columns={"inviteeId"}), 
    @Index(columns={"inviteeEmailHash"})})
public class InvitationRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    /** The column identifier for the {@link #inviteeEmail} field. */
    public static final String INVITEE_EMAIL = "inviteeEmail";

    /** The qualified column identifier for the {@link #inviteeEmail} field. */
    public static final ColumnExp INVITEE_EMAIL_C =
        new ColumnExp(InvitationRecord.class, INVITEE_EMAIL);

    /** The column identifier for the {@link #inviterId} field. */
    public static final String INVITER_ID = "inviterId";

    /** The qualified column identifier for the {@link #inviterId} field. */
    public static final ColumnExp INVITER_ID_C =
        new ColumnExp(InvitationRecord.class, INVITER_ID);

    /** The column identifier for the {@link #inviteHash} field. */
    public static final String INVITE_HASH = "inviteHash";

    /** The qualified column identifier for the {@link #inviteHash} field. */
    public static final ColumnExp INVITE_HASH_C =
        new ColumnExp(InvitationRecord.class, INVITE_HASH);

    /** The column identifier for the {@link #inviteeId} field. */
    public static final String INVITEE_ID = "inviteeId";

    /** The qualified column identifier for the {@link #inviteeId} field. */
    public static final ColumnExp INVITEE_ID_C =
        new ColumnExp(InvitationRecord.class, INVITEE_ID);

    /** The column identifier for the {@link #issued} field. */
    public static final String ISSUED = "issued";

    /** The qualified column identifier for the {@link #issued} field. */
    public static final ColumnExp ISSUED_C =
        new ColumnExp(InvitationRecord.class, ISSUED);

    /** The column identifier for the {@link #viewed} field. */
    public static final String VIEWED = "viewed";

    /** The qualified column identifier for the {@link #viewed} field. */
    public static final ColumnExp VIEWED_C =
        new ColumnExp(InvitationRecord.class, VIEWED);
    // AUTO-GENERATED: FIELDS END

    /** Increment this value if you modify the definition of this persistent
     * object in a way that will result in a change to its SQL counterpart. */
    public static final int SCHEMA_VERSION = 1;

    /** The email address we're sending this invitation to. */
    @Id
    public String inviteeEmail;

    /** The inviter's member Id */
    @Id
    public int inviterId; 

    /** An MD5 Hash generated from the invitee's email address and the inviter's memberId. Used
     * for the unique URL that is sent with this invitation. */
    public byte[] inviteHash;

    /** The memberId that was assigned to the invitee when (if) the invitation was accepted. */
    public int inviteeId;

    /** The time that this invite was sent out */
    public Timestamp issued;

    /** The time that this invitation was first viewed. */
    public Timestamp viewed;

    /** A blank constructor used when loading records from the database. */
    public InvitationRecord ()
    {
    }

    /** Generates a string representation of this instance. */
    public String toString ()
    {
        return StringUtil.fieldsToString(this);
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link #InvitationRecord}
     * with the supplied key values.
     */
    public static Key<InvitationRecord> getKey (String inviteeEmail, int inviterId)
    {
        return new Key<InvitationRecord>(
                InvitationRecord.class,
                new String[] { INVITEE_EMAIL, INVITER_ID },
                new Comparable[] { inviteeEmail, inviterId });
    }
    // AUTO-GENERATED: METHODS END
}
