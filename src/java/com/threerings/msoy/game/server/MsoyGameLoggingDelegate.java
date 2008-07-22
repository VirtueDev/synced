package com.threerings.msoy.game.server;

import java.util.Map;

import com.samskivert.util.HashIntMap;

import com.threerings.parlor.game.server.GameManagerDelegate;
import com.threerings.msoy.game.data.PlayerObject;
import com.threerings.msoy.server.MsoyEventLogger;

import static com.threerings.msoy.Log.log;

/**
 * Delegate that keeps track of whether the game played is single- or multi-player,
 * and keeps track of time spent in it for game metrics tracking purposes.
 *
 * Note: game time semantics are different than those used for flow awards:
 * this logs the entire time from joining the table to leaving it,
 * whether or not a 'game' is active or not.
 */
public class MsoyGameLoggingDelegate extends GameManagerDelegate
{
    public MsoyGameLoggingDelegate (GameContent content, MsoyEventLogger eventLog)
    {
        _content = content;
        _eventLog = eventLog;
    }

    @Override
    public void bodyEntered (int bodyOid)
    {
        super.bodyEntered(bodyOid);

        // track when this occupant entered
        _entries.put(bodyOid, System.currentTimeMillis());
    }

    @Override
    public void bodyLeft (int bodyOid)
    {
        super.bodyLeft(bodyOid);

        final Long entry = _entries.remove(bodyOid);
        final PlayerObject plobj = (PlayerObject) MsoyGameServer.omgr.getObject(bodyOid);

        if (entry == null || plobj == null) {
            log.warning("Unknown game player just left!", "bodyOid", bodyOid);
            return;
        }

        // now that they left, log their info
        int memberId = plobj.memberName.getMemberId();
        int seconds = (int)((System.currentTimeMillis() - entry) / 1000);

        final MsoyGameManager gmgr = (MsoyGameManager)_plmgr;
        final String tracker = (plobj.referral != null) ? plobj.referral.tracker : null;
        if (tracker == null) {
            log.warning("Game finished without referral info", "memberId", memberId);
        }

        _eventLog.gameLeft(
            memberId, _content.game.genre, _content.game.gameId, seconds, 
            gmgr.isMultiplayer(), tracker);
        
    }

    /** Game description. */
    final protected GameContent _content;

    /** Event logger. */
    final protected MsoyEventLogger _eventLog;

    /** Mapping from player oid to their entry timestamp. */
    Map<Integer, Long> _entries = new HashIntMap<Long>();
}
