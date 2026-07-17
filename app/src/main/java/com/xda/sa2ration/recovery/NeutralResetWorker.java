package com.xda.sa2ration.recovery;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.xda.sa2ration.backend.SurfaceFlingerBackend;
import com.xda.sa2ration.domain.DisplayConfiguration;
import com.xda.sa2ration.persistence.ConfigurationRepository;
import com.xda.sa2ration.shell.RootShellExecutor;
import com.xda.sa2ration.shell.ShellResult;

public final class NeutralResetWorker extends Worker {
    public NeutralResetWorker(@NonNull Context context,@NonNull WorkerParameters params){super(context,params);}
    @NonNull @Override public Result doWork(){
        try(RootShellExecutor executor=new RootShellExecutor()){
            ShellResult result=new SurfaceFlingerBackend(executor).resetToNeutral();
            if(result.isSuccess())ConfigurationRepository.getInstance(getApplicationContext()).confirmStable(DisplayConfiguration.neutral());
            return result.isSuccess()?Result.success():Result.failure();
        }
    }
}
