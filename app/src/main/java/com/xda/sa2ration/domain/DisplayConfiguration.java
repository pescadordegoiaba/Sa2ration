package com.xda.sa2ration.domain;

import java.util.Arrays;

/** Versioned domain state. Disabled stages retain their values but contribute identity. */
public final class DisplayConfiguration {
    public static final int CURRENT_SCHEMA = 2;

    public int schemaVersion = CURRENT_SCHEMA;
    public boolean masterEnabled = true;
    public boolean colorManagementEnabled = true;
    public String activeProfileId = "neutral";

    public boolean globalSaturationEnabled = true;
    public double globalSaturation = 1.0;
    public boolean rgbSaturationEnabled = true;
    public double redSaturation = 1.0;
    public double greenSaturation = 1.0;
    public double blueSaturation = 1.0;

    public boolean globalContrastEnabled = true;
    public double globalContrast = 1.0;
    public boolean rgbContrastEnabled = true;
    public double redContrast = 1.0;
    public double greenContrast = 1.0;
    public double blueContrast = 1.0;

    public boolean digitalBrightnessEnabled;
    public double digitalBrightnessGain = 1.0;
    public double digitalBrightnessOffset;
    public double redBrightnessGain = 1.0;
    public double greenBrightnessGain = 1.0;
    public double blueBrightnessGain = 1.0;

    public boolean blackLevelEnabled;
    public double blackLevel;
    public double redBlackLevel;
    public double greenBlackLevel;
    public double blueBlackLevel;

    public boolean whiteLevelEnabled;
    public double whiteLevel = 1.0;
    public double redWhiteLevel = 1.0;
    public double greenWhiteLevel = 1.0;
    public double blueWhiteLevel = 1.0;

    public boolean rgbGainEnabled;
    public double redGain = 1.0;
    public double greenGain = 1.0;
    public double blueGain = 1.0;

    public boolean rgbOffsetEnabled;
    public double redOffset;
    public double greenOffset;
    public double blueOffset;

    public boolean temperatureEnabled;
    public double temperatureKelvin = 6500.0;
    public double neutralTemperatureKelvin = 6500.0;
    public boolean temperatureManualRgb;
    public double temperatureRed = 1.0;
    public double temperatureGreen = 1.0;
    public double temperatureBlue = 1.0;

    public boolean tintEnabled;
    public double tint;
    public boolean hueEnabled;
    public double hueDegrees;

    public boolean channelMixerEnabled;
    public double[] channelMixer = identityArray();

    public boolean grayscaleEnabled;
    public double grayscaleIntensity = 1.0;
    public String grayscaleCoefficients = "REC709";
    public double customLumaRed = 0.2126;
    public double customLumaGreen = 0.7152;
    public double customLumaBlue = 0.0722;

    public boolean inversionEnabled;
    public double inversionIntensity = 1.0;
    public boolean invertRed = true;
    public boolean invertGreen = true;
    public boolean invertBlue = true;

    public boolean filterEnabled;
    public String filterPreset = "NONE";
    public double filterIntensity = 1.0;

    public boolean customMatrixEnabled;
    public double[] customMatrix = identityArray();

    public boolean restoreAtBoot = true;
    public boolean experimentalFeaturesEnabled;

    public static DisplayConfiguration neutral() {
        return new DisplayConfiguration();
    }

    public DisplayConfiguration copy() {
        DisplayConfiguration copy = new DisplayConfiguration();
        copy.schemaVersion = schemaVersion;
        copy.masterEnabled = masterEnabled;
        copy.colorManagementEnabled = colorManagementEnabled;
        copy.activeProfileId = activeProfileId;
        copy.globalSaturationEnabled = globalSaturationEnabled;
        copy.globalSaturation = globalSaturation;
        copy.rgbSaturationEnabled = rgbSaturationEnabled;
        copy.redSaturation = redSaturation; copy.greenSaturation = greenSaturation; copy.blueSaturation = blueSaturation;
        copy.globalContrastEnabled = globalContrastEnabled; copy.globalContrast = globalContrast;
        copy.rgbContrastEnabled = rgbContrastEnabled;
        copy.redContrast = redContrast; copy.greenContrast = greenContrast; copy.blueContrast = blueContrast;
        copy.digitalBrightnessEnabled = digitalBrightnessEnabled;
        copy.digitalBrightnessGain = digitalBrightnessGain; copy.digitalBrightnessOffset = digitalBrightnessOffset;
        copy.redBrightnessGain = redBrightnessGain; copy.greenBrightnessGain = greenBrightnessGain; copy.blueBrightnessGain = blueBrightnessGain;
        copy.blackLevelEnabled = blackLevelEnabled; copy.blackLevel = blackLevel;
        copy.redBlackLevel = redBlackLevel; copy.greenBlackLevel = greenBlackLevel; copy.blueBlackLevel = blueBlackLevel;
        copy.whiteLevelEnabled = whiteLevelEnabled; copy.whiteLevel = whiteLevel;
        copy.redWhiteLevel = redWhiteLevel; copy.greenWhiteLevel = greenWhiteLevel; copy.blueWhiteLevel = blueWhiteLevel;
        copy.rgbGainEnabled = rgbGainEnabled; copy.redGain = redGain; copy.greenGain = greenGain; copy.blueGain = blueGain;
        copy.rgbOffsetEnabled = rgbOffsetEnabled; copy.redOffset = redOffset; copy.greenOffset = greenOffset; copy.blueOffset = blueOffset;
        copy.temperatureEnabled = temperatureEnabled; copy.temperatureKelvin = temperatureKelvin;
        copy.neutralTemperatureKelvin = neutralTemperatureKelvin; copy.temperatureManualRgb = temperatureManualRgb;
        copy.temperatureRed = temperatureRed; copy.temperatureGreen = temperatureGreen; copy.temperatureBlue = temperatureBlue;
        copy.tintEnabled = tintEnabled; copy.tint = tint; copy.hueEnabled = hueEnabled; copy.hueDegrees = hueDegrees;
        copy.channelMixerEnabled = channelMixerEnabled; copy.channelMixer = safeArray(channelMixer, 9, identityArray());
        copy.grayscaleEnabled = grayscaleEnabled; copy.grayscaleIntensity = grayscaleIntensity;
        copy.grayscaleCoefficients = grayscaleCoefficients; copy.customLumaRed = customLumaRed;
        copy.customLumaGreen = customLumaGreen; copy.customLumaBlue = customLumaBlue;
        copy.inversionEnabled = inversionEnabled; copy.inversionIntensity = inversionIntensity;
        copy.invertRed = invertRed; copy.invertGreen = invertGreen; copy.invertBlue = invertBlue;
        copy.filterEnabled = filterEnabled; copy.filterPreset = filterPreset; copy.filterIntensity = filterIntensity;
        copy.customMatrixEnabled = customMatrixEnabled; copy.customMatrix = safeArray(customMatrix, 16, Matrix4.identity().toArray());
        copy.restoreAtBoot = restoreAtBoot; copy.experimentalFeaturesEnabled = experimentalFeaturesEnabled;
        return copy;
    }

    public void sanitize() {
        schemaVersion = CURRENT_SCHEMA;
        if (activeProfileId == null) activeProfileId = "neutral";
        globalSaturation = finite(globalSaturation, 1.0);
        redSaturation = finite(redSaturation, 1.0); greenSaturation = finite(greenSaturation, 1.0); blueSaturation = finite(blueSaturation, 1.0);
        globalContrast = finite(globalContrast, 1.0);
        redContrast = finite(redContrast, 1.0); greenContrast = finite(greenContrast, 1.0); blueContrast = finite(blueContrast, 1.0);
        digitalBrightnessGain = finite(digitalBrightnessGain, 1.0); digitalBrightnessOffset = finite(digitalBrightnessOffset, 0.0);
        redBrightnessGain = finite(redBrightnessGain, 1.0); greenBrightnessGain = finite(greenBrightnessGain, 1.0); blueBrightnessGain = finite(blueBrightnessGain, 1.0);
        blackLevel = finite(blackLevel, 0.0); redBlackLevel = finite(redBlackLevel, 0.0); greenBlackLevel = finite(greenBlackLevel, 0.0); blueBlackLevel = finite(blueBlackLevel, 0.0);
        whiteLevel = finite(whiteLevel, 1.0); redWhiteLevel = finite(redWhiteLevel, 1.0); greenWhiteLevel = finite(greenWhiteLevel, 1.0); blueWhiteLevel = finite(blueWhiteLevel, 1.0);
        redGain = finite(redGain, 1.0); greenGain = finite(greenGain, 1.0); blueGain = finite(blueGain, 1.0);
        redOffset = finite(redOffset, 0.0); greenOffset = finite(greenOffset, 0.0); blueOffset = finite(blueOffset, 0.0);
        temperatureKelvin = positiveFinite(temperatureKelvin, 6500.0);
        neutralTemperatureKelvin = positiveFinite(neutralTemperatureKelvin, 6500.0);
        temperatureRed = finite(temperatureRed, 1.0); temperatureGreen = finite(temperatureGreen, 1.0); temperatureBlue = finite(temperatureBlue, 1.0);
        tint = finite(tint, 0.0); hueDegrees = finite(hueDegrees, 0.0);
        channelMixer = safeArray(channelMixer, 9, identityArray());
        grayscaleIntensity = finite(grayscaleIntensity, 1.0);
        if (grayscaleCoefficients == null) grayscaleCoefficients = "REC709";
        customLumaRed = finite(customLumaRed, 0.2126); customLumaGreen = finite(customLumaGreen, 0.7152); customLumaBlue = finite(customLumaBlue, 0.0722);
        inversionIntensity = finite(inversionIntensity, 1.0);
        if (filterPreset == null) filterPreset = "NONE";
        filterIntensity = finite(filterIntensity, 1.0);
        customMatrix = safeArray(customMatrix, 16, Matrix4.identity().toArray());
    }

    private static double finite(double value, double fallback) {
        return Double.isFinite(value) ? value : fallback;
    }

    private static double positiveFinite(double value, double fallback) {
        return Double.isFinite(value) && value > 0 ? value : fallback;
    }

    private static double[] safeArray(double[] value, int length, double[] fallback) {
        if (value == null || value.length != length) return fallback.clone();
        double[] result = value.clone();
        for (int i = 0; i < result.length; i++) {
            if (!Double.isFinite(result[i])) result[i] = fallback[i];
        }
        return result;
    }

    public static double[] identityArray() {
        return new double[]{1, 0, 0, 0, 1, 0, 0, 0, 1};
    }

    @Override
    public String toString() {
        return "DisplayConfiguration{" + schemaVersion + ", profile=" + activeProfileId
                + ", matrix=" + Arrays.toString(customMatrix) + "}";
    }
}
