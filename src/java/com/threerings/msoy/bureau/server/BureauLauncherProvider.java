//
// $Id$

package com.threerings.msoy.bureau.server;

import com.threerings.msoy.admin.gwt.BureauLauncherInfo;
import com.threerings.msoy.bureau.client.BureauLauncherService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationProvider;

/**
 * Defines the server-side of the {@link BureauLauncherService}.
 */
public interface BureauLauncherProvider extends InvocationProvider
{
    /**
     * Handles a {@link BureauLauncherService#launcherInitialized} request.
     */
    void launcherInitialized (ClientObject caller);

    /**
     * Handles a {@link BureauLauncherService#setBureauLauncherInfo} request.
     */
    void setBureauLauncherInfo (ClientObject caller, BureauLauncherInfo arg1);
}
