package com.xda.sa2ration.domain;

import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ColorTransformPipelineTest {
    private static final double EPS = 1e-5;

    @Test public void allEffectsDisabledProducesIdentity() {
        DisplayConfiguration c=allDisabled();
        assertTrue(DefaultTransformStages.createPipeline().compose(c).isIdentity());
    }

    @Test public void masterSwitchProducesIdentity() {
        DisplayConfiguration c=DisplayConfiguration.neutral();
        c.hueEnabled=true; c.hueDegrees=90; c.masterEnabled=false;
        assertTrue(DefaultTransformStages.createPipeline().compose(c).isIdentity());
    }

    @Test public void disablingStageCompletelyRemovesItsEffect() {
        DisplayConfiguration c=allDisabled();
        c.rgbGainEnabled=true; c.redGain=3;
        assertFalse(DefaultTransformStages.createPipeline().compose(c).isIdentity());
        c.rgbGainEnabled=false;
        assertTrue(DefaultTransformStages.createPipeline().compose(c).isIdentity());
        assertEquals(3,c.redGain,0); // preserved for re-enable
    }

    @Test public void declaredOrderIsStable() {
        String[] ids={"temperature","tint","rgb_gain","rgb_offset","channel_mixer","hue",
                "rgb_saturation","contrast","digital_brightness","black_level","white_level",
                "grayscale","inversion","filter","custom_matrix"};
        for(int i=0;i<ids.length;i++) assertEquals(ids[i],DefaultTransformStages.createPipeline().stages().get(i).id());
    }

    @Test public void rgbGainAndOffsetAreCorrect() {
        DisplayConfiguration c=allDisabled(); c.rgbGainEnabled=true;c.redGain=2;c.greenGain=3;c.blueGain=4;
        c.rgbOffsetEnabled=true;c.redOffset=.1;c.greenOffset=.2;c.blueOffset=.3;
        Matrix4 m=DefaultTransformStages.createPipeline().compose(c);
        assertEquals(2,m.get(0,0),EPS);assertEquals(3,m.get(1,1),EPS);assertEquals(4,m.get(2,2),EPS);
        assertEquals(.1,m.get(0,3),EPS);assertEquals(.2,m.get(1,3),EPS);assertEquals(.3,m.get(2,3),EPS);
    }

    @Test public void contrastCentersAroundHalf() {
        DisplayConfiguration c=allDisabled();c.globalContrastEnabled=true;c.globalContrast=2;
        Matrix4 m=DefaultTransformStages.createPipeline().compose(c);
        assertEquals(2,m.get(0,0),EPS); assertEquals(-.5,m.get(0,3),EPS);
    }

    @Test public void digitalBrightnessCombinesGainAndOffset() {
        DisplayConfiguration c=allDisabled();c.digitalBrightnessEnabled=true;c.digitalBrightnessGain=1.5;
        c.redBrightnessGain=2;c.digitalBrightnessOffset=.25;
        Matrix4 m=DefaultTransformStages.createPipeline().compose(c);
        assertEquals(3,m.get(0,0),EPS);assertEquals(1.5,m.get(1,1),EPS);assertEquals(.25,m.get(2,3),EPS);
    }

    @Test public void blackAndWhiteLevelsAreLinearAndOrdered() {
        DisplayConfiguration c=allDisabled();c.blackLevelEnabled=true;c.blackLevel=.1;
        c.whiteLevelEnabled=true;c.whiteLevel=2;
        Matrix4 m=DefaultTransformStages.createPipeline().compose(c);
        assertEquals(2,m.get(0,0),EPS);assertEquals(.2,m.get(0,3),EPS);
    }

    @Test public void hueZeroIsIdentity() {
        DisplayConfiguration c=allDisabled();c.hueEnabled=true;c.hueDegrees=0;
        assertTrue(DefaultTransformStages.createPipeline().compose(c).approximatelyEquals(Matrix4.identity(),1e-3));
    }

    @Test public void temperatureNeutralIsIdentity() {
        DisplayConfiguration c=allDisabled();c.temperatureEnabled=true;c.temperatureKelvin=6500;c.neutralTemperatureKelvin=6500;
        assertTrue(DefaultTransformStages.createPipeline().compose(c).approximatelyEquals(Matrix4.identity(),EPS));
    }

    @Test public void tintNeutralIsIdentity() {
        DisplayConfiguration c=allDisabled();c.tintEnabled=true;c.tint=0;
        assertTrue(DefaultTransformStages.createPipeline().compose(c).isIdentity());
    }

    @Test public void channelMixerCanSwapRedAndBlue() {
        DisplayConfiguration c=allDisabled();c.channelMixerEnabled=true;
        c.channelMixer=new double[]{0,0,1,0,1,0,1,0,0};
        Matrix4 m=DefaultTransformStages.createPipeline().compose(c);
        assertEquals(1,m.get(0,2),EPS);assertEquals(1,m.get(2,0),EPS);
    }

    @Test public void grayscaleRec709UsesExpectedCoefficients() {
        DisplayConfiguration c=allDisabled();c.grayscaleEnabled=true;c.grayscaleCoefficients="REC709";c.grayscaleIntensity=1;
        Matrix4 m=DefaultTransformStages.createPipeline().compose(c);
        assertEquals(.2126,m.get(0,0),EPS);assertEquals(.7152,m.get(1,1),EPS);assertEquals(.0722,m.get(2,2),EPS);
    }

    @Test public void inversionCanTargetOneChannel() {
        DisplayConfiguration c=allDisabled();c.inversionEnabled=true;c.invertRed=true;c.invertGreen=false;c.invertBlue=false;c.inversionIntensity=1;
        Matrix4 m=DefaultTransformStages.createPipeline().compose(c);
        assertEquals(-1,m.get(0,0),EPS);assertEquals(1,m.get(0,3),EPS);assertEquals(1,m.get(1,1),EPS);
    }

    @Test public void filterIntensityZeroIsIdentity() {
        DisplayConfiguration c=allDisabled();c.filterEnabled=true;c.filterPreset="SEPIA";c.filterIntensity=0;
        assertTrue(DefaultTransformStages.createPipeline().compose(c).isIdentity());
    }

    @Test public void customMatrixIsComposed() {
        DisplayConfiguration c=allDisabled();c.customMatrixEnabled=true;c.customMatrix=Matrix4.diagonal(2,3,4).toArray();
        assertEquals(3,DefaultTransformStages.createPipeline().compose(c).get(1,1),EPS);
    }

    @Test(expected=IllegalArgumentException.class)
    public void customMatrixRejectsInvalidLastRow() {
        DisplayConfiguration c=allDisabled();c.customMatrixEnabled=true;c.customMatrix=Matrix4.identity().toArray();c.customMatrix[3]=1;
        DefaultTransformStages.createPipeline().compose(c);
    }

    @Test public void sanitizationReplacesNanAndInfinity() {
        DisplayConfiguration c=allDisabled();c.rgbGainEnabled=true;c.redGain=Double.NaN;c.greenGain=Double.POSITIVE_INFINITY;
        Matrix4 m=DefaultTransformStages.createPipeline().compose(c);
        assertEquals(1,m.get(0,0),EPS);assertEquals(1,m.get(1,1),EPS);
    }

    @Test public void emptyPipelineIsIdentity() {
        assertTrue(new ColorTransformPipeline(Collections.emptyList()).compose(DisplayConfiguration.neutral()).isIdentity());
    }

    private static DisplayConfiguration allDisabled() {
        DisplayConfiguration c=DisplayConfiguration.neutral();
        c.globalSaturationEnabled=false;c.rgbSaturationEnabled=false;c.globalContrastEnabled=false;c.rgbContrastEnabled=false;
        return c;
    }
}
