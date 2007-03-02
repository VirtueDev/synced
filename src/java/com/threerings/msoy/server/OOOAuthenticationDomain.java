//
// $Id$

package com.threerings.msoy.server;

import com.samskivert.io.PersistenceException;
import com.samskivert.net.MailUtil;

import com.samskivert.servlet.user.InvalidUsernameException;
import com.samskivert.servlet.user.Password;
import com.samskivert.servlet.user.UserExistsException;
import com.samskivert.servlet.user.Username;

import com.threerings.user.OOOUser;
import com.threerings.user.OOOUserManager;
import com.threerings.user.OOOUserRepository;

import com.threerings.msoy.data.MsoyAuthCodes;
import com.threerings.msoy.data.MsoyTokenRing;
import com.threerings.msoy.web.data.ServiceException;

import static com.threerings.msoy.Log.log;

/**
 * Implements account authentication against the OOO global user database.
 */
public class OOOAuthenticationDomain
    implements MsoyAuthenticator.Domain
{
    // from interface MsoyAuthenticator.Domain
    public void init ()
        throws PersistenceException
    {
        // we get our user manager configuration from the ocean config
        _usermgr = new OOOUserManager(
            ServerConfig.config.getSubProperties("oooauth"), MsoyServer.conProv);
        _authrep = (OOOUserRepository)_usermgr.getRepository();
    }

    // from interface MsoyAuthenticator.Domain
    public MsoyAuthenticator.Account createAccount (String accountName, String password)
        throws ServiceException, PersistenceException
    {
        // make sure this account is not already in use
        if (MsoyServer.memberRepo.loadMember(accountName) != null) {
            throw new ServiceException(MsoyAuthCodes.DUPLICATE_EMAIL);
        }

        // create a new account record
        int userId;
        try {
            userId = _authrep.createUser(
                new MsoyUsername(accountName), Password.makeFromCrypto(password), accountName,
                OOOUser.METASOY_SITE_ID, 0);
        } catch (InvalidUsernameException iue) {
            throw new ServiceException(MsoyAuthCodes.INVALID_EMAIL);
        } catch (UserExistsException uee) {
            throw new ServiceException(MsoyAuthCodes.DUPLICATE_EMAIL);
        }

        // load up our newly created record
        OOOUser user = (OOOUser)_authrep.loadUser(userId);
        if (user == null) {
            throw new ServiceException(MsoyAuthCodes.SERVER_ERROR);
        }
        _authrep.loadMachineIdents(user);

        // create and return an account metadata record
        OOOAccount account = new OOOAccount();
        account.accountName = user.username;
        account.tokens = new MsoyTokenRing();
        account.record = user;
        return account;
    }

    // from interface MsoyAuthenticator.Domain
    public MsoyAuthenticator.Account authenticateAccount (String accountName, String password)
        throws ServiceException, PersistenceException
    {
        // load up their user account record
        OOOUser user = _authrep.loadUserByEmail(accountName, true);
        if (user == null) {
            throw new ServiceException(MsoyAuthCodes.NO_SUCH_USER);
        }

        // now check their password
        if (PASSWORD_BYPASS != password && !user.password.equals(password)) {
            throw new ServiceException(MsoyAuthCodes.INVALID_PASSWORD);
        }

        // configure their access tokens
        int tokens = 0;
        if (user.holdsToken(OOOUser.ADMIN) || user.holdsToken(OOOUser.MAINTAINER)) {
            tokens |= MsoyTokenRing.ADMIN;
        }
        if (user.holdsToken(OOOUser.SUPPORT)) {
            tokens |= MsoyTokenRing.SUPPORT;
        }

        // create and return an account record
        OOOAccount account = new OOOAccount();
        account.accountName = user.username;
        account.tokens = new MsoyTokenRing(tokens);
        account.record = user;
        return account;
    }

    // from interface MsoyAuthenticator.Domain
    public void validateAccount (MsoyAuthenticator.Account account, String machIdent)
        throws ServiceException, PersistenceException
    {
        OOOAccount oooacc = (OOOAccount)account;
        int rv = _authrep.validateUser(
            OOOUser.METASOY_SITE_ID, oooacc.record, machIdent, account.firstLogon);
        switch (rv) {
        case OOOUserRepository.ACCOUNT_BANNED:
            throw new ServiceException(MsoyAuthCodes.BANNED);
        case OOOUserRepository.NEW_ACCOUNT_TAINTED:
            throw new ServiceException(MsoyAuthCodes.MACHINE_TAINTED);
        }
        // TODO: do we care about other badness like DEADBEAT?
    }

    // from interface MsoyAuthenticator.Domain
    public void validateAccount (MsoyAuthenticator.Account account)
        throws ServiceException, PersistenceException
    {
        OOOAccount oooacc = (OOOAccount)account;
        if (oooacc.record.isBanned(OOOUser.METASOY_SITE_ID)) {
            throw new ServiceException(MsoyAuthCodes.BANNED);
        }
        // TODO: do we care about other badness like DEADBEAT?
    }

    protected static class OOOAccount extends MsoyAuthenticator.Account
    {
        public OOOUser record;
    }

    protected static class MsoyUsername extends Username
    {
        public MsoyUsername (String username) throws InvalidUsernameException {
            super(username);
        }
        @Override
        protected void validateName (String username) throws InvalidUsernameException {
            if (!MailUtil.isValidAddress(username)) {
                throw new InvalidUsernameException("");
            }
        }
    }

    protected OOOUserRepository _authrep;
    protected OOOUserManager _usermgr;
}
