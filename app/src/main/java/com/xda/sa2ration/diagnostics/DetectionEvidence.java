package com.xda.sa2ration.diagnostics;

public final class DetectionEvidence {
    public final String source;
    public final String detail;
    public final int weight;

    public DetectionEvidence(String source,String detail,int weight){this.source=source;this.detail=detail;this.weight=weight;}
}
