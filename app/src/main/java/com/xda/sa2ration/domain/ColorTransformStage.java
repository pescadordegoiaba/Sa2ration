package com.xda.sa2ration.domain;

public interface ColorTransformStage {
    String id();
    boolean isEnabled(DisplayConfiguration configuration);
    Matrix4 createMatrix(DisplayConfiguration configuration);

    default boolean isLinear() {
        return true;
    }

    default BackendRequirement backendRequirement() {
        return BackendRequirement.LINEAR_COLOR_MATRIX;
    }
}
