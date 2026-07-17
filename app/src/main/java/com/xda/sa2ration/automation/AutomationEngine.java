package com.xda.sa2ration.automation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class AutomationEngine {
    public AutomationRule select(List<AutomationRule> rules,AutomationContext context){
        if(rules==null||context==null)return null;
        List<AutomationRule> sorted=new ArrayList<>();for(AutomationRule rule:rules)if(rule!=null)sorted.add(rule);
        sorted.sort(Comparator.comparingInt((AutomationRule rule)->rule.priority).reversed());
        for(AutomationRule rule:sorted)if(rule.enabled&&matches(rule,context))return rule;
        return null;
    }

    public boolean matches(AutomationRule rule,AutomationContext context){
        if(rule==null||context==null||rule.conditionType==null||rule.operand==null)return false;
        try{switch(rule.conditionType){
            case APPLICATION:return rule.operand.equals(context.foregroundPackage);
            case CHARGING:return strictBoolean(rule.operand)==context.charging;
            case POWER_SAVER:return strictBoolean(rule.operand)==context.powerSaver;
            case HDR:return strictBoolean(rule.operand)==context.hdr;
            case GAME:return strictBoolean(rule.operand)==context.game;
            case VIDEO:return strictBoolean(rule.operand)==context.video;
            case EXTERNAL_DISPLAY:return strictBoolean(rule.operand)==context.externalDisplay;
            case DAY_OF_WEEK:return Integer.parseInt(rule.operand)==context.dayOfWeek;
            case BATTERY_LEVEL:return context.batteryPercent<=Integer.parseInt(rule.operand);
            case AMBIENT_LIGHT:return context.ambientLux<=Double.parseDouble(rule.operand);
            case CURRENT_BRIGHTNESS:return context.brightness<=Integer.parseInt(rule.operand);
            case TIME_RANGE:
                String[] parts=rule.operand.split("-",-1);if(parts.length!=2)return false;
                int start=minutes(parts[0]),end=minutes(parts[1]),now=context.hour*60+context.minute;
                return start<=end?now>=start&&now<=end:now>=start||now<=end;
            default:return false;
        }}catch(Exception ignored){return false;}
    }

    private boolean strictBoolean(String value){if("true".equalsIgnoreCase(value))return true;if("false".equalsIgnoreCase(value))return false;throw new IllegalArgumentException("invalid boolean");}
    private int minutes(String value){String[] parts=value.trim().split(":",-1);if(parts.length!=2)throw new IllegalArgumentException("invalid time");int hour=Integer.parseInt(parts[0]),minute=Integer.parseInt(parts[1]);if(hour<0||hour>23||minute<0||minute>59)throw new IllegalArgumentException("invalid time");return hour*60+minute;}
}
