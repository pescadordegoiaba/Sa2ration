package com.xda.sa2ration.backend;

public final class SurfaceFlingerTransactions {
    public final Integer colorMatrix;
    public final Integer globalSaturation;
    public final Integer colorManagement;
    public final Integer daltonizer;
    public final Integer queryColorManaged;
    public final int confidence;
    public final String warning;

    public SurfaceFlingerTransactions(Integer colorMatrix,Integer globalSaturation,Integer colorManagement,
                                      Integer daltonizer,Integer queryColorManaged,int confidence,String warning){
        this.colorMatrix=colorMatrix;this.globalSaturation=globalSaturation;this.colorManagement=colorManagement;
        this.daltonizer=daltonizer;this.queryColorManaged=queryColorManaged;this.confidence=confidence;this.warning=warning;
    }
}
