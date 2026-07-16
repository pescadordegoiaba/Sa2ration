package com.xda.sa2ration.profile;

import com.xda.sa2ration.domain.DisplayConfiguration;
import java.util.ArrayList;import java.util.List;import java.util.Locale;

public final class ProfileTemplates {
    private ProfileTemplates(){}
    public static List<DisplayProfile> defaults(){List<DisplayProfile> list=new ArrayList<>();
        list.add(profile("neutral","Neutro",c->{}));list.add(profile("natural","Natural",c->{c.globalSaturation=1.0;c.globalContrast=1.0;}));
        list.add(profile("srgb","sRGB",c->{c.colorManagementEnabled=true;c.globalSaturation=.98;}));
        list.add(profile("vivid","Vívido",c->{c.globalSaturation=1.25;c.globalContrast=1.08;}));
        list.add(profile("amoled","AMOLED",c->{c.globalSaturation=1.18;c.blackLevelEnabled=true;c.blackLevel=-.015;}));
        list.add(profile("lcd","LCD",c->{c.globalSaturation=1.08;c.globalContrast=1.05;}));
        list.add(profile("movie","Filme",c->{c.temperatureEnabled=true;c.temperatureKelvin=5500;c.globalContrast=1.08;}));
        list.add(profile("photo","Fotografia",c->{c.globalSaturation=1.0;c.globalContrast=1.0;c.colorManagementEnabled=true;}));
        list.add(profile("reading","Leitura",c->{c.temperatureEnabled=true;c.temperatureKelvin=4000;c.globalSaturation=.75;}));
        list.add(profile("night","Noturno",c->{c.temperatureEnabled=true;c.temperatureKelvin=2700;c.digitalBrightnessEnabled=true;c.digitalBrightnessGain=.75;}));
        list.add(profile("games","Jogos",c->{c.globalSaturation=1.2;c.globalContrast=1.1;}));
        list.add(profile("fps","FPS",c->{c.globalContrast=1.18;c.blackLevelEnabled=true;c.blackLevel=.05;}));
        list.add(profile("low_light","Baixa luminosidade",c->{c.digitalBrightnessEnabled=true;c.digitalBrightnessGain=.55;c.temperatureEnabled=true;c.temperatureKelvin=3400;}));
        list.add(profile("grayscale","Escala de cinza",c->{c.grayscaleEnabled=true;c.grayscaleIntensity=1;}));
        list.add(profile("oled_saver","Economia OLED",c->{c.digitalBrightnessEnabled=true;c.digitalBrightnessGain=.7;c.grayscaleEnabled=true;c.grayscaleIntensity=.25;}));
        list.add(profile("outdoor","Externo",c->{c.globalContrast=1.15;c.globalSaturation=1.12;}));return list;}
    private interface Setup{void apply(DisplayConfiguration c);}private static DisplayProfile profile(String id,String name,Setup setup){DisplayProfile p=new DisplayProfile();p.id=id;p.name=name;p.builtIn=true;p.configuration=DisplayConfiguration.neutral();p.configuration.activeProfileId=id;setup.apply(p.configuration);return p;}
}
