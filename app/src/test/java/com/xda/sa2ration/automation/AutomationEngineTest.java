package com.xda.sa2ration.automation;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public final class AutomationEngineTest {
    @Test public void highestPriorityMatchingRuleWins(){AutomationRule low=new AutomationRule();low.conditionType=AutomationConditionType.CHARGING;low.operand="true";low.priority=1;AutomationRule high=new AutomationRule();high.conditionType=AutomationConditionType.APPLICATION;high.operand="com.video";high.priority=50;AutomationContext context=new AutomationContext();context.charging=true;context.foregroundPackage="com.video";assertSame(high,new AutomationEngine().select(Arrays.asList(low,high),context));}
    @Test public void invalidOperandDoesNotCrashOrMatchFalseBoolean(){AutomationRule time=new AutomationRule();time.conditionType=AutomationConditionType.TIME_RANGE;time.operand="invalid";assertFalse(new AutomationEngine().matches(time,new AutomationContext()));AutomationRule bool=new AutomationRule();bool.conditionType=AutomationConditionType.CHARGING;bool.operand="invalid";assertFalse(new AutomationEngine().matches(bool,new AutomationContext()));}
    @Test public void timeRangeCanCrossMidnight(){AutomationRule rule=new AutomationRule();rule.conditionType=AutomationConditionType.TIME_RANGE;rule.operand="22:00-06:00";AutomationContext context=new AutomationContext();context.hour=23;assertTrue(new AutomationEngine().matches(rule,context));context.hour=5;assertTrue(new AutomationEngine().matches(rule,context));context.hour=12;assertFalse(new AutomationEngine().matches(rule,context));}
    @Test public void nullRulesAreIgnored(){AutomationRule rule=new AutomationRule();rule.conditionType=AutomationConditionType.CHARGING;rule.operand="true";AutomationContext context=new AutomationContext();context.charging=true;assertSame(rule,new AutomationEngine().select(Arrays.asList(null,rule),context));}
}
