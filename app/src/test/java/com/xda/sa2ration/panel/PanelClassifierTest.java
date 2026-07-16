package com.xda.sa2ration.panel;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PanelClassifierTest {
    @Test public void detectsDynamicAmoled(){DisplayPanelInfo i=PanelClassifier.classify("panel=Dynamic AMOLED 2X s6e3");assertEquals(PanelTechnology.DYNAMIC_AMOLED,i.detectedTechnology);assertTrue(i.confidence>=90);}
    @Test public void detectsLtpoOled(){DisplayPanelInfo i=PanelClassifier.classify("panel_name=ltpo oled dsi");assertEquals(PanelTechnology.LTPO_OLED,i.detectedTechnology);}
    @Test public void detectsIpsLcdWithBacklight(){DisplayPanelInfo i=PanelClassifier.classify("IPS LCD /sys/class/backlight/panel0");assertEquals(PanelTechnology.IPS_LCD,i.detectedTechnology);assertTrue(i.confidence>=80);}
    @Test public void detectsMiniLed(){assertEquals(PanelTechnology.MINI_LED,PanelClassifier.classify("mini-led local dimming").detectedTechnology);}
    @Test public void brandAloneDoesNotClassify(){assertEquals(PanelTechnology.UNKNOWN,PanelClassifier.classify("manufacturer=samsung").detectedTechnology);}
    @Test public void unknownEvidenceStaysUnknown(){assertEquals(PanelTechnology.UNKNOWN,PanelClassifier.classify("generic display dsi").detectedTechnology);}
}
