package com.xda.sa2ration.domain;

import java.util.Arrays;
import java.util.Locale;

/** Immutable 4x4 matrix stored in SurfaceFlinger-compatible column-major order. */
public final class Matrix4 {
    public static final int SIZE = 16;
    private static final double EPSILON = 1e-9;
    private final double[] values;

    private Matrix4(double[] values) {
        if (values == null || values.length != SIZE) {
            throw new IllegalArgumentException("Matrix4 requires exactly 16 values");
        }
        this.values = values.clone();
        for (double value : this.values) {
            if (!Double.isFinite(value)) {
                throw new IllegalArgumentException("Matrix contains NaN or infinity");
            }
        }
    }

    public static Matrix4 identity() {
        return of(new double[]{
                1, 0, 0, 0,
                0, 1, 0, 0,
                0, 0, 1, 0,
                0, 0, 0, 1
        });
    }

    public static Matrix4 of(double[] values) {
        return new Matrix4(values);
    }

    public static Matrix4 diagonal(double red, double green, double blue) {
        return of(new double[]{
                red, 0, 0, 0,
                0, green, 0, 0,
                0, 0, blue, 0,
                0, 0, 0, 1
        });
    }

    public static Matrix4 affine(double[][] coefficients, double[] offsets) {
        if (coefficients == null || coefficients.length != 3 || offsets == null
                || offsets.length != 3) {
            throw new IllegalArgumentException("Expected 3x3 coefficients and three offsets");
        }
        double[] result = new double[SIZE];
        for (int row = 0; row < 3; row++) {
            if (coefficients[row] == null || coefficients[row].length != 3) {
                throw new IllegalArgumentException("Expected a complete 3x3 matrix");
            }
            for (int column = 0; column < 3; column++) {
                result[column * 4 + row] = coefficients[row][column];
            }
            result[12 + row] = offsets[row];
        }
        result[15] = 1;
        return of(result);
    }

    /** Returns {@code left × right}; the right-hand transform is applied first. */
    public static Matrix4 multiply(Matrix4 left, Matrix4 right) {
        double[] result = new double[SIZE];
        for (int column = 0; column < 4; column++) {
            for (int row = 0; row < 4; row++) {
                double sum = 0;
                for (int k = 0; k < 4; k++) {
                    sum += left.values[k * 4 + row] * right.values[column * 4 + k];
                }
                result[column * 4 + row] = sum;
            }
        }
        return of(result);
    }

    public Matrix4 then(Matrix4 next) {
        return multiply(next, this);
    }

    public double get(int row, int column) {
        if (row < 0 || row > 3 || column < 0 || column > 3) {
            throw new IndexOutOfBoundsException("Matrix index out of range");
        }
        return values[column * 4 + row];
    }

    public double[] toArray() {
        return values.clone();
    }

    public float[] toFloatArray() {
        float[] result = new float[SIZE];
        for (int i = 0; i < SIZE; i++) {
            result[i] = (float) values[i];
        }
        return result;
    }

    public boolean approximatelyEquals(Matrix4 other, double tolerance) {
        if (other == null || tolerance < 0) return false;
        for (int i = 0; i < SIZE; i++) {
            if (Math.abs(values[i] - other.values[i]) > tolerance) return false;
        }
        return true;
    }

    public boolean isIdentity() {
        return approximatelyEquals(identity(), EPSILON);
    }

    public boolean isSurfaceFlingerAffine() {
        return Math.abs(values[3]) <= EPSILON
                && Math.abs(values[7]) <= EPSILON
                && Math.abs(values[11]) <= EPSILON
                && Math.abs(values[15] - 1.0) <= EPSILON;
    }

    public static Matrix4 interpolate(Matrix4 from, Matrix4 to, double amount) {
        if (!Double.isFinite(amount)) throw new IllegalArgumentException("Non-finite intensity");
        double[] result = new double[SIZE];
        for (int i = 0; i < SIZE; i++) {
            result[i] = from.values[i] + (to.values[i] - from.values[i]) * amount;
        }
        return of(result);
    }

    public String toSurfaceFlingerArguments() {
        StringBuilder builder = new StringBuilder();
        for (double value : values) {
            builder.append(" f ").append(String.format(Locale.US, "%.8f", value));
        }
        return builder.toString();
    }

    @Override
    public boolean equals(Object object) {
        return object instanceof Matrix4 && Arrays.equals(values, ((Matrix4) object).values);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(values);
    }
}
