package
{

import flash.display.Sprite;    
import flash.display.Shape;
import flash.text.TextField;
import mx.core.BitmapAsset;

import com.threerings.ezgame.HostCoordinator;
import com.threerings.ezgame.HostEvent;
import com.threerings.ezgame.MessageReceivedEvent;
import com.threerings.ezgame.MessageReceivedListener;
import com.threerings.ezgame.PropertyChangedEvent;
import com.threerings.ezgame.PropertyChangedListener;

import com.whirled.WhirledGameControl;


/**
   Main game takes care of initializing network connections,
   maintaining distributed data representation, and responding to events.
*/

[SWF(width="600", height="400")]
public class TangleWord extends Sprite 
{
    
    // PUBLIC METHODS

    // Constructor creates the board, and registers itself for events
    // and other startup information.
    public function TangleWord () : void
    {
        // Initialize game data
        _gameCtrl = new WhirledGameControl (this);
        _gameCtrl.registerListener (this);
        if (_gameCtrl.isConnected())
        {
            _coordinator = new HostCoordinator (_gameCtrl);
            _coordinator.addEventListener (HostEvent.CHANGED, hostChangedHandler);
            _rounds = new RoundProvider (_gameCtrl, _coordinator);
            
            // Create MVC elements
            _controller = new Controller (_gameCtrl, null, _rounds); // we'll set the model later...
            _display = new Display (_controller, _rounds);
            _model = new Model (_gameCtrl, _coordinator, _rounds, _display);
            _controller.setModel (_model);                           // ... as in, right here :)
            addChild (_display);
            
            // Try initializing the game state
            maybeStartGame ();
            
        } else {
            // Initialize the background bitmap
            var background :BitmapAsset = Resources.makeGameBackground ();
            Assert.NotNull (background, "Background bitmap failed to initialize!"); 
            addChild (background);
            // Error message
            var label :TextField = new TextField();
            label.x = Properties.BOARD.x;
            label.y = Properties.BOARD.y;
            label.width = Properties.BOARD.width;
            label.multiline = true;
            label.htmlText = "<center><p align=\"center\"><font size=\"+2\">TangleWord</font><br/>This game can only be played in <a href=\"http://www.whirled.com\"><u>the Whirled</u></a>.</p>";
            addChild(label);
        }
    }

    /**
     * Once the host was found, start the game!
     */
    private function hostChangedHandler (event : HostEvent) : void
    {
        // Try initializing the game state (if I'm the first host)
        if (event.previousHost == 0 && event.newHost == _gameCtrl.getMyId()) {
            maybeStartGame ();
        }
    }

    /**
       Sets up the game information. This needs to happen after all of the
       MVC objects have been initialized, and only once the host state
       has been determined. 
    */
    private function maybeStartGame () : void
    {
        if (_rounds != null && _coordinator != null &&
            _coordinator.status != HostCoordinator.STATUS_UNKNOWN)
        {
            _rounds.setTimeout (RoundProvider.SYSTEM_STARTED_STATE, 0); 
            _rounds.setTimeout (RoundProvider.ROUND_STARTED_STATE, Properties.ROUND_LENGTH);
            _rounds.setTimeout (RoundProvider.ROUND_ENDED_STATE, Properties.PAUSE_LENGTH);
            
            // If I joined an existing game, display time remaining till next round
            if (! isNaN (_rounds.getCurrentStateTimeout ()))
            {
                var timeout : Number = _rounds.getCurrentStateTimeout ();
                var timenow : Number = (new Date()).time;
                var delta : Number = (timeout - timenow) / 1000;
                _display.forceTimerStart (delta);
                _display.logPleaseWait ();
            }
            
            // However, if I somehow became the host, just initialize everything anew.
            if (_coordinator.status == HostCoordinator.STATUS_HOST) {
                _rounds.setCurrentState (RoundProvider.SYSTEM_STARTED_STATE);
                initializeScoreboard ();
            }
        }
    }


    // PRIVATE FUNCTIONS

    /** Creates a new distributed scoreboard */
    private function initializeScoreboard () : void
    {
        // Create a new instance, and fill in the names
        var board : Scoreboard = new Scoreboard ();
        var occupants : Array = _gameCtrl.getOccupants ();
        for each (var id : int in occupants)
        {
            board.addPlayer (_gameCtrl.getOccupantName (id));
        }

        // Finally, share it!
        _gameCtrl.set (SHARED_SCOREBOARD, board.internalScoreObject);
    }
        
    


    // PRIVATE MEMBER VARIABLES

    /** Game control object */
    private var _gameCtrl : WhirledGameControl;

    /** Helps us determine who is the authoritative host */
    private var _coordinator : HostCoordinator;

    /** Coordinates round info */
    private var _rounds : RoundProvider;
    
    /** Data interface */
    private var _model : Model;

    /** Data display */
    private var _display : Display;

    /** Data validation */
    private var _controller : Controller;


    // PRIVATE CONSTANTS

    /** Key name: shared letter set */
    private static const SHARED_LETTER_SET : String = "Shared Letter Set";

    /** Key name: shared scoreboard */
    private static const SHARED_SCOREBOARD : String = "Shared Scoreboard";

    
}
    
    
    
}
