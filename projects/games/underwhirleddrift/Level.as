package {

import flash.display.Sprite;
import flash.display.Shape;
import flash.display.Bitmap;
import flash.display.BitmapData;
import flash.display.DisplayObject;

import flash.geom.Point;
import flash.geom.Matrix;
import flash.geom.Rectangle;

import flash.utils.describeType;

public class Level extends Sprite
{
    public var log :Log = Log.getLog(UnderwhirledDrift);

    public function Level (level :int)
    {
        var backgroundImage :Shape;
        var backgroundSprite :Sprite = new BACKGROUNDS[level]();
        var backgroundBitmap :BitmapData = new BitmapData(backgroundSprite.width, 
            backgroundSprite.height);
        var backgroundTrans :Matrix = new Matrix();
        backgroundTrans.translate(backgroundSprite.width / 2, backgroundSprite.height / 2);
        backgroundBitmap.draw(backgroundSprite, backgroundTrans);
        for (var ii :int = 0; ii < 4; ii++) {
            backgroundImage = new Shape();
            backgroundImage.graphics.beginBitmapFill(backgroundBitmap);
            // TODO: have background tiling react to the size of the level map properly
            backgroundImage.graphics.drawRect(0, 0, 1536, 1536);
            backgroundImage.graphics.endFill();
            if (ii < 2) {
                backgroundImage.x = -1536;
            }
            if ((ii % 2) == 0) {
                backgroundImage.y = -1536;
            }
            addChild(backgroundImage);
        }

        addChild(new ROUGHS[level]());
        addChild(_track = new TRACKS[level]());
        addChild(new WALLS[level]());
    }

    public function isOnRoad (loc :Point) :Boolean
    {
        var imgData :BitmapData = new BitmapData(1, 1, true, 0);
        var trans :Matrix = new Matrix();
        trans.translate(-loc.x, -loc.y);
        imgData.draw(_track, trans);
        return (imgData.getPixel32(0, 0) & 0xFF000000) != 0;
    }

    /** Embedded tracks */
    [Embed(source='rsrc/level_1.swf#track')]
    protected static const TRACK_LEVEL_1 :Class;
    protected static const TRACKS :Array = [ TRACK_LEVEL_1 ];

    /** Embedded roughs */
    [Embed(source='rsrc/level_1.swf#rough')]
    protected static const ROUGH_LEVEL_1 :Class;
    protected static const ROUGHS :Array = [ ROUGH_LEVEL_1 ];

    /** Embedded walls */
    [Embed(source='rsrc/level_1.swf#wall')]
    protected static const WALL_LEVEL_1 :Class;
    protected static const WALLS :Array = [ WALL_LEVEL_1 ];

    /** Embedded background tiles */
    [Embed(source='rsrc/level_1.swf#background')]
    protected static const BACKGROUND_LEVEL_1 :Class;
    protected static const BACKGROUNDS :Array = [ BACKGROUND_LEVEL_1 ];

    protected var _track :DisplayObject;
}
}
