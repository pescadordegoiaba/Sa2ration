package com.xda.sa2ration.domain;

import java.util.Arrays;

/** Factory and implementations for the documented linear pipeline. */
public final class DefaultTransformStages {
    private DefaultTransformStages() {}

    public static ColorTransformPipeline createPipeline() {
        return new ColorTransformPipeline(Arrays.asList(
                temperature(), tint(), rgbGain(), rgbOffset(), channelMixer(), hue(),
                saturation(), contrast(), digitalBrightness(), blackLevel(), whiteLevel(),
                grayscale(), inversion(), filter(), customMatrix()
        ));
    }

    public static ColorTransformStage temperature() { return new TemperatureStage(); }
    public static ColorTransformStage tint() { return new TintStage(); }
    public static ColorTransformStage rgbGain() { return new RgbGainStage(); }
    public static ColorTransformStage rgbOffset() { return new RgbOffsetStage(); }
    public static ColorTransformStage channelMixer() { return new ChannelMixerStage(); }
    public static ColorTransformStage hue() { return new HueStage(); }
    public static ColorTransformStage saturation() { return new SaturationStage(); }
    public static ColorTransformStage contrast() { return new ContrastStage(); }
    public static ColorTransformStage digitalBrightness() { return new DigitalBrightnessStage(); }
    public static ColorTransformStage blackLevel() { return new BlackLevelStage(); }
    public static ColorTransformStage whiteLevel() { return new WhiteLevelStage(); }
    public static ColorTransformStage grayscale() { return new GrayscaleStage(); }
    public static ColorTransformStage inversion() { return new InversionStage(); }
    public static ColorTransformStage filter() { return new FilterStage(); }
    public static ColorTransformStage customMatrix() { return new CustomMatrixStage(); }

    private abstract static class BaseStage implements ColorTransformStage {
        private final String id;
        BaseStage(String id) { this.id = id; }
        @Override public String id() { return id; }
    }

    private static final class TemperatureStage extends BaseStage {
        TemperatureStage() { super("temperature"); }
        @Override public boolean isEnabled(DisplayConfiguration c) { return c.temperatureEnabled; }
        @Override public Matrix4 createMatrix(DisplayConfiguration c) {
            if (c.temperatureManualRgb) return Matrix4.diagonal(c.temperatureRed, c.temperatureGreen, c.temperatureBlue);
            double[] current = kelvinToRgb(c.temperatureKelvin);
            double[] neutral = kelvinToRgb(c.neutralTemperatureKelvin);
            return Matrix4.diagonal(current[0]/neutral[0], current[1]/neutral[1], current[2]/neutral[2]);
        }
    }

    private static final class TintStage extends BaseStage {
        TintStage() { super("tint"); }
        @Override public boolean isEnabled(DisplayConfiguration c) { return c.tintEnabled; }
        @Override public Matrix4 createMatrix(DisplayConfiguration c) {
            double t = c.tint;
            return Matrix4.diagonal(1.0 - t * .5, 1.0 + t, 1.0 - t * .5);
        }
    }

    private static final class RgbGainStage extends BaseStage {
        RgbGainStage() { super("rgb_gain"); }
        @Override public boolean isEnabled(DisplayConfiguration c) { return c.rgbGainEnabled; }
        @Override public Matrix4 createMatrix(DisplayConfiguration c) { return Matrix4.diagonal(c.redGain,c.greenGain,c.blueGain); }
    }

    private static final class RgbOffsetStage extends BaseStage {
        RgbOffsetStage() { super("rgb_offset"); }
        @Override public boolean isEnabled(DisplayConfiguration c) { return c.rgbOffsetEnabled; }
        @Override public Matrix4 createMatrix(DisplayConfiguration c) { return affineIdentity(c.redOffset,c.greenOffset,c.blueOffset); }
    }

    private static final class ChannelMixerStage extends BaseStage {
        ChannelMixerStage() { super("channel_mixer"); }
        @Override public boolean isEnabled(DisplayConfiguration c) { return c.channelMixerEnabled; }
        @Override public Matrix4 createMatrix(DisplayConfiguration c) {
            double[] m = c.channelMixer;
            return Matrix4.affine(new double[][]{{m[0],m[1],m[2]},{m[3],m[4],m[5]},{m[6],m[7],m[8]}}, new double[]{0,0,0});
        }
    }

    private static final class HueStage extends BaseStage {
        HueStage() { super("hue"); }
        @Override public boolean isEnabled(DisplayConfiguration c) { return c.hueEnabled; }
        @Override public Matrix4 createMatrix(DisplayConfiguration c) {
            double radians = Math.toRadians(c.hueDegrees);
            double cos = Math.cos(radians), sin = Math.sin(radians);
            return Matrix4.affine(new double[][]{
                    {.213+.787*cos-.213*sin, .715-.715*cos-.715*sin, .072-.072*cos+.928*sin},
                    {.213-.213*cos+.143*sin, .715+.285*cos+.140*sin, .072-.072*cos-.283*sin},
                    {.213-.213*cos-.787*sin, .715-.715*cos+.715*sin, .072+.928*cos+.072*sin}
            }, new double[]{0,0,0});
        }
    }

    /** Global saturation is handled by transaction 1022; this stage is per-channel. */
    private static final class SaturationStage extends BaseStage {
        SaturationStage() { super("rgb_saturation"); }
        @Override public boolean isEnabled(DisplayConfiguration c) { return c.rgbSaturationEnabled; }
        @Override public Matrix4 createMatrix(DisplayConfiguration c) {
            final double lr=.2126, lg=.7152, lb=.0722;
            double r=c.redSaturation, g=c.greenSaturation, b=c.blueSaturation;
            return Matrix4.affine(new double[][]{
                    {lr*(1-r)+r, lg*(1-r), lb*(1-r)},
                    {lr*(1-g), lg*(1-g)+g, lb*(1-g)},
                    {lr*(1-b), lg*(1-b), lb*(1-b)+b}
            }, new double[]{0,0,0});
        }
    }

    private static final class ContrastStage extends BaseStage {
        ContrastStage() { super("contrast"); }
        @Override public boolean isEnabled(DisplayConfiguration c) { return c.globalContrastEnabled || c.rgbContrastEnabled; }
        @Override public Matrix4 createMatrix(DisplayConfiguration c) {
            double global = c.globalContrastEnabled ? c.globalContrast : 1;
            double r=global*(c.rgbContrastEnabled?c.redContrast:1);
            double g=global*(c.rgbContrastEnabled?c.greenContrast:1);
            double b=global*(c.rgbContrastEnabled?c.blueContrast:1);
            return Matrix4.affine(new double[][]{{r,0,0},{0,g,0},{0,0,b}}, new double[]{.5*(1-r),.5*(1-g),.5*(1-b)});
        }
    }

    private static final class DigitalBrightnessStage extends BaseStage {
        DigitalBrightnessStage() { super("digital_brightness"); }
        @Override public boolean isEnabled(DisplayConfiguration c) { return c.digitalBrightnessEnabled; }
        @Override public Matrix4 createMatrix(DisplayConfiguration c) {
            return Matrix4.affine(new double[][]{
                    {c.digitalBrightnessGain*c.redBrightnessGain,0,0},
                    {0,c.digitalBrightnessGain*c.greenBrightnessGain,0},
                    {0,0,c.digitalBrightnessGain*c.blueBrightnessGain}},
                    new double[]{c.digitalBrightnessOffset,c.digitalBrightnessOffset,c.digitalBrightnessOffset});
        }
    }

    private static final class BlackLevelStage extends BaseStage {
        BlackLevelStage() { super("black_level"); }
        @Override public boolean isEnabled(DisplayConfiguration c) { return c.blackLevelEnabled; }
        @Override public Matrix4 createMatrix(DisplayConfiguration c) {
            return affineIdentity(c.blackLevel+c.redBlackLevel,c.blackLevel+c.greenBlackLevel,c.blackLevel+c.blueBlackLevel);
        }
    }

    private static final class WhiteLevelStage extends BaseStage {
        WhiteLevelStage() { super("white_level"); }
        @Override public boolean isEnabled(DisplayConfiguration c) { return c.whiteLevelEnabled; }
        @Override public Matrix4 createMatrix(DisplayConfiguration c) {
            return Matrix4.diagonal(c.whiteLevel*c.redWhiteLevel,c.whiteLevel*c.greenWhiteLevel,c.whiteLevel*c.blueWhiteLevel);
        }
    }

    private static final class GrayscaleStage extends BaseStage {
        GrayscaleStage() { super("grayscale"); }
        @Override public boolean isEnabled(DisplayConfiguration c) { return c.grayscaleEnabled; }
        @Override public Matrix4 createMatrix(DisplayConfiguration c) {
            double[] luma = luma(c);
            Matrix4 gray = Matrix4.affine(new double[][]{luma,luma,luma}, new double[]{0,0,0});
            return Matrix4.interpolate(Matrix4.identity(), gray, c.grayscaleIntensity);
        }
    }

    private static final class InversionStage extends BaseStage {
        InversionStage() { super("inversion"); }
        @Override public boolean isEnabled(DisplayConfiguration c) { return c.inversionEnabled; }
        @Override public Matrix4 createMatrix(DisplayConfiguration c) {
            double t=c.inversionIntensity;
            double r=c.invertRed?1-2*t:1, g=c.invertGreen?1-2*t:1, b=c.invertBlue?1-2*t:1;
            return Matrix4.affine(new double[][]{{r,0,0},{0,g,0},{0,0,b}}, new double[]{c.invertRed?t:0,c.invertGreen?t:0,c.invertBlue?t:0});
        }
    }

    private static final class FilterStage extends BaseStage {
        FilterStage() { super("filter"); }
        @Override public boolean isEnabled(DisplayConfiguration c) { return c.filterEnabled && FilterPreset.parse(c.filterPreset)!=FilterPreset.NONE; }
        @Override public Matrix4 createMatrix(DisplayConfiguration c) {
            return Matrix4.interpolate(Matrix4.identity(), FilterPreset.parse(c.filterPreset).matrix(), c.filterIntensity);
        }
    }

    private static final class CustomMatrixStage extends BaseStage {
        CustomMatrixStage() { super("custom_matrix"); }
        @Override public boolean isEnabled(DisplayConfiguration c) { return c.customMatrixEnabled; }
        @Override public Matrix4 createMatrix(DisplayConfiguration c) {
            Matrix4 matrix=Matrix4.of(c.customMatrix);
            if (!matrix.isSurfaceFlingerAffine()) throw new IllegalArgumentException("Custom matrix last row must be 0,0,0,1");
            return matrix;
        }
    }

    private static Matrix4 affineIdentity(double r, double g, double b) {
        return Matrix4.affine(new double[][]{{1,0,0},{0,1,0},{0,0,1}}, new double[]{r,g,b});
    }

    private static double[] luma(DisplayConfiguration c) {
        switch (c.grayscaleCoefficients) {
            case "REC601": return new double[]{.299,.587,.114};
            case "REC2020": return new double[]{.2627,.6780,.0593};
            case "AVERAGE": return new double[]{1.0/3,1.0/3,1.0/3};
            case "CUSTOM": return new double[]{c.customLumaRed,c.customLumaGreen,c.customLumaBlue};
            default: return new double[]{.2126,.7152,.0722};
        }
    }

    /** Standard sRGB approximation; values are normalized to 0..1. */
    private static double[] kelvinToRgb(double kelvin) {
        double t=Math.max(1000,Math.min(40000,kelvin))/100.0;
        double r,g,b;
        if(t<=66){r=255;g=99.4708025861*Math.log(t)-161.1195681661;}
        else{r=329.698727446*Math.pow(t-60,-.1332047592);g=288.1221695283*Math.pow(t-60,-.0755148492);}
        if(t>=66)b=255; else if(t<=19)b=0; else b=138.5177312231*Math.log(t-10)-305.0447927307;
        return new double[]{clamp255(r)/255.0,clamp255(g)/255.0,clamp255(b)/255.0};
    }

    private static double clamp255(double value){return Math.max(0.0001,Math.min(255,value));}
}
