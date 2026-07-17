package com.xda.sa2ration.panel;

import java.util.EnumMap;import java.util.Locale;import java.util.Map;

public final class PanelClassifier {
    private PanelClassifier(){}
    public static DisplayPanelInfo classify(String rawEvidence){
        DisplayPanelInfo info=new DisplayPanelInfo();Map<PanelTechnology,Integer> scores=new EnumMap<>(PanelTechnology.class);String text=rawEvidence==null?"":rawEvidence.toLowerCase(Locale.US);
        score(text,"dynamic amoled",PanelTechnology.DYNAMIC_AMOLED,95,scores,info);score(text,"super amoled",PanelTechnology.SUPER_AMOLED,95,scores,info);
        if(text.contains("ltpo")&&(text.contains("oled")||text.contains("amoled")||text.contains("s6e3")))add(PanelTechnology.LTPO_OLED,90,"LTPO combinado com evidência OLED",scores,info);
        if(text.contains("mini-led")||text.contains("mini_led")||text.contains("miniled"))add(PanelTechnology.MINI_LED,95,"nome contém Mini-LED",scores,info);
        score(text,"poled",PanelTechnology.POLED,90,scores,info);score(text,"amoled",PanelTechnology.AMOLED,85,scores,info);score(text,"oled",PanelTechnology.OLED,75,scores,info);
        score(text,"pls",PanelTechnology.PLS_LCD,85,scores,info);score(text,"ips",PanelTechnology.IPS_LCD,80,scores,info);
        if(text.contains("ltps")&&text.contains("lcd"))add(PanelTechnology.LTPS_LCD,85,"LTPS e LCD encontrados",scores,info);
        if(text.contains("ltpo")&&text.contains("lcd"))add(PanelTechnology.LTPO_LCD,80,"LTPO e LCD encontrados",scores,info);
        score(text,"tft",PanelTechnology.TFT_LCD,55,scores,info);score(text,"lcd",PanelTechnology.LCD,60,scores,info);
        if(text.contains("/sys/class/backlight/")&&!text.contains("no backlight"))add(PanelTechnology.LCD,45,"backlight físico exposto",scores,info);
        if((text.contains("s6e3")||text.contains("s6e8")||text.contains("ea807"))&&!text.contains("/sys/class/backlight/"))add(PanelTechnology.AMOLED,58,"código de painel OLED conhecido e sem backlight exposto",scores,info);
        PanelTechnology best=PanelTechnology.UNKNOWN;int max=0;for(Map.Entry<PanelTechnology,Integer> e:scores.entrySet())if(e.getValue()>max){best=e.getKey();max=e.getValue();}
        info.detectedTechnology=best;info.effectiveTechnology=best;info.confidence=Math.min(99,max);info.hardwareSupportInferred=best!=PanelTechnology.UNKNOWN;return info;
    }
    private static void score(String text,String keyword,PanelTechnology t,int w,Map<PanelTechnology,Integer>s,DisplayPanelInfo i){if(text.contains(keyword))add(t,w,"evidência contém '"+keyword+"'",s,i);}
    private static void add(PanelTechnology t,int w,String d,Map<PanelTechnology,Integer>s,DisplayPanelInfo i){s.put(t,Math.max(s.getOrDefault(t,0),w));i.addEvidence("heurística",d,w);}
}
