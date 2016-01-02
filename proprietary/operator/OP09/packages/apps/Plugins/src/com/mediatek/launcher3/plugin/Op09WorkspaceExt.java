package com.mediatek.launcher3.plugin;

import android.content.Context;

import com.mediatek.common.PluginImpl;
import com.mediatek.launcher3.ext.DefaultOperatorChecker;
import com.mediatek.launcher3.ext.LauncherLog;
import com.mediatek.op09.plugin.R;

/**
 * OP09 IWorkspaceExt implements for Launcher3.
 */
@PluginImpl(interfaceName = "com.mediatek.launcher3.ext.IWorkspaceExt")
public class Op09WorkspaceExt extends DefaultOperatorChecker {
    private static final String TAG = "Op09WorkspaceExt";

    private static final int WORKSPACE_ICON_TEXT_LINENUM = 2;
    private static final int WORKSPACE_ICON_TEXT_SIZE_SP = 12;
    private static final int WORKSPACE_SCREEN_NUMBER = 5;

    /**
     * Constructs a new Op09WorkspaceExt instance.
     * @param context A Context object
     */
    public Op09WorkspaceExt(Context context) {
        super(context);
    }

    @Override
    public boolean supportEditAndHideApps() {
        return true;
    }

    @Override
    public boolean supportAppListCycleSliding() {
        return true;
    }
}
