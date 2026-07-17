package com.xda.sa2ration.recovery;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.xda.sa2ration.backend.SurfaceFlingerBackend;
import com.xda.sa2ration.domain.DefaultTransformStages;
import com.xda.sa2ration.domain.DisplayConfiguration;
import com.xda.sa2ration.domain.StoredApplicationState;
import com.xda.sa2ration.persistence.ConfigurationRepository;
import com.xda.sa2ration.shell.RootShellExecutor;
import com.xda.sa2ration.shell.ShellCommand;
import com.xda.sa2ration.shell.ShellResult;

public final class RestoreDisplayWorker extends Worker {
    public RestoreDisplayWorker(@NonNull Context context, @NonNull WorkerParameters params) { super(context, params); }

    @NonNull @Override public Result doWork() {
        ConfigurationRepository repository=ConfigurationRepository.getInstance(getApplicationContext());
        StoredApplicationState state=repository.load();
        try(RootShellExecutor executor=new RootShellExecutor()) {
            SurfaceFlingerBackend backend=new SurfaceFlingerBackend(executor);
            ShellResult safeMode=executor.executeBlocking(ShellCommand.builder(
                    "if [ -e /data/adb/sa2ration/disable ] || [ \"$(getprop persist.sa2ration.safe_mode)\" = \"1\" ]; then echo 1; else echo 0; fi")
                    .timeoutMs(5_000).build());
            if("1".equals(safeMode.stdout.trim()) || state.consecutiveBootFailures>=3) {
                backend.resetToNeutral();
                return Result.failure();
            }
            if(!state.stable.restoreAtBoot) return Result.success();
            state.consecutiveBootFailures++;
            repository.save(state);
            DisplayConfiguration stable=state.stable.copy();
            ShellResult result=backend.apply(stable,DefaultTransformStages.createPipeline().compose(stable));
            if(result.isSuccess()) {
                state.consecutiveBootFailures=0;
                repository.save(state);
                return Result.success();
            }
            return Result.retry();
        }
    }
}
