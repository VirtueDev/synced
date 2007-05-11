//
// $Id$

package client.editem;

import com.google.gwt.user.client.ui.TabPanel;
import com.google.gwt.user.client.ui.VerticalPanel;

import com.threerings.msoy.item.data.all.Avatar;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.MediaDesc;

/**
 * A class for creating and editing {@link Avatar} digital items.
 */
public class AvatarEditor extends ItemEditor
{
    // @Override from ItemEditor
    public void setItem (Item item)
    {
        super.setItem(item);
        _avatar = (Avatar)item;
        _mainUploader.setMedia(_avatar.avatarMedia);
    }

    // @Override from ItemEditor
    public Item createBlankItem ()
    {
        return new Avatar();
    }

    // @Override from ItemEditor
    protected void createInterface (VerticalPanel contents, TabPanel tabs)
    {
        tabs.add(createMainUploader(CEditem.emsgs.avatarMainTitle(), new MediaUpdater() {
            public String updateMedia (MediaDesc desc, int width, int height) {
                if (!desc.hasFlashVisual()) {
                    return CEditem.emsgs.errAvatarNotFlash();
                }
                _avatar.avatarMedia = desc;
                _avatar.scale = 1f;
                return null;
            }
        }), CEditem.emsgs.avatarMainTab());

        super.createInterface(contents, tabs);
    }

    // @Override from ItemEditor
    protected void createFurniUploader (TabPanel tabs)
    {
        // nada: avatars should no longer have a furni visualization
    }

    // @Override from ItemEditor
    protected void createThumbUploader (TabPanel tabs)
    {
        String title = CEditem.emsgs.avatarThumbTitle();
        _thumbUploader = createUploader(Item.THUMB_MEDIA, title, true, new MediaUpdater() {
            public String updateMedia (MediaDesc desc, int width, int height) {
                if (!desc.isImage()) {
                    return CEditem.emsgs.errThumbNotImage();
                }
                _item.thumbMedia = desc;
                return null;
            }
        });
        tabs.add(_thumbUploader, CEditem.emsgs.avatarThumbTab());
    }

    protected Avatar _avatar;
}
