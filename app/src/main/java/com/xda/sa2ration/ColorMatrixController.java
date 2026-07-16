package com.xda.sa2ration;

import java.util.Locale;

/**
 * Builds the client color matrix consumed by SurfaceFlinger transaction 1015.
 */
public final class ColorMatrixController {

    public static final float MIN_MULTIPLIER = 0f;
    public static final float MAX_MULTIPLIER = 10f;
    public static final float DEFAULT_MULTIPLIER = 1f;

    // Rec. 709 / sRGB luminance coefficients.
    private static final float LUMA_RED = 0.2126f;
    private static final float LUMA_GREEN = 0.7152f;
    private static final float LUMA_BLUE = 0.0722f;

    private ColorMatrixController() {
    }

    /**
     * Creates a column-major 4x4 matrix. Saturation is calculated independently
     * for every output channel and contrast is centered around 0.5.
     */
    public static float[] createMatrix(float globalContrast,
                                       float redContrast,
                                       float greenContrast,
                                       float blueContrast,
                                       float redSaturation,
                                       float greenSaturation,
                                       float blueSaturation) {
        float cr = clamp(globalContrast) * clamp(redContrast);
        float cg = clamp(globalContrast) * clamp(greenContrast);
        float cb = clamp(globalContrast) * clamp(blueContrast);
        float sr = clamp(redSaturation);
        float sg = clamp(greenSaturation);
        float sb = clamp(blueSaturation);

        float[] matrix = new float[16];

        // Column 0 (red input).
        matrix[0] = cr * (LUMA_RED * (1f - sr) + sr);
        matrix[1] = cg * (LUMA_RED * (1f - sg));
        matrix[2] = cb * (LUMA_RED * (1f - sb));
        matrix[3] = 0f;

        // Column 1 (green input).
        matrix[4] = cr * (LUMA_GREEN * (1f - sr));
        matrix[5] = cg * (LUMA_GREEN * (1f - sg) + sg);
        matrix[6] = cb * (LUMA_GREEN * (1f - sb));
        matrix[7] = 0f;

        // Column 2 (blue input).
        matrix[8] = cr * (LUMA_BLUE * (1f - sr));
        matrix[9] = cg * (LUMA_BLUE * (1f - sg));
        matrix[10] = cb * (LUMA_BLUE * (1f - sb) + sb);
        matrix[11] = 0f;

        // Column 3 (translation and homogeneous coordinate).
        matrix[12] = 0.5f * (1f - cr);
        matrix[13] = 0.5f * (1f - cg);
        matrix[14] = 0.5f * (1f - cb);
        matrix[15] = 1f;

        return matrix;
    }

    /**
     * Creates a command for SurfaceFlinger's client color matrix transaction.
     */
    public static String createSurfaceFlingerCommand(float[] matrix) {
        if (matrix == null || matrix.length != 16) {
            throw new IllegalArgumentException("A 4x4 color matrix must contain 16 values");
        }
        StringBuilder command = new StringBuilder("service call SurfaceFlinger 1015 i32 1");
        for (float value : matrix) {
            command.append(" f ").append(String.format(Locale.US, "%.6f", value));
        }
        return command.toString();
    }

    public static float clamp(float value) {
        if (Float.isNaN(value) || Float.isInfinite(value)) {
            return DEFAULT_MULTIPLIER;
        }
        return Math.max(MIN_MULTIPLIER, Math.min(MAX_MULTIPLIER, value));
    }
}
