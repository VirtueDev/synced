//
// $Id$

package com.threerings.msoy.server.persist;

import java.sql.Date;
import java.sql.Timestamp;

import com.samskivert.io.PersistenceException;

import com.samskivert.jdbc.depot.Key;
import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.annotation.*; // for Depot annotations
import com.samskivert.jdbc.depot.expression.ColumnExp;

import com.samskivert.util.StringUtil;

import com.threerings.msoy.server.MsoyServer;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.web.data.Invitation;

/**
 * Contains persistent data stored for every member of MetaSOY.
 */
@Entity(indices={
        @Index(name="ixInviter", columns={"inviterId"}),
        @Index(name="ixInvitee", columns={"inviteeId"}), 
        @Index(name="ixInvite", columns={"inviteId"})})
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

    /** The column identifier for the {@link #inviteId} field. */
    public static final String INVITE_ID = "inviteId";

    /** The qualified column identifier for the {@link #inviteId} field. */
    public static final ColumnExp INVITE_ID_C =
        new ColumnExp(InvitationRecord.class, INVITE_ID);

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
    public static final int SCHEMA_VERSION = 2;

    /** The email address we're sending this invitation to. */
    @Id
    public String inviteeEmail;

    /** The inviter's member Id */
    @Id
    public int inviterId; 

    /** A randomly generated string of numbers and characters that is used to uniquely identify
     * this invitation. */
    @Column(unique=true)
    public String inviteId;

    /** The memberId that was assigned to the invitee when (if) the invitation was accepted. */
    public int inviteeId;

    /** The time that this invite was sent out */
    @Column(columnDefinition="issued DATETIME NOT NULL")
    public Timestamp issued;

    /** The time that this invitation was first viewed. */
    @Column(columnDefinition="viewed DATETIME")
    public Timestamp viewed;

    /** A blank constructor used when loading records from the database. */
    public InvitationRecord ()
    {
    }

    /**
     * Create a new record for an Invitation that is being issued right now.
     */
    public InvitationRecord (String inviteeEmail, int inviterId, String inviteId)
    {
        this.inviteeEmail = inviteeEmail;
        this.inviterId = inviterId;
        this.inviteId = inviteId;
        issued = new Timestamp((new java.util.Date()).getTime());
    }

    /** Generates a string representation of this instance. */
    public String toString ()
    {
        return StringUtil.fieldsToString(this);
    }

    public Invitation toInvitationObject () 
        throws PersistenceException
    {
        Invitation inv = new Invitation();
        inv.inviteeEmail = inviteeEmail;
        inv.inviteId = inviteId;
        MemberNameRecord memNameRec = MsoyServer.memberRepo.loadMemberName(inviterId);
        inv.inviter = new MemberName(memNameRec.name, memNameRec.memberId);
        return inv;
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
