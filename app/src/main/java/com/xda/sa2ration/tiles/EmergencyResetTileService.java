package com.xda.sa2ration.tiles;

import android.service.quicksettings.TileService;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import com.xda.sa2ration.recovery.NeutralResetWorker;

public final class EmergencyResetTileService extends TileService {
    @Override public void onClick(){super.onClick();WorkManager.getInstance(this).enqueue(new OneTimeWorkRequest.Builder(NeutralResetWorker.class).build());}
}
