package com.xda.sa2ration.domain;

import com.xda.sa2ration.automation.AutomationRule;
import java.util.ArrayList;
import java.util.List;

public final class StoredApplicationState {
    public int schemaVersion = DisplayConfiguration.CURRENT_SCHEMA;
    public DisplayConfiguration current = DisplayConfiguration.neutral();
    public DisplayConfiguration stable = DisplayConfiguration.neutral();
    public int consecutiveBootFailures;
    public long lastConfirmedAtEpochMs;
    public String selectedPanelTechnology = "AUTO";
    public String selectedRootImplementation = "AUTO";
    public String selectedBackend = "AUTO";
    public List<AutomationRule> automations = new ArrayList<>();

    public void sanitize() {
        schemaVersion = DisplayConfiguration.CURRENT_SCHEMA;
        if (current == null) current = DisplayConfiguration.neutral();
        if (stable == null) stable = DisplayConfiguration.neutral();
        current.sanitize();
        stable.sanitize();
        if (consecutiveBootFailures < 0) consecutiveBootFailures = 0;
        if (selectedPanelTechnology == null) selectedPanelTechnology = "AUTO";
        if (selectedRootImplementation == null) selectedRootImplementation = "AUTO";
        if (selectedBackend == null) selectedBackend = "AUTO";
        if (automations == null) automations = new ArrayList<>();
    }
}
