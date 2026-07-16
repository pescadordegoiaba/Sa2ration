package com.xda.sa2ration;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.util.Optional;

public class NewBootReceiver extends BroadcastReceiver {

    /**
     * Receives new boot event. Applies persisted saturation and cm values, if present.
     * @param context context passed
     * @param intent intent passed
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            // Restore global saturation and the complete per-channel color matrix.
            Optional<String> saturation = PersistenceController.getInstance(context).restoreFromProperties(MainActivity.keys.SATURATION.name());
            Optional<String> cm = PersistenceController.getInstance(context).restoreFromProperties( MainActivity.keys.CM.name());
            float contrast = restoreMultiplier(context, MainActivity.keys.CONTRAST);
            float redSaturation = restoreMultiplier(context, MainActivity.keys.RED_SATURATION);
            float greenSaturation = restoreMultiplier(context, MainActivity.keys.GREEN_SATURATION);
            float blueSaturation = restoreMultiplier(context, MainActivity.keys.BLUE_SATURATION);
            float redContrast = restoreMultiplier(context, MainActivity.keys.RED_CONTRAST);
            float greenContrast = restoreMultiplier(context, MainActivity.keys.GREEN_CONTRAST);
            float blueContrast = restoreMultiplier(context, MainActivity.keys.BLUE_CONTRAST);
            float[] matrix = ColorMatrixController.createMatrix(
                    contrast, redContrast, greenContrast, blueContrast,
                    redSaturation, greenSaturation, blueSaturation);
            String matrixCommand = ColorMatrixController.createSurfaceFlingerCommand(matrix);
            saturation.ifPresent(s -> CommandController.execCommand("setprop " + MainActivity.PERSISTENT_COLOR_SATURATION
                    + " " + s, "service call SurfaceFlinger 1022 f " + s, matrixCommand));
            if (!saturation.isPresent()) {
                CommandController.execCommand(matrixCommand);
            }
            cm.ifPresent(c ->  CommandController.execCommand("service call SurfaceFlinger 1023 i32 " + c ));
        }
    }

    private float restoreMultiplier(Context context, MainActivity.keys key) {
        Optional<String> saved = PersistenceController.getInstance(context)
                .restoreFromProperties(key.name());
        if (!saved.isPresent()) {
            return ColorMatrixController.DEFAULT_MULTIPLIER;
        }
        try {
            return ColorMatrixController.clamp(Float.parseFloat(saved.get()));
        } catch (NumberFormatException ignored) {
            return ColorMatrixController.DEFAULT_MULTIPLIER;
        }
    }

}
