//
// $Id$

package client.whirled;

import client.msgs.CMsgs;

import com.threerings.msoy.web.client.WorldServiceAsync;

/**
 * Extends {@link CShell} and provides group-specific services.
 */
public class CWhirled extends CMsgs
{
    /** Provides world-related services. */
    public static WorldServiceAsync worldsvc;

    /** Messages used by the my whirled/whirledwide interfaces. */
    public static WhirledMessages msgs;
}
