//
// $Id$

package com.threerings.msoy.server;

import java.util.List;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.jdbc.WriteOnlyUnit;
import com.samskivert.util.Invoker;

import com.threerings.util.Name;

import com.threerings.presents.annotation.EventThread;
import com.threerings.presents.annotation.MainInvoker;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.InvocationManager;
import com.threerings.presents.util.PersistingUnit;

import com.threerings.crowd.chat.server.ChatProvider;

import com.threerings.msoy.chat.data.MsoyChatCodes;

import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.MsoyCodes;
import com.threerings.msoy.data.UserAction;
import com.threerings.msoy.data.all.VisitorInfo;
import com.threerings.msoy.server.MemberLocal;
import com.threerings.msoy.server.MemberLogic;
import com.threerings.msoy.server.MsoyEventLogger;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.server.persist.MemberRepository;
import com.threerings.msoy.server.util.MailSender;
import com.threerings.msoy.server.util.ServiceUnit;

import com.threerings.msoy.admin.data.CostsConfigObject;
import com.threerings.msoy.admin.server.RuntimeConfig;

import com.threerings.msoy.web.gwt.Args;
import com.threerings.msoy.web.gwt.Pages;
import com.threerings.msoy.web.gwt.ServiceException;

import com.threerings.msoy.badge.data.BadgeType;
import com.threerings.msoy.badge.data.all.EarnedBadge;
import com.threerings.msoy.money.data.all.Currency;
import com.threerings.msoy.money.data.all.PriceQuote;
import com.threerings.msoy.money.data.all.TransactionType;
import com.threerings.msoy.money.server.MoneyException;
import com.threerings.msoy.money.server.MoneyLogic;
import com.threerings.msoy.money.server.MoneyServiceException;
import com.threerings.msoy.money.server.MoneyLogic.BuyOperation;
import com.threerings.msoy.money.server.persist.MoneyRepository;
import com.threerings.msoy.notify.server.NotificationManager;

import static com.threerings.msoy.Log.log;

/**
 * Handles global runtime services (that are not explicitly member related and thus not handled by
 * MemberManager}.
 */
@Singleton @EventThread
public class MsoyManager
    implements MsoyProvider
{
    @Inject public MsoyManager (InvocationManager invmgr)
    {
        // register our bootstrap invocation service
        invmgr.registerDispatcher(new MsoyDispatcher(this), MsoyCodes.MSOY_GROUP);
    }

    // from interface MemberProvider
    public void setHearingGroupChat (
        ClientObject caller, int groupId, boolean hear, InvocationService.ConfirmListener listener)
        throws InvocationException
    {
        caller.getLocal(MemberLocal.class).setHearingGroupChat(groupId, hear);
        listener.requestProcessed();
    }

    // from interface MemberProvider
    public void emailShare (ClientObject caller, boolean isGame, String placeName, int placeId,
                            String[] emails, String message, boolean friend,
                            InvocationService.ConfirmListener cl)
    {
        final MemberObject memObj = (MemberObject) caller;
        Pages page = isGame ? Pages.GAMES : Pages.WORLD;
        String args = Args.compose(isGame ? "d" : "s", placeId);
        int memberId = memObj.getMemberId();
        String url = friend ? page.makeFriendURL(memberId, args) :
                              page.makeAffiliateURL(memberId, args);
        final String template = isGame ? "shareGameInvite" : "shareRoomInvite";
        // username is their authentication username which is their email address
        final String from = memObj.username.toString();
        for (final String recip : emails) {
            // this just passes the buck to an executor, so we can call it from the dobj thread
            _mailer.sendTemplateEmail(
                MailSender.By.HUMAN, recip, from, template, "inviter", memObj.memberName,
                "name", placeName, "message", message, "link", url);
        }
        cl.requestProcessed();
    }

    // from interface MemberProvider
    public void trackVectorAssociation (ClientObject caller, final String vector)
    {
        final VisitorInfo info = ((MemberObject)caller).visitorInfo;
        _invoker.postUnit(new WriteOnlyUnit("trackVectorAssociation") {
            @Override public void invokePersist () throws Exception {
                _memberLogic.trackVectorAssociation(info, vector);
            }
        });
    }

    // from interface MemberProvider
    public void getABTestGroup (final ClientObject caller, final String testName,
        final boolean logEvent, final InvocationService.ResultListener listener)
    {
        final VisitorInfo vinfo = ((MemberObject)caller).visitorInfo;
        _invoker.postUnit(new PersistingUnit("getABTestGroup", listener) {
            @Override public void invokePersistent () throws Exception {
                _testGroup = _memberLogic.getABTestGroup(testName, vinfo, logEvent);
            }
            @Override public void handleSuccess () {
                reportRequestProcessed(_testGroup);
            }
            protected Integer _testGroup;
        });
    }

    // from interface MemberProvider
    public void trackClientAction (final ClientObject caller, final String actionName,
        final String details)
    {
        final MemberObject memObj = (MemberObject) caller;
        if (memObj.visitorInfo == null) {
            log.warning("Failed to log client action with null visitorInfo", "caller", caller.who(),
                        "actionName", actionName);
            return;
        }
        _eventLog.clientAction(memObj.getVisitorId(), actionName, details);
    }

    // from interface MemberProvider
    public void trackTestAction (final ClientObject caller, final String actionName,
        final String testName)
    {
        final VisitorInfo vinfo = ((MemberObject) caller).visitorInfo;
        final String visitorId =  ((MemberObject) caller).getVisitorId();
        if (vinfo == null) {
            log.warning("Failed to log test action with null visitorInfo", "caller", caller.who(),
                        "actionName", actionName);
            return;
        }

        _invoker.postUnit(new Invoker.Unit("getABTestGroup") {
            @Override public boolean invoke () {
                int abTestGroup = -1;
                String actualTestName;
                if (testName != null) {
                    // grab the group without logging a tracking event about it
                    abTestGroup = _memberLogic.getABTestGroup(testName, vinfo, false);
                    actualTestName = testName;
                } else {
                    actualTestName = "";
                }
                _eventLog.testAction(visitorId, actionName, actualTestName, abTestGroup);
                return false;
            }
        });
    }

    // from interface MemberProvider
    public void loadAllBadges (ClientObject caller, InvocationService.ResultListener listener)
        throws InvocationException
    {
        long now = System.currentTimeMillis();
        List<EarnedBadge> badges = Lists.newArrayList();
        for (BadgeType type : BadgeType.values()) {
            int code = type.getCode();
            for (int ii = 0; ii < type.getNumLevels(); ii++) {
                String levelUnits = type.getRequiredUnitsString(ii);
                int coinValue = type.getCoinValue(ii);
                badges.add(new EarnedBadge(code, ii, levelUnits, coinValue, now));
            }
        }

        listener.requestProcessed(badges.toArray(new EarnedBadge[badges.size()]));
    }

    // from interface MemberProvider
    public void dispatchDeferredNotifications (ClientObject caller)
    {
        _notifyMan.dispatchDeferredNotifications((MemberObject)caller);
    }

    // from MsoyProvider
    public void secureBroadcastQuote (
        ClientObject caller, final InvocationService.ResultListener listener)
        throws InvocationException
    {
        final int baseCost = _runtime.getBarCost(CostsConfigObject.BROADCAST_BASE);
        final int increment = _runtime.getBarCost(CostsConfigObject.BROADCAST_INCREMENT);
        final int memberId = ((MemberObject)caller).getMemberId();
        _invoker.postUnit(new PersistingUnit("secureBroadcastQuote", listener) {
            @Override public void invokePersistent ()
                throws InvocationException {
                // make sure they're not a troublemaker
                MemberRecord mrec = _memberRepo.loadMember(memberId);
                if (mrec == null || mrec.isTroublemaker()) {
                    throw new InvocationException("e.broadcast_restricted");
                }
                _quote = secureBroadcastQuote(memberId, baseCost, increment);
            }
            @Override public void handleSuccess () {
                reportRequestProcessed(_quote);
            }
            protected PriceQuote _quote;
        });
    }

    // from MsoyProvider
    public void purchaseAndSendBroadcast (
        ClientObject caller, final int authedCost, final String message,
        InvocationService.ResultListener listener)
        throws InvocationException
    {
        final int baseCost = _runtime.getBarCost(CostsConfigObject.BROADCAST_BASE);
        final int increment = _runtime.getBarCost(CostsConfigObject.BROADCAST_INCREMENT);
        final int memberId = ((MemberObject)caller).getMemberId();
        final Name from = ((MemberObject)caller).getVisibleName();
        _invoker.postUnit(new ServiceUnit("purchaseBroadcast", listener) {
            public void invokePersistent ()
                throws ServiceException, InvocationException {
                // make sure they're not a troublemaker
                MemberRecord mrec = _memberRepo.loadMember(memberId);
                if (mrec == null || mrec.isTroublemaker()) {
                    throw new InvocationException("e.broadcast_restricted");
                }

                // check for a price change
                PriceQuote newQuote = secureBroadcastQuote(memberId, baseCost, increment);
                if (!newQuote.isPurchaseValid(Currency.BARS, authedCost, 0 /* unused exrate */)) {
                    _newQuote = newQuote;
                    return;
                }
                // our buy operation saves the history record but has no ware
                BuyOperation<Void> buyOp = new BuyOperation<Void>() {
                    public boolean create (boolean magicFree, Currency currency, int amountPaid)
                        throws MoneyServiceException {
                        _moneyRepo.noteBroadcastPurchase(memberId, amountPaid, message);
                        return true;
                    }
                    public Void getWare () {
                        return null;
                    }
                };
                // buy it! with exception translation
                try {
                    _moneyLogic.buyFromOOO(_memberRepo.loadMember(memberId), BROADCAST_PURCHASE_KEY,
                        Currency.BARS, authedCost, Currency.BARS, newQuote.getBars(), buyOp,
                        UserAction.Type.BOUGHT_BROADCAST, "m.broadcast_bought",
                        TransactionType.BROADCAST_PURCHASE, null);

                } catch (MoneyException mex) {
                    throw mex.toServiceException();
                }
            }
            public void handleSuccess () {
                // inform the client, null = success, non-null = price changed
                reportRequestProcessed(_newQuote);

                if (_newQuote == null) {
                    // send the message, wheee. log something beforehand in case broadcast throws
                    log.info("Sending broadcast message", "from", from);
                    _chatprov.broadcast(
                        from, MsoyChatCodes.PAID_BROADCAST_MODE, null, message, true);
                    _eventLog.broadcastSent(memberId, authedCost);
                }
            }

            protected PriceQuote _newQuote;
        });
    }

    /**
     * Blocks and obtains a price quote.
     */
    protected PriceQuote secureBroadcastQuote (int memberId, int baseBars, int incrementBars)
    {
        int cost = baseBars +
            (int) Math.ceil(incrementBars * _moneyLogic.getRecentBroadcastFactor());
        return _moneyLogic.securePrice(
            memberId, BROADCAST_PURCHASE_KEY, Currency.BARS, cost, false);
    }

    // dependencies
    @Inject protected @MainInvoker Invoker _invoker;
    @Inject protected ChatProvider _chatprov;
    @Inject protected MailSender _mailer;
    @Inject protected MemberLogic _memberLogic;
    @Inject protected MemberRepository _memberRepo;
    @Inject protected MoneyLogic _moneyLogic;
    @Inject protected MoneyRepository _moneyRepo;
    @Inject protected MsoyEventLogger _eventLog;
    @Inject protected NotificationManager _notifyMan;
    @Inject protected RuntimeConfig _runtime;

    /** An arbitrary key for tracking quotes for broadcast messages. */
    protected static final Object BROADCAST_PURCHASE_KEY = new Object();
}
