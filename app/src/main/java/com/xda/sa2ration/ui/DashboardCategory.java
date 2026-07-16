package com.xda.sa2ration.ui;

public enum DashboardCategory {
    SIMPLE("Simples"), COLOR("Cor"), RGB("RGB"), DISPLAY("Tela"), OLED("OLED"), LCD("LCD"),
    ACCESSIBILITY("Acessibilidade"), PROFILES("Perfis"), AUTOMATION("Automação"),
    COMPATIBILITY("Compatibilidade"), ADVANCED("Avançado");
    public final String title;
    DashboardCategory(String title){this.title=title;}
}
