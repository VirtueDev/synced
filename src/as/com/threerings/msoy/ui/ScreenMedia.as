package com.threerings.msoy.ui {

import flash.display.DisplayObject;
import flash.display.Loader;
import flash.display.LoaderInfo;
import flash.display.Shape;

import flash.events.Event;
import flash.events.EventDispatcher;
import flash.events.IOErrorEvent;
import flash.events.MouseEvent;
import flash.events.SecurityErrorEvent;
import flash.events.StatusEvent;
import flash.events.TextEvent;

import flash.media.Video;

import flash.net.LocalConnection;
import flash.net.NetConnection;
import flash.net.NetStream;

import flash.system.ApplicationDomain;
import flash.system.LoaderContext;
import flash.system.SecurityDomain;

import flash.net.URLRequest;

import mx.containers.Box;

import mx.effects.Glow;

import mx.events.EffectEvent;

import com.threerings.media.image.ImageUtil;

import com.threerings.msoy.data.MediaData;

import com.threerings.util.HashMap;

/**
 * A wrapper class for all media that will be placed on the screen.
 * Subject to change.
 */
public class ScreenMedia extends Box
{
    /**
     * Constructor.
     */
    public function ScreenMedia (desc :MediaData)
    {
        _desc = desc;
        _id = int(Math.random() * int.MAX_VALUE);

        var url :String = desc.URL;
        if (desc.isAVM1) {
            // TODO
            url += "?oid=" + _id;
        }

/** Experimental
        if (url.toLowerCase().lastIndexOf(".flv") ==
                url.length - ".flv".length) {
            var nc :NetConnection = new NetConnection();
            nc.connect(url);

            var stream :NetStream = new NetStream(nc);

            var video :Video = new Video();
            video.attachNetStream(stream);

            rawChildren.addChild(video);
            stream.play();

            return;
        }
**/

        // create our loader and set up some event listeners
        var loader :Loader = new Loader();
        loader.contentLoaderInfo.addEventListener(
            Event.COMPLETE, loadingComplete);
        loader.contentLoaderInfo.addEventListener(
            IOErrorEvent.IO_ERROR, loadError);

        // grab hold of the EventDispatcher we'll use for comm
        _dispatch = loader.contentLoaderInfo.sharedEvents;

        // if we know the size of the media, create a mask to prevent
        // it from drawing outside those bounds
        if (desc.width != -1 && desc.height != -1) {
            var mask :Shape = new Shape();
            with (mask.graphics) {
                beginFill(0xFFFFFF);
                drawRect(0, 0, desc.width, desc.height);
                endFill();
            }
            // the mask must be added to the display list (which is wacky)
            rawChildren.addChild(mask);
            loader.mask = mask;
        }

        // start it loading, add it as a child
        loader.load(new URLRequest(url), getContext(url));
        rawChildren.addChild(loader);

        if (desc.isInteractive()) {
            addEventListener(MouseEvent.MOUSE_OVER, mouseOver);
            addEventListener(MouseEvent.MOUSE_OUT, mouseOut);
            addEventListener(MouseEvent.CLICK, mouseClick);
        }

        // I don't know if these lines are necessary: remove?
        if (desc.width != -1 && desc.height != -1) {
            width = desc.width;
            height = desc.height;
        }

        //addEventListener(Event.ENTER_FRAME, tick);
    }

    // documentation inherited
    public override function set x (newValue :Number) :void
    {
        // TODO: test
        super.x = newValue - _desc.originX;
    }

    // documentation inherited
    public override function get x () :Number
    {
        // TODO: test
        return (super.x + _desc.originX);
    }

    // documentation inherited
    public override function set y (newValue :Number) :void
    {
        // TODO: test
        super.y = newValue - _desc.originY;
    }

    // documentation inherited
    public override function get y () :Number
    {
        // TODO: test
        return (super.y + _desc.originY);
    }

    /**
     * Accessor: media property.
     */
    // Probably this should be removed.
    public function get media () :DisplayObject
    {
        // untested
        // TODO: needed? remove?
        for (var ii :int = rawChildren.numChildren - 1; ii >= 0; ii--) {
            var disp :DisplayObject = rawChildren.getChildAt(ii);
            if (disp is Loader) {
                return (disp as Loader).content;

            } else if (!(disp is Shape)) {
                return disp;
            }
        }
        return null; // never found!
    }

    /**
     * Send a message to the client swf that we're representing.
     */
    public function sendMessage (type :String, msg :String) :void
    {
        trace("sent [" + type + "=" + msg + "]");
        // simple post an event across the security boundary
        _dispatch.dispatchEvent(new TextEvent(type, false, false, msg));

        if (_desc.isAVM1) {
            if (_oldDispatch == null) {
                _oldDispatch = new LocalConnection();
                _oldDispatch.allowDomain("*");
                _oldDispatch.addEventListener(
                    StatusEvent.STATUS, onLocalConnStatus);
            }
            trace("dispatching on \"_msoy" + _id + "\".");
            try {
                _oldDispatch.send("_msoy" + _id, type, msg);
            } catch (e :Error) {
                // nada
            }
        }
    }

    /**
     * A callback called when there is a status event from using
     * the local connection.
     */
    protected static function onLocalConnStatus (event :StatusEvent) :void
    {
        // This method exists because if we don't eat status-error messages
        // then they end up bubbling up somewhere else.

        //trace("statusEvent: " + event);
    }

    protected function getContext (url :String) :LoaderContext
    {
        var loadCtx :LoaderContext = (_loadCtx.get(url) as LoaderContext);
        if (loadCtx == null) {
            trace("Creating new loadctx for " + url);
            loadCtx = new LoaderContext(
                false,
                new ApplicationDomain(ApplicationDomain.currentDomain),
                null
                );
            _loadCtx.put(url, loadCtx);
        }
        return loadCtx;
    }

    /**
     * Remove our listeners from the LoaderInfo object.
     */
    protected function removeListeners (info :LoaderInfo) :void
    {
        info.removeEventListener(Event.COMPLETE, loadingComplete);
        info.removeEventListener(IOErrorEvent.IO_ERROR, loadError);
    }

    /**
     * Callback function.
     */
    protected function loadError (event :IOErrorEvent) :void
    {
        var info :LoaderInfo = (event.target as LoaderInfo);
        removeListeners(info);

        // remove all children
        for (var ii :int = rawChildren.numChildren - 1; ii >= 0; ii--) {
            rawChildren.removeChildAt(ii);
        }

        // create a 'broken media' image and use that instead
        var w :int = _desc.width;
        var h :int = _desc.height;
        if (w == -1) {
            w = 100;
        }
        if (h == -1) {
            h = 100;
        }
        rawChildren.addChild(ImageUtil.createErrorImage(w, h));
    }

    /**
     * Callback function.
     */
    protected function loadingComplete (event :Event) :void
    {
        var info :LoaderInfo = (event.target as LoaderInfo);
        removeListeners(info);

        // Try accessing the 'content' property and see if that generates
        // a security error. If so, leave it where it is.
        try {
            info.content; // access
        } catch (err :SecurityError) {
            return;
        }

        // remove all our "raw" children, which will remove the loader
        for (var ii :int = rawChildren.numChildren - 1; ii >= 0; ii--) {
            rawChildren.removeChildAt(ii);
        }

        // now add the content as our child, letting the Loader get gc'd.
        rawChildren.addChild(info.content);

        // transfer the mask, if any
        info.content.mask = info.loader.mask;
        if (info.content.mask != null) {
            rawChildren.addChild(info.content.mask);
        }
    }

    /**
     * Callback function.
     */
    protected function mouseOver (event :MouseEvent) :void
    {
        if (_glow == null) {
            _glow = new Glow(this);
            _glow.alphaFrom = 0;
            _glow.alphaTo = 1;
            _glow.blurXFrom = 0;
            _glow.blurXTo = 20;
            _glow.blurYFrom = 0;
            _glow.blurYTo = 20;
            _glow.color = 0x40e0e0;
            _glow.duration = 200;
        }

        _glow.play();
    }

    /**
     * Callback function.
     */
    protected function mouseOut (event :MouseEvent) :void
    {
        if (_glow != null) {
            _glow.end();
            _glow = null;

            // remove the GlowFilter that is added
            // TODO: maybe ensure there are no other filters that
            // need preserving
            filters = new Array();
        }
    }

    /**
     * Callback function.
     */
    protected function mouseClick (event :MouseEvent) :void
    {
        var look :String = (Math.random() < .5) ? "red" : "blue";
        sendMessage("setLook", look);
    }

/*
    protected function tick (event :Event) :void
    {
        if (!mouseEnabled) {
            trace("mouse was disabled on media: " + _desc.URL);
            mouseEnabled = true;
        }
        if (mouseChildren) {
            trace("mousechildren enabled on " + _desc.URL);
            // setting this to false makes swfs not capture mouse input
            // so that mouse hover, etc, work.
            mouseChildren = false;
        }
    }
*/

    protected var _id :int;

    /** Our Media descripter. */
    protected var _desc :MediaData;

    /** Used to dispatch events down to the swf we contain. */
    protected var _dispatch :EventDispatcher;

    /** The glow effect used for mouse hovering. */
    protected var _glow :Glow;

    /** A single LocalConnection used to communicate with all AVM1 media. */
    protected static var _oldDispatch :LocalConnection;

    protected static var _loadCtx :HashMap = new HashMap();
}

}
