//
// $Id$

package com.threerings.msoy.game.client {

import mx.styles.StyleManager;

import com.threerings.util.Log;

import com.threerings.presents.client.BasicDirector;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.ClientEvent;

import com.threerings.crowd.client.PlaceController;

import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.MsoyController;
import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.MsoyCodes;
import com.threerings.msoy.utils.Base64Encoder;

import com.threerings.msoy.item.data.all.Game;
import com.threerings.msoy.world.client.WorldContext;

import com.threerings.msoy.avrg.client.AVRGameBackend;
import com.threerings.msoy.avrg.client.AVRGameController;
import com.threerings.msoy.avrg.client.AVRGameLiaison;

import com.threerings.msoy.game.data.GameGameMarshaller;
import com.threerings.msoy.game.data.LobbyCodes;
import com.threerings.msoy.game.data.MsoyGameCodes;
import com.threerings.msoy.game.data.MsoyGameConfig;
import com.threerings.msoy.game.data.MsoyGameDefinition;
import com.threerings.msoy.game.data.WorldGameMarshaller;

/**
 * A director that manages game related bits.
 */
public class GameDirector extends BasicDirector
{
    public const log :Log = Log.getLog(this);

    // statically reference classes we require
    MsoyGameDefinition;
    WorldGameMarshaller;
    GameGameMarshaller;

    public function GameDirector (ctx :WorldContext)
    {
        super(ctx);
        _wctx = ctx;
    }

    /**
     * Returns true if we're in a lobby or in a game.
     */
    public function isGaming () :Boolean
    {
        return (_liaison != null);
    }

    /**
     * Returns the gameId of the game we're currently connected to, or zero if we're not.
     */
    public function getGameId () :int
    {
        return (_liaison != null) ? _liaison.gameId : 0;
    }

    /**
     * Returns the name of the game we're currently connected to, or null if we're not.
     */
    public function getGameName () :String
    {
        return (_liaison != null) ? _liaison.gameName : null;
    }

    /**
     * Returns the currently active GameContext or null if no game is active.
     */
    public function getGameContext () :GameContext
    {
        return (_liaison != null) ? _liaison.getGameContext() : null;
    }

    /**
     * Returns the configuration of the (non-world) game we currently occupy if we're in a game.
     * Returns null otherwise.
     */
    public function getGameConfig () :MsoyGameConfig
    {
        if (_liaison != null) {
            return _liaison.gameConfig as MsoyGameConfig;
        }
        return null;
    }

    /**
     * Returns the currently active GameController or null if no game is active.
     */
    public function getGameController () :PlaceController
    {
        var gctx :GameContext = getGameContext();
        return (gctx != null) ? gctx.getLocationDirector().getPlaceController() : null;
    }

    /**
     * Populates the supplied array with data to create the game control bar menu.
     *
     * @return true if the menu was populated, false if we're not in a game.
     */
    public function populateGameMenu (menuData :Array) :Boolean
    {
        if (_liaison == null) {
            return false;
        }

        menuData.push({label: _liaison.gameName});
        menuData.push({type: "separator"});
        menuData.push({label: Msgs.GAME.get("b.gameInstructions"), command: viewGameInstructions,
            icon: StyleManager.getStyleDeclaration(".controlBarGameButton").getStyle("image")});
        if (_liaison.gameGroupId != Game.NO_GROUP) {
            menuData.push({label: Msgs.GAME.get("b.gameGroup"),
                           command: MsoyController.VIEW_GROUP, arg: _liaison.gameGroupId });
        }
        menuData.push({label: Msgs.GAME.get("b.gameShop"), command: viewGameShop });
        menuData.push({label: Msgs.GAME.get("b.gameComment"), command: viewGameComments,
            icon: StyleManager.getStyleDeclaration(".controlBarButtonComment").getStyle("image")});
        menuData.push({label: Msgs.GAME.get("b.gameTrophies"), command: viewGameTrophies});
        if (_liaison is AVRGameLiaison) {
            menuData.push({label: Msgs.GAME.get("b.gameInvite"), enabled: false});
            menuData.push({label: Msgs.GAME.get("b.gameExit"), command: leaveAVRGame});
        }
        if (Game.isDevelopmentVersion(_liaison.gameId) && !_wctx.getMyName().isGuest() &&
            !(_liaison is AVRGameLiaison)) {
            menuData.push({label: Msgs.GAME.get("b.gameRemoveTrophies"), command: removeTrophies});
        }

        return true;
    }

    /**
     * Requests that the lobby for the specified game be joined and displayed.
     */
    public function displayLobby (gameId :int, ghost :String, gport :int) :void
    {
        log.info("Displaying lobby [gameId=" + gameId + "].");

        if (_liaison != null) {
            if (_liaison is LobbyGameLiaison && _liaison.gameId == gameId) {
                LobbyGameLiaison(_liaison).showLobby();
            } else {
                _liaison.shutdown();
                _liaison = null;
            }
        }
        if (_liaison == null) {
            // create our new liaison, which will resolve the lobby and do all the business
            _liaison = new LobbyGameLiaison(_wctx, gameId, LobbyCodes.SHOW_LOBBY_ANY);
            _liaison.start(ghost, gport);
        }
    }
    
    /**
     * Requests that the lobby for the current game be displayed. Returns true
     * when successful, or false if there is no existing game or existing lobby 
     * (eg. the game is an AVRG) or other problems were encountered.
     * 
     * @param multiplayer When true, and the game supports multiplayer, it will
     * only show multiplayer table options (create table or join existing ones).
     * Otherwise it will show a generic lobby.
     */
    public function displayCurrentGameLobby (multiplayer :Boolean) :Boolean
    {
        log.info("Display current game's lobby", "gameId", getGameId());
        if (_liaison is LobbyGameLiaison) {
            const type :int = multiplayer ? 
                LobbyCodes.SHOW_LOBBY_MULTIPLAYER : LobbyCodes.SHOW_LOBBY_ANY;
                
            LobbyGameLiaison(_liaison).showLobby(type);
            return true;
        }
        
        return false;
    }
    
    /**
     * Displays the instructions page for the currently active game.
     */
    public function viewGameInstructions () :void
    {
        _wctx.getMsoyClient().trackClientAction("flashViewGameInstructions", null);
        _wctx.getWorldController().displayPage("games", "d_" + getGameId() + "_i");
    }

    /**
     * Displays the comments page for the currently active game.
     */
    public function viewGameComments () :void
    {
        _wctx.getMsoyClient().trackClientAction("flashViewGameComments", null);
        _wctx.getWorldController().displayPage("games", "d_" + getGameId() + "_c");
    }

    /**
     * Displays the trophies page for the currently active game.
     */
    public function viewGameTrophies () :void
    {
        TrophyPanel.show(getGameContext(), getGameId(), getGameName());
    }

    /**
     * Removes the trophies that this player has earned in an in-development game copy.
     */
    public function removeTrophies () :void
    {
        if (!Game.isDevelopmentVersion(_liaison.gameId)) {
            log.warning("Asked to remove copies from a non-development game", "gameId", 
                _liaison.gameId);
            return;
        }

        var svc :GameGameService = 
            getGameContext().getClient().requireService(GameGameService) as GameGameService;
        svc.removeDevelopmentTrophies(getGameContext().getClient(), _liaison.gameId, 
            getGameContext().getMsoyContext().confirmListener(
            "m.trophies_removed", MsoyCodes.GAME_MSGS));
    }

    /**
     * Displays the shop page for the currently active game.
     */
    public function viewGameShop (itemType :int = 0, catalogId :int = 0) :void
    {
        var args :String;
        if (catalogId != 0) {
            args = "l_" + itemType + "_" + catalogId;
        } else {
            args = "g_" + getGameId();
            if (itemType != 0) {
                args += "_" + itemType;
            }
        }
        _wctx.getMsoyClient().trackClientAction("flashViewGameShop", null);
        _wctx.getWorldController().displayPage("shop", args);
    }
    
    public function viewSharePage (defmsg :String, token :String = "", roomId :int = 0) :void
    {
    	// The default message and token can by anything.  We will parse the underscores specially,
    	// so encode them before adding them as arguments to the invites page.
    	var encoder :Base64Encoder = new Base64Encoder();
    	encoder.encodeUTFBytes(token);
    	var encodedToken :String = encoder.toString();
    	var args :String = "invites_share_" + getGameId() + "_" + encodeShareString(defmsg) + "_" + 
    	   encodedToken + (_liaison is AVRGameLiaison ? "_avrg" + roomId : "_game");
    	_wctx.getWorldController().displayPage("people", args);
    }

    /**
     * Requests that we immediately start playing the specified game id.
     *
     * @param mode one of either 's' for single player, 'f' for friends-only quick game, or 'm' for
     * anyone quick game.
     */
    public function playNow (gameId :int, modeStr: String, ghost :String, gport :int, 
        token :String, shareMemberId :int) :void
    {
        var mode :int = LobbyCodes.PLAY_NOW_SINGLE;
        if (modeStr == "m") {
            mode = LobbyCodes.PLAY_NOW_ANYONE;
        }
        if (_liaison != null) {
            if (_liaison is LobbyGameLiaison && _liaison.gameId == gameId) {
                LobbyGameLiaison(_liaison).playNow(mode);
            } else {
                _liaison.shutdown();
                _liaison = null;
            }
        }
        if (_liaison == null) {
            // create our new liaison, which will head on into the game once we're logged on
            _liaison = new LobbyGameLiaison(_wctx, gameId, mode, 0, token, shareMemberId);
            _liaison.start(ghost, gport);
        }
    }

    /**
     * Requests that we join the given player in the given game.
     */
    public function joinPlayer (gameId :int, memberId :int) :void
    {
        if (_liaison != null) {
            if (_liaison is AVRGameLiaison || _liaison.gameId != gameId) {
                _liaison.shutdown();
                _liaison = null;
            }
        }

        if (_liaison == null) {
            _liaison = new LobbyGameLiaison(_wctx, gameId, LobbyCodes.JOIN_PLAYER, memberId);
            _liaison.start(); // game host/port are unknown here
        } else {
            LobbyGameLiaison(_liaison).joinPlayer(memberId);
        }
    }

    /**
     * Requests that we join the given player at his pending game table.
     */
    public function joinPlayerTable (
        gameId :int, memberId :int, ghost :String = null, gport :int = 0) :void
    {
        if (_liaison != null) {
            if (_liaison is AVRGameLiaison || _liaison.gameId != gameId) {
                _liaison.shutdown();
                _liaison = null;
            }
        }
        displayLobby(gameId, ghost, gport);
        LobbyGameLiaison(_liaison).joinPlayerTable(memberId);
    }

    /** Forwards idleness status updates to any AVRG we may be playing. */
    public function setIdle (nowIdle :Boolean) :void
    {
        if (_liaison != null && _liaison is AVRGameLiaison) {
            var ctrl :AVRGameController = AVRGameLiaison(_liaison).getAVRGameController();
            if (ctrl != null) {
                ctrl.setIdle(nowIdle);
            }
        }
    }

    /**
     * Retrieve the backend of the AVRG currently in progress, or null.
     */
    public function getAVRGameBackend () :AVRGameBackend
    {
        if (_liaison != null && _liaison is AVRGameLiaison) {
            return AVRGameLiaison(_liaison).getAVRGameBackend();
        }
        return null;
    }

    /**
     * Called when we first login and then every time we leave a game or a lobby;
     * checks to see if we have a persistent AVRG we should (re-)activate.
     */
    public function checkMemberAVRGame () :void
    {
        // Re-entering an AVRG after leaving a game lobby is a bit strange but was thought to be a
        // cool feature at one point. TODO: get rid of this completely if it isn't needed anymore.
        if (false && _liaison == null) {
            var memberObj :MemberObject = _wctx.getMemberObject();
            // we might not yet be logged onto our world server; freak not out if so
            if (memberObj != null && memberObj.avrGameId != 0) {
                _liaison = new AVRGameLiaison(_wctx, memberObj.avrGameId);
                _liaison.start();
            }
        }
    }

    /**
     * Activates the specified AVR game, connecting to the appropriate game server and clearing any
     * existing game server connection.
     */
    public function activateAVRGame (gameId :int, token :String = "", shareMemberId :int = 0) :void
    {
        if (_liaison != null) {
            if (_liaison is LobbyGameLiaison) {
                log.warning("Eek, asked to join an AVRG while in a lobbied game.");
                return;
            }
            if (_liaison.gameId == gameId) {
                if (_liaison.gameConfig == null) {
                    displayFeedback("m.locating_game");
                }
                return;
            }
            // TODO: implement proper leaving, this should only be the fallback
            _liaison.shutdown();
        }

        displayFeedback("m.locating_game");

        _liaison = new AVRGameLiaison(_wctx, gameId, token, shareMemberId);
        _liaison.start();
    }

    public function leaveAVRGame () :void
    {
        if (_liaison != null && _liaison is AVRGameLiaison) {
            AVRGameLiaison(_liaison).leaveAVRGame();
        }
    }

    /**
     * Requests that we move to the specified game location.
     */
    public function enterGame (gameOid :int) :void
    {
        if (_liaison == null) {
            log.warning("Requested to enter game but have no liaison?! [oid=" + gameOid + "].");
        } else if (!(_liaison is LobbyGameLiaison)) {
            log.warning("Requested to enter game but have AVRG liaison?! [oid=" + gameOid + "].");
        } else {
            // this is only used for testing game loading issues per WRLD-531,
            // and will be removed after the test is over. -- robert
            _wctx.getMsoyClient().trackClientAction("WRLD-531-2 game started", "stage 6");

            LobbyGameLiaison(_liaison).enterGame(gameOid);
        }
    }

    /**
     * Leaves our current parlor game and either returns to the Whirled or displays the lobby.
     */
    public function backToWhirled (showLobby :Boolean) :void
    {
        if (showLobby) {
            LobbyGameLiaison(_liaison).clearGame();
            _wctx.getWorldController().handleJoinGameLobby(getGameId());
        } else {
            // go back to our previous location
            _wctx.getMsoyController().handleMoveBack(true);
        }
    }

    /**
     * Called by the GameLiaison when it has shutdown and gone away.
     */
    public function liaisonCleared (liaison :GameLiaison) : void
    {
        // we could get the "wrong" liaison here, if we were asked to load up a new lobby while
        // another one was active.
        if (_liaison == liaison) {
            _liaison = null;
            // if this was a lobbied game, see about restarting the AVRG
            if (liaison is LobbyGameLiaison) {
                checkMemberAVRGame();
            }
        }
    }

    // from BasicDirector
    override public function clientDidLogoff (event :ClientEvent) :void
    {
        super.clientDidLogoff(event);

        // if we're actually logging off, rather than just switching servers, then shutdown any
        // active game connection
        if (!_wctx.getClient().isSwitchingServers() && _liaison != null) {
            _liaison.shutdown();
        }
    }
    
    protected function encodeShareString (src :String) :String
    {
        var output :String = "";
        for (var i: int = 0; i < src.length; i++) {
            if (src.charAt(i) == '_') {
                output += "\\-";
            } else if (src.charAt(i) == '\\') {
                output += "\\\\";
            } else {
                output += src.charAt(i);
            }
        }
        return output;
    }

    /**
     * A convenience method to display feedback using the game bundle.
     */
    protected function displayFeedback (msg :String) :void
    {
        _wctx.displayFeedback(MsoyGameCodes.GAME_BUNDLE, msg);
    }

    // from BasicDirector
    override protected function registerServices (client :Client) :void
    {
        client.addServiceGroup(MsoyCodes.GAME_GROUP);
    }

    /** A casted ref to the msoy context. */
    protected var _wctx :WorldContext;

    /** Handles our connection to the game server. */
    protected var _liaison :GameLiaison;
}
}
