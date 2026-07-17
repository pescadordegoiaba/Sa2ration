package com.xda.sa2ration.panel;

import android.content.Context;import android.hardware.display.DisplayManager;import android.os.Build;import android.view.Display;
import com.xda.sa2ration.shell.RootShellExecutor;import com.xda.sa2ration.shell.ShellCommand;import com.xda.sa2ration.shell.ShellResult;
import java.util.Locale;import java.util.regex.Matcher;import java.util.regex.Pattern;

public final class DisplayPanelDetector {
    private final Context context;private final RootShellExecutor executor;
    public DisplayPanelDetector(Context context,RootShellExecutor executor){this.context=context.getApplicationContext();this.executor=executor;}

    public DisplayPanelInfo detect(String manualSelection){
        DisplayPanelInfo info=new DisplayPanelInfo();info.manufacturer=Build.MANUFACTURER;
        StringBuilder evidenceText=new StringBuilder();
        DisplayManager manager=(DisplayManager)context.getSystemService(Context.DISPLAY_SERVICE);Display[] displays=manager.getDisplays();
        for(Display display:displays){Display.Mode mode=display.getMode();evidenceText.append(display.getName()).append(' ').append(mode.getPhysicalWidth()).append('x').append(mode.getPhysicalHeight()).append('@').append(mode.getRefreshRate()).append('\n');}
        info.addSource("DisplayManager/Display.Mode");
        String command="echo __PROPS__; getprop | grep -iE 'panel|display|oled|amoled|lcd|ltpo|backlight|dsi' | head -n 160; "
                +"echo __DISPLAY__; dumpsys display 2>/dev/null | grep -iE 'name|display|mode|hdr|density' | head -n 180; "
                +"echo __SF__; dumpsys SurfaceFlinger --display-id 2>/dev/null | head -n 120; "
                +"echo __SYSFS__; for f in /sys/class/drm/*/panel_name /sys/class/drm/*/name /sys/class/graphics/fb*/name /sys/devices/platform/*/panel_name; do [ -r \"$f\" ] && echo \"$f=$(cat \"$f\" 2>/dev/null)\"; done; "
                +"echo __BACKLIGHT__; for d in /sys/class/backlight/*; do [ -e \"$d\" ] && echo \"$d\"; done";
        ShellResult shell=executor.executeBlocking(ShellCommand.builder(command).timeoutMs(18_000).build());
        evidenceText.append(shell.stdout);String text=evidenceText.toString().toLowerCase(Locale.US);
        info.addSource("getprop");info.addSource("dumpsys display");info.addSource("dumpsys SurfaceFlinger --display-id");info.addSource("/sys/class/drm");info.addSource("/sys/class/backlight");info.addSource("/sys/class/graphics");info.addSource("device tree/platform panel names");
        DisplayPanelInfo classified=PanelClassifier.classify(text);PanelTechnology best=classified.detectedTechnology;
        info.detectedTechnology=best;info.effectiveTechnology=best;info.confidence=classified.confidence;info.hardwareSupportInferred=classified.hardwareSupportInferred;
        for(com.xda.sa2ration.diagnostics.DetectionEvidence e:classified.evidence())info.addEvidence(e.source,e.detail,e.weight);
        Matcher panel=Pattern.compile("(?:panel_name|panel name|\\[.*panel.*\\])[^\\n=:]*[=:]\\s*([^\\n]+)",Pattern.CASE_INSENSITIVE).matcher(evidenceText);if(panel.find())info.panelName=panel.group(1).trim();
        if(info.panelName.isEmpty())info.panelName="Não informado";
        String manu=Build.MANUFACTURER.toLowerCase(Locale.US);if(manu.contains("samsung"))info.probableBackend="samsung/exynos";else if(text.contains("qcom")||text.contains("qualcomm"))info.probableBackend="qualcomm";else if(text.contains("mediatek")||text.contains("mtk"))info.probableBackend="mediatek";else if(manu.contains("google"))info.probableBackend="pixel";
        PanelTechnology manual=parseManual(manualSelection);if(manual!=null){info.manuallyOverridden=true;info.effectiveTechnology=manual;info.manualConflict=best!=PanelTechnology.UNKNOWN&&manual!=best;info.addEvidence("seleção manual","Interface forçada para "+manual,0);}
        return info;
    }

    private PanelTechnology parseManual(String value){if(value==null||"AUTO".equals(value))return null;try{return PanelTechnology.valueOf(value);}catch(Exception ignored){return PanelTechnology.OTHER;}}
}
