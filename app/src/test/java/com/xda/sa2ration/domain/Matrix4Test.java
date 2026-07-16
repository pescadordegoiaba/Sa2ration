package com.xda.sa2ration.domain;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class Matrix4Test {
    private static final double EPS = 1e-6;

    @Test public void identityIsNeutral() { assertTrue(Matrix4.identity().isIdentity()); }

    @Test public void multiplicationUsesColumnMajorAndCorrectOrder() {
        Matrix4 gain = Matrix4.diagonal(2,3,4);
        Matrix4 offset = Matrix4.affine(new double[][]{{1,0,0},{0,1,0},{0,0,1}}, new double[]{.1,.2,.3});
        Matrix4 composed = gain.then(offset);
        assertEquals(2, composed.get(0,0), EPS);
        assertEquals(.1, composed.get(0,3), EPS);
        assertEquals(.2, composed.get(1,3), EPS);
        assertEquals(.3, composed.get(2,3), EPS);
    }

    @Test public void surfaceFlingerArgumentsContainSixteenFloats() {
        assertEquals(16, Matrix4.identity().toSurfaceFlingerArguments().split(" f ", -1).length - 1);
    }

    @Test(expected=IllegalArgumentException.class)
    public void rejectsNan() { Matrix4.of(new double[]{1,0,0,0,0,1,0,0,0,0,Double.NaN,0,0,0,0,1}); }

    @Test public void detectsInvalidHomogeneousRow() {
        double[] matrix=Matrix4.identity().toArray(); matrix[3]=1;
        assertFalse(Matrix4.of(matrix).isSurfaceFlingerAffine());
    }
}
