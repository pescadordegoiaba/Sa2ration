package com.xda.sa2ration.ui;

import com.xda.sa2ration.domain.DisplayConfiguration;
import com.xda.sa2ration.domain.Matrix4;
import com.xda.sa2ration.backend.SurfaceFlingerCapabilities;
import com.xda.sa2ration.panel.DisplayPanelInfo;
import com.xda.sa2ration.root.RootDetectionResult;
import com.xda.sa2ration.profile.DisplayProfile;
import java.util.Collections;
import java.util.List;

public final class DisplayUiState {
    public final DisplayConfiguration configuration;
    public final Matrix4 finalMatrix;
    public final boolean loading;
    public final boolean applying;
    public final boolean awaitingConfirmation;
    public final int countdownSeconds;
    public final String backendName;
    public final String message;
    public final RootDetectionResult root;
    public final DisplayPanelInfo panel;
    public final SurfaceFlingerCapabilities surfaceFlinger;
    public final String selectedRoot;
    public final String selectedPanel;
    public final String selectedBackend;
    public final List<DisplayProfile> profiles;

    public DisplayUiState(DisplayConfiguration configuration, Matrix4 finalMatrix, boolean loading,
                          boolean applying, boolean awaitingConfirmation, int countdownSeconds,
                          String backendName, String message,RootDetectionResult root,
                          DisplayPanelInfo panel,SurfaceFlingerCapabilities surfaceFlinger,
                          String selectedRoot,String selectedPanel,String selectedBackend,
                          List<DisplayProfile> profiles) {
        this.configuration = configuration;
        this.finalMatrix = finalMatrix;
        this.loading = loading;
        this.applying = applying;
        this.awaitingConfirmation = awaitingConfirmation;
        this.countdownSeconds = countdownSeconds;
        this.backendName = backendName;
        this.message = message;
        this.root=root;this.panel=panel;this.surfaceFlinger=surfaceFlinger;
        this.selectedRoot=selectedRoot;this.selectedPanel=selectedPanel;this.selectedBackend=selectedBackend;
        this.profiles=profiles==null?Collections.emptyList():Collections.unmodifiableList(profiles);
    }

    public static DisplayUiState loading() {
        return new DisplayUiState(DisplayConfiguration.neutral(), Matrix4.identity(), true,
                false, false, 0, "Detectando…", "",new RootDetectionResult(),
                new DisplayPanelInfo(),null,"AUTO","AUTO","AUTO",Collections.emptyList());
    }

    public DisplayUiState with(DisplayConfiguration configuration, Matrix4 matrix, boolean applying,
                               boolean awaiting, int seconds, String message) {
        return new DisplayUiState(configuration, matrix, false, applying, awaiting, seconds,
                backendName, message == null ? "" : message,root,panel,surfaceFlinger,
                selectedRoot,selectedPanel,selectedBackend,profiles);
    }
}
