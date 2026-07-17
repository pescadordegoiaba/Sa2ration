package com.xda.sa2ration.recovery;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

/** Non-exported in-app reset entrypoint. */
public final class InternalResetReceiver extends BroadcastReceiver {
    public static final String ACTION_RESET="com.xda.sa2ration.action.RESET_DISPLAY";
    @Override public void onReceive(Context context, Intent intent) {
        if(intent!=null&&ACTION_RESET.equals(intent.getAction())) {
            WorkManager.getInstance(context).enqueueUniqueWork("display-reset",
                    ExistingWorkPolicy.REPLACE,new OneTimeWorkRequest.Builder(NeutralResetWorker.class).build());
        }
    }
}
