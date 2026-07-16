package com.xda.sa2ration;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.xda.sa2ration.recovery.RestoreDisplayWorker;

/** Lightweight receiver: all persistence, root and display work runs in WorkManager. */
public final class NewBootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        if(intent==null||intent.getAction()==null)return;
        String action=intent.getAction();
        if(Intent.ACTION_BOOT_COMPLETED.equals(action)||Intent.ACTION_USER_UNLOCKED.equals(action)
                ||Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) {
            OneTimeWorkRequest request=new OneTimeWorkRequest.Builder(RestoreDisplayWorker.class)
                    .addTag("display-restore").build();
            WorkManager.getInstance(context).enqueueUniqueWork("display-restore",
                    ExistingWorkPolicy.REPLACE,request);
        }
    }
}
