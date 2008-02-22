/*************************************************************************
 * 
 * ADOBE CONFIDENTIAL
 * __________________
 * 
 *  [2002] - [2007] Adobe Systems Incorporated 
 *  All Rights Reserved.
 * 
 * NOTICE:  All information contained herein is, and remains
 * the property of Adobe Systems Incorporated and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Adobe Systems Incorporated
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Adobe Systems Incorporated.
 */
package
{

/**
 *  @private
 *  In some projects, this class is used to link additional classes
 *  into the SWC beyond those that are found by dependency analysis
 *  starting from the classes specified in manifest.xml.
 *  This project has no manifest file (because there are no MXML tags
 *  corresponding to any classes in it) so all the classes linked into
 *  the SWC are found by a dependency analysis starting from the classes
 *  listed here.
 */
internal class FlexClasses
{
	import mx.core.BitmapAsset; BitmapAsset;
	import mx.core.ByteArrayAsset; ByteArrayAsset;
	import mx.core.ButtonAsset; ButtonAsset;
	import mx.core.FontAsset; FontAsset;
	import mx.core.FlexLoader; FlexLoader;
	import mx.core.IFlexModule; IFlexModule;
	import mx.core.MovieClipAsset; MovieClipAsset;
	import mx.core.MovieClipLoaderAsset; MovieClipLoaderAsset;
	import mx.core.SimpleApplication; SimpleApplication;
	import mx.core.SoundAsset; SoundAsset;
	import mx.core.SpriteAsset; SpriteAsset;
	import mx.core.TextFieldAsset; TextFieldAsset;
	import mx.events.ModuleEvent; ModuleEvent;
	import mx.modules.IModuleInfo; IModuleInfo;
	import mx.modules.ModuleBase; ModuleBase;
	import mx.modules.ModuleManager; ModuleManager;
	import mx.resources.IResourceBundle; IResourceBundle;
	import mx.resources.IResourceManager; IResourceManager;
	import mx.resources.IResourceModule; IResourceModule;
	import mx.resources.Locale; Locale;
	import mx.resources.ResourceBundle; ResourceBundle;
	import mx.resources.ResourceManager; ResourceManager;
	// Maintain alphabetical order
}

}
