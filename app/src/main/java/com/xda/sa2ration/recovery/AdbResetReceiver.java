package com.xda.sa2ration.recovery;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

/** Emergency entry point protected by the platform DUMP permission (available to adb shell/root). */
public final class AdbResetReceiver extends BroadcastReceiver {
    public static final String ACTION = "com.xda.sa2ration.action.ADB_RESET_DISPLAY";
    @Override public void onReceive(Context context, Intent intent) {
        if (intent != null && ACTION.equals(intent.getAction())) {
            WorkManager.getInstance(context).enqueueUniqueWork("adb-display-reset", ExistingWorkPolicy.REPLACE,
                    new OneTimeWorkRequest.Builder(NeutralResetWorker.class).build());
        }
    }
}
