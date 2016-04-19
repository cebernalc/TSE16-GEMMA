/**
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.gtasks;

import android.content.Context;

import org.tasks.AccountManager;
import org.tasks.preferences.PermissionChecker;
import org.tasks.preferences.Preferences;

public class GtasksTestPreferenceService extends GtasksPreferenceService {

    public GtasksTestPreferenceService(Context context, Preferences preferences, AccountManager accountManager) {
        super(context, preferences, accountManager);
    }

    @Override
    public boolean isLoggedIn() {
        return false;
    }

    @Override
    public long getLastSyncDate() {
        return 0L;
    }
}
