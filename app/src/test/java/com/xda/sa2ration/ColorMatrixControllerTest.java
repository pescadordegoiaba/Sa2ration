package com.xda.sa2ration;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ColorMatrixControllerTest {

    private static final float EPSILON = 0.0001f;

    @Test
    public void defaultsProduceIdentityMatrix() {
        float[] matrix = ColorMatrixController.createMatrix(1f, 1f, 1f, 1f,
                1f, 1f, 1f);

        for (int i = 0; i < matrix.length; i++) {
            float expected = i == 0 || i == 5 || i == 10 || i == 15 ? 1f : 0f;
            assertEquals(expected, matrix[i], EPSILON);
        }
    }

    @Test
    public void zeroSaturationProducesLuminanceInEveryChannel() {
        float[] matrix = ColorMatrixController.createMatrix(1f, 1f, 1f, 1f,
                0f, 0f, 0f);

        assertEquals(0.2126f, matrix[0], EPSILON);
        assertEquals(0.2126f, matrix[1], EPSILON);
        assertEquals(0.2126f, matrix[2], EPSILON);
        assertEquals(0.7152f, matrix[4], EPSILON);
        assertEquals(0.7152f, matrix[5], EPSILON);
        assertEquals(0.7152f, matrix[6], EPSILON);
        assertEquals(0.0722f, matrix[8], EPSILON);
        assertEquals(0.0722f, matrix[9], EPSILON);
        assertEquals(0.0722f, matrix[10], EPSILON);
    }

    @Test
    public void contrastIsCenteredAroundMidpointAndCanReachTen() {
        float[] matrix = ColorMatrixController.createMatrix(10f, 1f, 1f, 1f,
                1f, 1f, 1f);

        assertEquals(10f, matrix[0], EPSILON);
        assertEquals(10f, matrix[5], EPSILON);
        assertEquals(10f, matrix[10], EPSILON);
        assertEquals(-4.5f, matrix[12], EPSILON);
        assertEquals(-4.5f, matrix[13], EPSILON);
        assertEquals(-4.5f, matrix[14], EPSILON);
    }

    @Test
    public void commandContainsAllSixteenFloats() {
        float[] identity = ColorMatrixController.createMatrix(1f, 1f, 1f, 1f,
                1f, 1f, 1f);
        String command = ColorMatrixController.createSurfaceFlingerCommand(identity);

        assertTrue(command.startsWith("service call SurfaceFlinger 1015 i32 1"));
        assertEquals(16, command.split(" f ", -1).length - 1);
    }

    @Test
    public void perChannelContrastOnlyChangesSelectedOutput() {
        float[] matrix = ColorMatrixController.createMatrix(1f, 2f, 1f, 1f,
                1f, 1f, 1f);

        assertEquals(2f, matrix[0], EPSILON);
        assertEquals(1f, matrix[5], EPSILON);
        assertEquals(1f, matrix[10], EPSILON);
        assertEquals(-0.5f, matrix[12], EPSILON);
        assertEquals(0f, matrix[13], EPSILON);
        assertEquals(0f, matrix[14], EPSILON);
    }

    @Test
    public void clampRejectsOutOfRangeAndNonFiniteValues() {
        assertEquals(0f, ColorMatrixController.clamp(-1f), EPSILON);
        assertEquals(10f, ColorMatrixController.clamp(11f), EPSILON);
        assertEquals(1f, ColorMatrixController.clamp(Float.NaN), EPSILON);
    }
}
