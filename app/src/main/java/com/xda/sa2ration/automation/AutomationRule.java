package com.xda.sa2ration.automation;
import java.util.UUID;
public final class AutomationRule {public String id=UUID.randomUUID().toString();public String name="Nova regra";public boolean enabled=true;public int priority;public AutomationConditionType conditionType=AutomationConditionType.APPLICATION;public String operand="";public String profileId="neutral";}
