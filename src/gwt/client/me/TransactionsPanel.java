//
// $Id$

package client.me;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.ChangeListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.money.data.all.ReportType;

import client.shell.Args;
import client.shell.CShell;
import client.shell.Pages;
import client.ui.MsoyUI;
import client.ui.RoundBox;
import client.ui.StretchButton;
import client.util.NaviUtil;
import client.util.Link;

public class TransactionsPanel extends FlowPanel
{
    public TransactionsPanel (int reportIndex, final int memberId)
    {
        setStyleName("transactions");

        FlexTable blurb = new FlexTable();
        blurb.setStyleName("Blurb");

        RoundBox tip = new RoundBox(RoundBox.MEDIUM_BLUE);
        tip.add(new HTML(REPORT_TIPS[reportIndex-1]));
        blurb.setWidget(0, 0, tip);

        FlowPanel billing = new FlowPanel();
        StretchButton buyBars = new StretchButton(StretchButton.ORANGE_THICK,
            MsoyUI.createLabel(_msgs.buyBars(), null));
        buyBars.addClickListener(NaviUtil.onBuyBars());
        billing.add(MsoyUI.createHTML(_msgs.billingTip(), "BillingTip"));
        billing.add(buyBars);
        blurb.setWidget(0, 1, billing);
        blurb.getCellFormatter().setStyleName(0, 1, "Billing");

        add(blurb);

        ReportType report = ReportType.fromIndex(reportIndex);
        // The data model is used in both the balance panel and the bling panel.
        MoneyTransactionDataModel model = new MoneyTransactionDataModel(memberId, report);

        final ListBox reportBox = new ListBox();
        for (String name : REPORT_NAMES) {
            reportBox.addItem(name);
        }
        reportBox.addChangeListener(new ChangeListener() {
            public void onChange (Widget widget) {
                Link.go(Pages.ME, Args.compose(MePage.TRANSACTIONS,
                        String.valueOf(reportBox.getSelectedIndex()+1),
                        String.valueOf(memberId)));
            }
        });
        reportBox.setSelectedIndex(reportIndex-1);

        add((report == ReportType.CREATOR) ?
            new IncomePanel(model, reportBox) : new BalancePanel(model, reportBox));

        // Extra bits
        if (report == ReportType.BLING) {
            add(new BlingPanel(model));
        } else if (CShell.isSupport() && report == ReportType.COINS) {
            add(new DeductPanel(memberId));
        }
    }

    protected static final MeMessages _msgs = GWT.create(MeMessages.class);

    protected static final String[] REPORT_NAMES = {
        _msgs.reportCoins(), _msgs.reportBars(), _msgs.reportBling(), _msgs.reportCreator()
    };

    protected static final String[] REPORT_TIPS = {
        _msgs.tipCoins(), _msgs.tipBars(Link.billingURL()), _msgs.tipBling(), _msgs.tipCreator()
    };
}
