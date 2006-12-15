//
// $Id$

package client.inventory;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.Window;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.KeyboardListenerAdapter;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.TabPanel;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.TextBoxBase;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import org.gwtwidgets.client.ui.FileUploadField;
import org.gwtwidgets.client.ui.FormPanel;

import com.threerings.msoy.item.web.Item;
import com.threerings.msoy.item.web.MediaDesc;

import client.util.BorderedPopup;
import client.util.MsoyUI;
import client.util.RowPanel;
import client.util.WebContext;

/**
 * The base class for an interface for creating and editing digital items.
 */
public abstract class ItemEditor extends BorderedPopup
{
    public static interface Binder
    {
        public void textUpdated (String newText);
    }

    public static interface MediaUpdater
    {
        /**
         * Return null, or a message indicating why the specified media
         * will not do.
         */
        public String updateMedia (MediaDesc desc);
    }

    public ItemEditor ()
    {
        super(false);

        VerticalPanel content = new VerticalPanel();
        content.setStyleName("itemEditor");
        content.add(_etitle = MsoyUI.createLabel("title", "Title"));

        TabPanel tabs;
        content.add(tabs = new TabPanel());
        tabs.setStyleName("Tabs");

        // the main tab will contain the base metadata and primary media uploader
        VerticalPanel main = new VerticalPanel();
        main.setStyleName("Tab");
        createMainInterface(main);
        tabs.add(main, "Main");

        // the extra tab will contain the furni and thumbnail media and description
        VerticalPanel extra = new VerticalPanel();
        extra.setStyleName("Tab");
        createExtraInterface(extra);
        tabs.add(extra, "Extra");

        // start with main selected
        tabs.selectTab(0);

        RowPanel buttons = new RowPanel();
        buttons.setStyleName("Buttons");
        buttons.add(_esubmit = new Button("submit"));
        _esubmit.setEnabled(false);
        _esubmit.addClickListener(new ClickListener() {
            public void onClick (Widget widget) {
                commitEdit();
            }
        });
        Button ecancel;
        buttons.add(ecancel = new Button("Cancel"));
        ecancel.addClickListener(new ClickListener() {
            public void onClick (Widget widget) {
                _parent.editComplete(null);
                hide();
            }
        });
        content.setHorizontalAlignment(VerticalPanel.ALIGN_RIGHT);
        content.add(buttons);

        setWidget(content);
    }

    /**
     * Configures this editor with a reference to the item service and its item
     * panel parent.
     */
    public void init (WebContext ctx, ItemPanel parent)
    {
        _ctx = ctx;
        _parent = parent;
    }

    /**
     * Configures this editor with an item to edit. The item may be freshly
     * constructed if we are using the editor to create a new item.
     */
    public void setItem (Item item)
    {
        _item = item;
        _etitle.setText((item.itemId <= 0) ? "Upload a New Item" : "Edit an Item");
        _esubmit.setText((item.itemId <= 0) ? "Upload" : "Update");

        _name.setText(_item.name);

        recheckFurniMedia();
        recheckThumbMedia();

        updateSubmittable();
    }

    /**
     * Returns the currently configured item.
     */
    public Item getItem ()
    {
        return _item;
    }

    /**
     * Instructs this editor to reopen the item inspector if the item is updated.
     */
    public void reinspectOnUpdate (boolean reinspectOnUpdate)
    {
        _reinspectOnUpdate = reinspectOnUpdate;
    }

    // @Override // from Widget
    protected void onLoad ()
    {
        super.onLoad();
        configureBridge();

        // recenter immediately and then do it again in a deferred command;
        // TODO: figure out how to trigger on an image being fully loaded and
        // have that result in calls to recenter(); yay for asynchronous layout
        recenter(false);
        recenter(true);
    }

    /**
     * Derived classes can add editors to the main tab by overriding this method.
     */
    protected void createMainInterface (VerticalPanel main)
    {
        // we have to do this wacky singleton crap because GWT and/or JavaScript doesn't seem to
        // cope with our trying to create an anonymous function that calls an instance method on a
        // JavaScript object
        _singleton = this;

        // create a name entry field
        main.add(createRow("Name:", bind(_name = new TextBox(), new Binder() {
            public void textUpdated (String text) {
                _item.name = text;
            }
        })));
    }

    /**
     * Derived classes can add editors to the main tab by overriding this method.
     */
    protected void createExtraInterface (VerticalPanel extra)
    {
        extra.add(createRow("Description", bind(_description = new TextArea(), new Binder() {
            public void textUpdated (String text) {
                _item.description = text;
            }
        })));
        _description.setCharacterWidth(40);
        _description.setVisibleLines(3);

        String title = "Furniture Image";
        if (_furniUploader == null) {
            _furniUploader = new MediaUploader(Item.FURNI_ID, title, true, new MediaUpdater() {
                public String updateMedia (MediaDesc desc) {
                    if (!desc.hasFlashVisual()) {
                        return "Furniture must be an web-viewable image type.";
                    }
                    _item.furniMedia = desc;
                    recenter(true);
                    return null;
                }
            });
            extra.add(_furniUploader);
        }

        title = "Thumbnail Image";
        _thumbUploader = new MediaUploader(Item.THUMB_ID, title, true, new MediaUpdater() {
            public String updateMedia (MediaDesc desc) {
                if (!desc.isImage()) {
                    return "Thumbnails must be an image type.";
                }
                _item.thumbMedia = desc;
                recenter(true);
                return null;
            }
        });
        extra.add(_thumbUploader);
    }

    protected RowPanel createRow (String label, Widget widget)
    {
        RowPanel row = new RowPanel();
        row.add(new Label(label));
        row.add(widget);
        return row;
    }

    /**
     * Recenters our popup. This should be called when media previews are changed.
     */
    protected void recenter (boolean defer)
    {
        if (defer) {
            DeferredCommand.add(new Command() {
                public void execute () {
                    recenter(false);
                }
            });
        } else {
            setPopupPosition((Window.getClientWidth() - getOffsetWidth()) / 2,
                             (Window.getClientHeight() - getOffsetHeight()) / 2);
        }
    }

    /**
     * This should be called by item editors that are used for editing
     * media that has a 'main' piece of media.
     */
    protected MediaUploader createMainUploader (String title, MediaUpdater updater)
    {
        return (_mainUploader = new MediaUploader(Item.MAIN_ID, title, false, updater));
    }

    /**
     * Editors should override this method to indicate when the item is in a
     * consistent state and may be uploaded.
     */
    protected boolean itemConsistent ()
    {
        return (_item != null) && _item.isConsistent();
    }

    /**
     * Get the MediaUploader with the specified id.
     */
    protected MediaUploader getUploader (String id)
    {
        if (Item.FURNI_ID.equals(id)) {
            return _furniUploader;

        } else if (Item.THUMB_ID.equals(id)) {
            return _thumbUploader;

        } else if (Item.MAIN_ID.equals(id)) {
            return _mainUploader; // could be null...

        } else {
            return null;
        }
    }

    /**
     * Configures this item editor with the hash value for media that it is
     * about to upload.
     */
    protected void setHash (String id, String mediaHash, int mimeType, int constraint,
                            String thumbMediaHash, int thumbMimeType)
    {
        MediaUploader mu = getUploader(id);
        if (mu == null) {
            return; // TODO: log something? in gwt land?
        }

        // set the new media in preview and in the item
        mu.setUploadedMedia(
            new MediaDesc(MediaDesc.stringToHash(mediaHash), (byte)mimeType, (byte)constraint));

        // if we got thumbnail media back from this upload, use that as well
        // TODO: avoid overwriting custom thumbnail, sigh
        if (thumbMediaHash.length() > 0) {
            _item.thumbMedia = new MediaDesc(
                MediaDesc.stringToHash(thumbMediaHash), (byte)thumbMimeType);
        }

        // have the item re-validate that no media ids are duplicated
        // unnecessarily
        _item.checkConsolidateMedia();

        // re-check the other two, as they may have changed
        if (!Item.THUMB_ID.equals(id)) {
            recheckThumbMedia();
        }
        if (!Item.FURNI_ID.equals(id)) {
            recheckFurniMedia();
        }

        // re-check submittable
        updateSubmittable();
    }

    /**
     * Called to re-set the displayed furni media to the MediaDesc
     * returned by the item.
     */
    protected void recheckFurniMedia ()
    {
        if (_furniUploader != null) {
            _furniUploader.setMedia(_item.getFurniMedia());
        }
    }

    /**
     * Called to re-set the displayed thumb media to the MediaDesc
     * returned by the item.
     */
    protected void recheckThumbMedia ()
    {
        if (_thumbUploader != null) {
            _thumbUploader.setMedia(_item.getThumbnailMedia());
        }
    }

    /**
     * This is called from our magical JavaScript method by JavaScript code
     * received from the server as a response to our file upload POST request.
     */
    protected static void callBridge (String id, String mediaHash, int mimeType, int constraint,
                                      String thumbMediaHash, int thumbMimeType)
    {
        _singleton.setHash(id, mediaHash, mimeType, constraint, thumbMediaHash, thumbMimeType);
    }

    /**
     * Editors should call this method when something changes that might render
     * an item consistent or inconsistent. It will update the enabled status of
     * the submit button.
     */
    protected void updateSubmittable ()
    {
        _esubmit.setEnabled(itemConsistent());
    }

    /**
     * Called when the user has clicked the "update" or "create" button to
     * commit their edits or create a new item, respectively.
     */
    protected void commitEdit ()
    {
        AsyncCallback cb = new AsyncCallback() {
            public void onSuccess (Object result) {
                _parent.setStatus(_item.itemId == 0 ?
                                  "Item created." : "Item updated.");
                _parent.editComplete(_item);
                if (_reinspectOnUpdate) {
                    new ItemDetailPopup(_ctx, _item, _parent).show();
                }
                hide();
            }
            public void onFailure (Throwable caught) {
                String reason = caught.getMessage();
                _parent.setStatus(_item.itemId == 0 ?
                                  "Item creation failed: " + reason :
                                  "Item update failed: " + reason);
            }
        };
        if (_item.itemId == 0) {
            _ctx.itemsvc.createItem(_ctx.creds, _item, cb);
        } else {
            _ctx.itemsvc.updateItem(_ctx.creds, _item, cb);
        }
    }

    /**
     * Creates a blank item for use when creating a new item using this editor.
     */
    protected abstract Item createBlankItem ();

    /**
     * A convenience method for attaching a textbox directly to a field in the
     * item to be edited.
     *
     * TODO: If you paste text into the field, this doesn't detect it.
     */
    protected TextBoxBase bind (final TextBoxBase textbox, final Binder binder)
    {
        textbox.addKeyboardListener(new KeyboardListenerAdapter() {
            public void onKeyPress (Widget sender, char keyCode, int mods) {
                if (_item != null) {
                    DeferredCommand.add(new Command() {
                        public void execute () {
                            binder.textUpdated(textbox.getText());
                            updateSubmittable();
                        }
                    });
                }
            }
        });
        return textbox;
    }

    /**
     * This wires up a sensibly named function that our POST response
     * JavaScript code can call.
     */
    protected static native void configureBridge () /*-{
        $wnd.setHash = function (id, hash, type, constraint, thash, ttype) {
           @client.inventory.ItemEditor::callBridge(Ljava/lang/String;Ljava/lang/String;IILjava/lang/String;I)(id, hash, type, constraint, thash, ttype);
        };
    }-*/; 

    protected WebContext _ctx;
    protected ItemPanel _parent;

    protected Item _item;
    protected boolean _reinspectOnUpdate;

    protected VerticalPanel _content;

    protected Label _etitle;
    protected TextBox _name;
    protected TextArea _description;
    protected Button _esubmit;

    protected static ItemEditor _singleton;

    protected MediaUploader _thumbUploader;
    protected MediaUploader _furniUploader;
    protected MediaUploader _mainUploader;
}
