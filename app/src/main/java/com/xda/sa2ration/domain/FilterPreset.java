package com.xda.sa2ration.domain;

import java.util.Locale;

public enum FilterPreset {
    NONE,
    SEPIA,
    WARM,
    COOL,
    AMBER,
    CYAN,
    TERMINAL_GREEN,
    FADED_FILM,
    HIGH_CONTRAST,
    LOW_SATURATION,
    MONOCHROME,
    NEGATIVE,
    RGB_SPLIT_LINEAR,
    PROTANOPIA_CORRECTION,
    DEUTERANOPIA_CORRECTION,
    TRITANOPIA_CORRECTION,
    PROTANOPIA_SIMULATION,
    DEUTERANOPIA_SIMULATION,
    TRITANOPIA_SIMULATION;

    public static FilterPreset parse(String value) {
        if (value == null) return NONE;
        try { return valueOf(value.trim().toUpperCase(Locale.US)); }
        catch (IllegalArgumentException ignored) { return NONE; }
    }

    public Matrix4 matrix() {
        switch (this) {
            case SEPIA: return rgb(new double[][]{{.393,.769,.189},{.349,.686,.168},{.272,.534,.131}});
            case WARM: return Matrix4.diagonal(1.12, 1.02, .88);
            case COOL: return Matrix4.diagonal(.90, 1.02, 1.14);
            case AMBER: return rgb(new double[][]{{1.0,.12,0},{0,.92,0},{0,0,.65}});
            case CYAN: return Matrix4.diagonal(.72, 1.05, 1.08);
            case TERMINAL_GREEN: return rgb(new double[][]{{0,0,0},{.18,.72,.10},{0,0,0}});
            case FADED_FILM: return Matrix4.affine(new double[][]{{.90,.05,.02},{.03,.88,.04},{.04,.08,.82}}, new double[]{.07,.07,.09});
            case HIGH_CONTRAST: return Matrix4.affine(new double[][]{{1.35,0,0},{0,1.35,0},{0,0,1.35}}, new double[]{-.175,-.175,-.175});
            case LOW_SATURATION: return saturation(.45, .45, .45);
            case MONOCHROME: return saturation(0, 0, 0);
            case NEGATIVE: return Matrix4.affine(new double[][]{{-1,0,0},{0,-1,0},{0,0,-1}}, new double[]{1,1,1});
            case RGB_SPLIT_LINEAR: return rgb(new double[][]{{1,.08,-.08},{-.06,1,.06},{.08,-.08,1}});
            case PROTANOPIA_CORRECTION: return rgb(new double[][]{{0,1.05118294,-.05116099},{0,1,0},{0,0,1}});
            case DEUTERANOPIA_CORRECTION: return rgb(new double[][]{{1,0,0},{.9513092,0,.04866992},{0,0,1}});
            case TRITANOPIA_CORRECTION: return rgb(new double[][]{{1,0,0},{0,1,0},{0,-.86744736,1.86727089}});
            case PROTANOPIA_SIMULATION: return rgb(new double[][]{{.56667,.43333,0},{.55833,.44167,0},{0,.24167,.75833}});
            case DEUTERANOPIA_SIMULATION: return rgb(new double[][]{{.625,.375,0},{.70,.30,0},{0,.30,.70}});
            case TRITANOPIA_SIMULATION: return rgb(new double[][]{{.95,.05,0},{0,.43333,.56667},{0,.475,.525}});
            default: return Matrix4.identity();
        }
    }

    private static Matrix4 rgb(double[][] values) {
        return Matrix4.affine(values, new double[]{0,0,0});
    }

    private static Matrix4 saturation(double red, double green, double blue) {
        final double lr=.2126, lg=.7152, lb=.0722;
        return Matrix4.affine(new double[][]{
                {lr*(1-red)+red, lg*(1-red), lb*(1-red)},
                {lr*(1-green), lg*(1-green)+green, lb*(1-green)},
                {lr*(1-blue), lg*(1-blue), lb*(1-blue)+blue}
        }, new double[]{0,0,0});
    }
}
