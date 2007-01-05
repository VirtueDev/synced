package com.threerings.msoy.swiftly.client;

import java.awt.Container;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.net.URL;

import javax.swing.JApplet;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class SwiftlyApplet extends JApplet {

    public void init() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // this should just fall back on a working theme
        }

        // Save the root content pane
        _contentPane = getContentPane();

        // Execute a job on the event-dispatching thread: creating this applet's GUI.
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    createGUI();
                }
            });
        } catch (Exception e) {
            System.err.println("createGUI didn't successfully complete");
        }

        // XXX temp. add a few example tabs
        _editor.addEditorTab("file #1", "http://localhost:8080/swiftly/index.html");
        _editor.addEditorTab("file #2", "http://localhost:8080/catalog/index.html");

        setStatus("Welcome to Swiftly!");
    }

    public void setStatus (String msg) {
        _statusbar.setLabel(msg);
    }

    protected void createGUI () {
        // setup the components
        _editor = new SwiftlyEditor(this);
        _editor.setMinimumSize(new Dimension(400, 0));
        _projectPanel = new SwiftlyProjectPanel(this);
        _projectPanel.setMinimumSize(new Dimension(0, 0));
        _toolbar = new SwiftlyToolbar(this);
        _statusbar = new SwiftlyStatusBar(this);
        _splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, _editor, _projectPanel);
        // TODO apparently GTK does not have the graphic for this. What to do?
        _splitPane.setOneTouchExpandable(true);
        _splitPane.setDividerLocation(650);

        // layout the window
        _contentPane.add(_toolbar, BorderLayout.PAGE_START);
        _contentPane.add(_splitPane, BorderLayout.CENTER);
        _contentPane.add(_statusbar, BorderLayout.SOUTH);
    }

    protected Container _contentPane;
    protected SwiftlyEditor _editor;
    protected SwiftlyToolbar _toolbar;
    protected SwiftlyStatusBar _statusbar;
    protected SwiftlyProjectPanel _projectPanel;
    protected JSplitPane _splitPane;
} 
