package com.xda.sa2ration.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Composes stages in declared order. Disabled stages are omitted entirely. */
public final class ColorTransformPipeline {
    private final List<ColorTransformStage> stages;

    public ColorTransformPipeline(List<ColorTransformStage> stages) {
        this.stages = Collections.unmodifiableList(new ArrayList<>(stages));
    }

    public Matrix4 compose(DisplayConfiguration configuration) {
        if (configuration == null || !configuration.masterEnabled) return Matrix4.identity();
        configuration.sanitize();
        Matrix4 result = Matrix4.identity();
        for (ColorTransformStage stage : stages) {
            if (stage.isEnabled(configuration)) {
                result = result.then(stage.createMatrix(configuration));
            }
        }
        return result;
    }

    public List<ColorTransformStage> stages() {
        return stages;
    }
}
