package com.xda.sa2ration.backend;

/** Resolves only known AOSP legacy transactions; OEM validation still occurs at execution. */
public final class SurfaceFlingerTransactionResolver {
    public SurfaceFlingerTransactions resolve(){
        return new SurfaceFlingerTransactions(1015,1022,1023,1014,1030,70,
                "Números AOSP prováveis; ROMs OEM podem alterá-los. Consultas desconhecidas não são executadas.");
    }
}
