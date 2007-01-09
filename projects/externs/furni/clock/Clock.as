//
// $Id$

package {

import flash.display.DisplayObject;
import flash.display.Sprite;

import flash.events.Event;

import flash.geom.Matrix;
import flash.geom.Transform;

[SWF(width="200", height="200")]
public class Clock extends Sprite
{
    /** The content pack. TODO. */
    public var content :Object = new ContentPack();


    public function Clock ()
    {
        root.loaderInfo.addEventListener(Event.UNLOAD, handleUnload);

        configureContent();
        updateDisplayedTime();

        addEventListener(Event.ENTER_FRAME, handleEnterFrame)
    }

    /**
     * Take care of releasing resources when we unload.
     */
    protected function handleUnload (event :Event) :void
    {
        removeEventListener(Event.ENTER_FRAME, handleEnterFrame);
    }

    /**
     * Update the position of each hand of the clock every frame.
     */
    protected function handleEnterFrame (evt :Event) :void
    {
        updateDisplayedTime();
    }

    protected function configureContent () :void
    {
        var centerX :int = 0;
        var centerY :int = 0;

        // configure the clock's face
        var face :DisplayObject = getDisplayResource("face");
        if (face != null) {
            addChild(face);

            var faceCenter :Array = (content.faceCenter as Array);
            if (faceCenter != null) {
                centerX = int(faceCenter[0]);
                centerY = int(faceCenter[1]);

            } else {
                centerX = face.width / 2;
                centerY = face.height / 2;
            }

        } else {
            trace("No clock face provided");
        }

        _hourHand = configureHand("hour", centerX, centerY);
        _minuteHand = configureHand("minute", centerX, centerY);
        _secondHand = configureHand("second", centerX, centerY);
        _smoothSeconds = Boolean(content.smoothSeconds);
    }

    protected function updateDisplayedTime () :void
    {
        var d :Date = new Date();

        updateHand(_hourHand,
            (d.hours % 12) * 60 * 60 + (d.minutes * 60) + d.seconds,
            12 * 60 * 60);
        updateHand(_minuteHand,
            (d.minutes * 60) + d.seconds, 60 * 60);

        // the second hand is optional
        if (_secondHand != null) {
            if (_smoothSeconds) {
                updateHand(_secondHand,
                    (d.seconds * 1000) + d.milliseconds, 60000);
            } else {
                updateHand(_secondHand, d.seconds, 60);
            }
        }
    }

    protected function updateHand (
        hand :DisplayObject, current :Number, total :Number) :void
    {
        hand.rotation = (current * 360) / total;
    }

    protected function getDisplayResource (name :String) :DisplayObject
    {
        var c :Class = (content[name] as Class);
        if (c != null) {
            return (new c() as DisplayObject);
        }
        return null;
    }

    protected function configureHand (
        name :String, x :int, y :int, optional :Boolean = false) :DisplayObject
    {
        var hand :DisplayObject = getDisplayResource(name + "Hand");
        if (hand != null) {
            var point :Array = (content[name + "Point"] as Array);
            if (point != null) {
                // create a wrapper for the hand so that we can apply the offset
                var wrap :Sprite = new Sprite();
                hand.x = -int(point[0]);
                hand.y = -int(point[1]);
                wrap.addChild(hand);

                wrap.x = x;
                wrap.y = y;
                addChild(wrap);
                // our caller doesn't need to know that it's getting
                // the wrapper
                return wrap;

            } else {
                trace("No " + name + " point specified.");
            }

        } else if (!optional) {
            trace("No " + name + " hand provided");
        }
        return null;
    }

    protected var _hourHand :DisplayObject;

    protected var _minuteHand :DisplayObject;

    protected var _secondHand :DisplayObject;

    protected var _smoothSeconds :Boolean;
}
}
