package com.xda.sa2ration.ui;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.xda.sa2ration.backend.CapabilityStatus;
import com.xda.sa2ration.domain.DisplayConfiguration;
import com.xda.sa2ration.domain.FilterPreset;
import com.xda.sa2ration.ui.widget.CapabilityCard;
import com.xda.sa2ration.ui.widget.EffectControlCard;
import com.xda.sa2ration.profile.DisplayProfile;
import com.xda.sa2ration.R;

import java.util.Locale;

public final class DashboardFragment extends Fragment {
    private static final String ARG_CATEGORY="category";
    private DisplayViewModel viewModel;
    private LinearLayout content;
    private boolean rendered;

    public static DashboardFragment newInstance(DashboardCategory category){
        DashboardFragment fragment=new DashboardFragment();Bundle args=new Bundle();args.putString(ARG_CATEGORY,category.name());fragment.setArguments(args);return fragment;
    }

    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater,@Nullable ViewGroup container,@Nullable Bundle savedInstanceState){
        Context context=requireContext();NestedScrollView scroll=new NestedScrollView(context);scroll.setFillViewport(true);
        content=new LinearLayout(context);content.setOrientation(LinearLayout.VERTICAL);content.setPadding(dp(12),dp(12),dp(12),dp(24));scroll.addView(content);
        ProgressBar progress=new ProgressBar(context);content.addView(progress,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT));
        viewModel=new ViewModelProvider(requireActivity()).get(DisplayViewModel.class);
        viewModel.state().observe(getViewLifecycleOwner(),state->{if(!rendered&&!state.loading){rendered=true;content.removeAllViews();render(category(),state);}});
        return scroll;
    }

    private DashboardCategory category(){String name=getArguments()==null?null:getArguments().getString(ARG_CATEGORY);try{return DashboardCategory.valueOf(name);}catch(Exception ignored){return DashboardCategory.SIMPLE;}}

    private void render(DashboardCategory category,DisplayUiState state){
        DisplayConfiguration c=state.configuration;
        addTitle(category.title);
        switch(category){
            case SIMPLE:renderSimple(c,state);break;case COLOR:renderColor(c);break;case RGB:renderRgb(c);break;
            case DISPLAY:renderDisplay();break;case OLED:renderOled();break;case LCD:renderLcd();break;
            case ACCESSIBILITY:renderAccessibility(c);break;case PROFILES:renderProfiles(state);break;
            case AUTOMATION:renderAutomation();break;case COMPATIBILITY:renderCompatibility(state);break;
            case ADVANCED:renderAdvanced(c,state);break;
        }
    }

    @SuppressLint("ClickableViewAccessibility") // Touch path invokes performClick on ACTION_UP.
    private void renderSimple(DisplayConfiguration c,DisplayUiState state){
        addEffect("Transformações do Sa2ration","Desliga todo o pipeline e aplica estado neutro.",c.masterEnabled,
                v->viewModel.update(x->x.masterEnabled=v),null);
        EffectControlCard sat=addEffect("Saturação global","Multiplicador nativo do SurfaceFlinger.",c.globalSaturationEnabled,
                v->viewModel.update(x->x.globalSaturationEnabled=v),()->reset(x->{x.globalSaturationEnabled=true;x.globalSaturation=1;}));
        sat.addNumeric("Saturação (×)",c.globalSaturation,0,10,.01,-100,100,v->viewModel.update(x->x.globalSaturation=v));
        EffectControlCard contrast=addEffect("Contraste global","Contraste linear centrado em 0,5.",c.globalContrastEnabled,
                v->viewModel.update(x->x.globalContrastEnabled=v),()->reset(x->{x.globalContrastEnabled=true;x.globalContrast=1;}));
        contrast.addNumeric("Contraste (×)",c.globalContrast,0,10,.01,-100,100,v->viewModel.update(x->x.globalContrast=v));
        addBrightness(c,true);addTemperature(c,true);
        EffectControlCard profile=addEffect("Perfil atual","Seleciona um perfil sem ocultar os controles simples.",true,v->{},null);
        String[] profileNames=new String[state.profiles.size()];for(int i=0;i<profileNames.length;i++)profileNames[i]=state.profiles.get(i).name;
        int selected=0;for(int i=0;i<state.profiles.size();i++)if(state.profiles.get(i).id.equals(c.activeProfileId)){selected=i;break;}final int profileIndex=selected;
        if(profileNames.length>0)profile.addDropdown("Perfil",profileNames,profileNames[profileIndex],(p,v,pos,id)->viewModel.applyProfile(state.profiles.get(pos).id));
        TextView backend=new TextView(requireContext());backend.setText(getString(R.string.backend_value,state.backendName));backend.setPadding(dp(8),dp(12),dp(8),dp(12));content.addView(backend);
        LinearLayout buttons=new LinearLayout(requireContext());buttons.setGravity(Gravity.CENTER);buttons.setOrientation(LinearLayout.HORIZONTAL);
        Button apply=button("Aplicar");apply.setOnClickListener(v->viewModel.apply());buttons.addView(apply,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1));
        Button reset=button("Resetar");reset.setOnClickListener(v->{viewModel.resetAll();requireActivity().recreate();});buttons.addView(reset,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1));
        content.addView(buttons);
        Button compare=button("Segure para Antes / solte para Depois");compare.setOnClickListener(v->{});compare.setOnTouchListener((v,event)->{if(event.getAction()==MotionEvent.ACTION_DOWN)viewModel.previewNeutral();else if(event.getAction()==MotionEvent.ACTION_UP){v.performClick();viewModel.previewCurrent();}else if(event.getAction()==MotionEvent.ACTION_CANCEL)viewModel.previewCurrent();return true;});content.addView(compare);
    }

    private void renderColor(DisplayConfiguration c){
        addBrightness(c,false);addBlack(c);addWhite(c);addTemperature(c,false);addTint(c);addHue(c);addGrayscale(c);addInversion(c);addFilter(c);
    }

    private void renderRgb(DisplayConfiguration c){
        EffectControlCard sat=addEffect("Saturação RGB","Saturação independente por canal.",c.rgbSaturationEnabled,v->viewModel.update(x->x.rgbSaturationEnabled=v),()->reset(x->{x.redSaturation=x.greenSaturation=x.blueSaturation=1;}));
        rgb(sat,"Saturação",c.redSaturation,c.greenSaturation,c.blueSaturation,(x,v)->x.redSaturation=v,(x,v)->x.greenSaturation=v,(x,v)->x.blueSaturation=v,0,10,-100,100);
        EffectControlCard contrast=addEffect("Contraste RGB","Contraste separado para cada canal.",c.rgbContrastEnabled,v->viewModel.update(x->x.rgbContrastEnabled=v),()->reset(x->{x.redContrast=x.greenContrast=x.blueContrast=1;}));
        rgb(contrast,"Contraste",c.redContrast,c.greenContrast,c.blueContrast,(x,v)->x.redContrast=v,(x,v)->x.greenContrast=v,(x,v)->x.blueContrast=v,0,10,-100,100);
        EffectControlCard gain=addEffect("Ganho RGB","Multiplica cada canal linearmente.",c.rgbGainEnabled,v->viewModel.update(x->x.rgbGainEnabled=v),()->reset(x->{x.redGain=x.greenGain=x.blueGain=1;}));
        rgb(gain,"Ganho",c.redGain,c.greenGain,c.blueGain,(x,v)->x.redGain=v,(x,v)->x.greenGain=v,(x,v)->x.blueGain=v,0,4,-100,100);
        EffectControlCard offset=addEffect("Offset RGB","Soma um deslocamento a cada canal.",c.rgbOffsetEnabled,v->viewModel.update(x->x.rgbOffsetEnabled=v),()->reset(x->{x.redOffset=x.greenOffset=x.blueOffset=0;}));
        rgb(offset,"Offset",c.redOffset,c.greenOffset,c.blueOffset,(x,v)->x.redOffset=v,(x,v)->x.greenOffset=v,(x,v)->x.blueOffset=v,-1,1,-10,10);
        addMixer(c);
    }

    private void addBrightness(DisplayConfiguration c,boolean compact){
        EffectControlCard card=addEffect("Brilho digital","Ganho e offset digitais; não altera a iluminação física do painel.",c.digitalBrightnessEnabled,v->viewModel.update(x->x.digitalBrightnessEnabled=v),()->reset(x->{x.digitalBrightnessGain=1;x.digitalBrightnessOffset=0;x.redBrightnessGain=x.greenBrightnessGain=x.blueBrightnessGain=1;}));
        card.addNumeric("Ganho global",c.digitalBrightnessGain,0,3,.01,-100,100,v->viewModel.update(x->x.digitalBrightnessGain=v));
        card.addNumeric("Offset global",c.digitalBrightnessOffset,-1,1,.01,-10,10,v->viewModel.update(x->x.digitalBrightnessOffset=v));
        if(!compact)rgb(card,"Ganho adicional",c.redBrightnessGain,c.greenBrightnessGain,c.blueBrightnessGain,(x,v)->x.redBrightnessGain=v,(x,v)->x.greenBrightnessGain=v,(x,v)->x.blueBrightnessGain=v,0,3,-100,100);
    }

    private void addBlack(DisplayConfiguration c){EffectControlCard card=addEffect("Nível de preto","Eleva ou esmaga sombras linearmente; não é black equalizer real.",c.blackLevelEnabled,v->viewModel.update(x->x.blackLevelEnabled=v),()->reset(x->{x.blackLevel=x.redBlackLevel=x.greenBlackLevel=x.blueBlackLevel=0;}));card.addNumeric("Global",c.blackLevel,-.5,.5,.01,-10,10,v->viewModel.update(x->x.blackLevel=v));rgb(card,"Canal",c.redBlackLevel,c.greenBlackLevel,c.blueBlackLevel,(x,v)->x.redBlackLevel=v,(x,v)->x.greenBlackLevel=v,(x,v)->x.blueBlackLevel=v,-.5,.5,-10,10);}
    private void addWhite(DisplayConfiguration c){EffectControlCard card=addEffect("Nível de branco","Ganho máximo linear com possibilidade de clipping.",c.whiteLevelEnabled,v->viewModel.update(x->x.whiteLevelEnabled=v),()->reset(x->{x.whiteLevel=x.redWhiteLevel=x.greenWhiteLevel=x.blueWhiteLevel=1;}));card.addNumeric("Global",c.whiteLevel,0,3,.01,-100,100,v->viewModel.update(x->x.whiteLevel=v));rgb(card,"Canal",c.redWhiteLevel,c.greenWhiteLevel,c.blueWhiteLevel,(x,v)->x.redWhiteLevel=v,(x,v)->x.greenWhiteLevel=v,(x,v)->x.blueWhiteLevel=v,0,3,-100,100);}
    private void addTemperature(DisplayConfiguration c,boolean compact){EffectControlCard card=addEffect("Temperatura de cor","Aproximação matemática sRGB; neutro inicial em 6500 K.",c.temperatureEnabled,v->viewModel.update(x->x.temperatureEnabled=v),()->reset(x->{x.temperatureKelvin=6500;x.neutralTemperatureKelvin=6500;x.temperatureManualRgb=false;}));card.addNumeric("Kelvin",c.temperatureKelvin,1000,12000,50,500,40000,v->viewModel.update(x->x.temperatureKelvin=v));String[] presets={"2000","2700","3400","4000","5000","5500","6500","7500","9300","12000"};card.addDropdown("Preset Kelvin",presets,String.format(Locale.US,"%.0f",c.temperatureKelvin),(p,v,pos,id)->viewModel.update(x->x.temperatureKelvin=Double.parseDouble(presets[pos])));if(!compact)card.addCheckBox("Modo RGB manual",c.temperatureManualRgb,(b,on)->viewModel.update(x->x.temperatureManualRgb=on));}
    private void addTint(DisplayConfiguration c){EffectControlCard card=addEffect("Tint verde–magenta","Valor positivo favorece verde; negativo favorece magenta.",c.tintEnabled,v->viewModel.update(x->x.tintEnabled=v),()->reset(x->x.tint=0));card.addNumeric("Tint",c.tint,-1,1,.01,-10,10,v->viewModel.update(x->x.tint=v));}
    private void addHue(DisplayConfiguration c){EffectControlCard card=addEffect("Matiz","Rotação linear no espaço RGB.",c.hueEnabled,v->viewModel.update(x->x.hueEnabled=v),()->reset(x->x.hueDegrees=0));card.addNumeric("Graus",c.hueDegrees,-180,180,1,-3600,3600,v->viewModel.update(x->x.hueDegrees=v));}
    private void addGrayscale(DisplayConfiguration c){EffectControlCard card=addEffect("Escala de cinza","Mistura por coeficientes de luminância selecionáveis.",c.grayscaleEnabled,v->viewModel.update(x->x.grayscaleEnabled=v),()->reset(x->{x.grayscaleIntensity=1;x.grayscaleCoefficients="REC709";}));card.addNumeric("Intensidade",c.grayscaleIntensity,0,1,.01,-10,10,v->viewModel.update(x->x.grayscaleIntensity=v));String[] modes={"REC601","REC709","REC2020","AVERAGE","CUSTOM"};card.addDropdown("Coeficientes",modes,c.grayscaleCoefficients,(p,v,pos,id)->viewModel.update(x->x.grayscaleCoefficients=modes[pos]));}
    private void addInversion(DisplayConfiguration c){EffectControlCard card=addEffect("Inversão","Negativo total ou por canal, com intensidade.",c.inversionEnabled,v->viewModel.update(x->x.inversionEnabled=v),()->reset(x->{x.inversionIntensity=1;x.invertRed=x.invertGreen=x.invertBlue=true;}));card.addNumeric("Intensidade",c.inversionIntensity,0,1,.01,-10,10,v->viewModel.update(x->x.inversionIntensity=v));card.addCheckBox("Vermelho",c.invertRed,(b,on)->viewModel.update(x->x.invertRed=on));card.addCheckBox("Verde",c.invertGreen,(b,on)->viewModel.update(x->x.invertGreen=on));card.addCheckBox("Azul",c.invertBlue,(b,on)->viewModel.update(x->x.invertBlue=on));}
    private void addFilter(DisplayConfiguration c){EffectControlCard card=addEffect("Filtros por matriz","Presets lineares; correções/simulações de daltonismo são aproximações.",c.filterEnabled,v->viewModel.update(x->x.filterEnabled=v),()->reset(x->{x.filterPreset="NONE";x.filterIntensity=1;}));String[] names=new String[FilterPreset.values().length];for(int i=0;i<names.length;i++)names[i]=FilterPreset.values()[i].name();card.addDropdown("Preset",names,c.filterPreset,(p,v,pos,id)->viewModel.update(x->x.filterPreset=names[pos]));card.addNumeric("Intensidade",c.filterIntensity,0,1,.01,-10,10,v->viewModel.update(x->x.filterIntensity=v));}

    private void addMixer(DisplayConfiguration c){EffectControlCard card=addEffect("Misturador de canais 3×3","Cada linha define um canal de saída.",c.channelMixerEnabled,v->viewModel.update(x->x.channelMixerEnabled=v),()->reset(x->x.channelMixer=DisplayConfiguration.identityArray()));String[] labels={"RR","RG","RB","GR","GG","GB","BR","BG","BB"};for(int i=0;i<9;i++){final int index=i;card.addNumeric(labels[i],c.channelMixer[i],-2,2,.01,-100,100,v->viewModel.update(x->x.channelMixer[index]=v));}card.addAction("Trocar vermelho e azul",v->reset(x->x.channelMixer=new double[]{0,0,1,0,1,0,1,0,0}));card.addAction("Trocar vermelho e verde",v->reset(x->x.channelMixer=new double[]{0,1,0,1,0,0,0,0,1}));card.addAction("Trocar verde e azul",v->reset(x->x.channelMixer=new double[]{1,0,0,0,0,1,0,1,0}));}

    private void renderDisplay(){cap("Brilho físico","Requer backend confirmado do sistema/fabricante.",CapabilityStatus.UNKNOWN);cap("Brilho automático / Extra Dim","Preparado; API e permissões variam por ROM.",CapabilityStatus.UNTESTED);cap("Taxa de atualização mínima/máxima/fixa","Modelo de capacidade disponível; backend não confirmado neste host.",CapabilityStatus.UNKNOWN);cap("Resolução e densidade DPI","Requer comandos wm e recuperação específica por dispositivo.",CapabilityStatus.EXPERIMENTAL);cap("Modos de cor / gamut / HDR","Detectados pelo backend SurfaceFlinger/fabricante quando disponíveis.",CapabilityStatus.UNTESTED);cap("HBM / DC dimming / PWM / MEMC","Nunca habilitado sem interface confirmada.",CapabilityStatus.UNKNOWN);}
    private void renderOled(){info("Esta seção só libera a interface; comandos de hardware continuam condicionados às capacidades detectadas.");cap("Proteção contra burn-in / pixel shift","Requer interface oficial ou vendor segura.",CapabilityStatus.UNKNOWN);cap("HBM, ABL e limite de brilho","Somente leitura/detecção até existir backend confirmado.",CapabilityStatus.UNKNOWN);cap("DC dimming / anti-flicker / PWM","Preparado para adaptadores de fabricante.",CapabilityStatus.UNKNOWN);cap("Black crush / baixa luminosidade","Ajuste linear disponível em Cor; correção real requer LUT.",CapabilityStatus.REQUIRES_MODULE);cap("Ciclos de compensação","Não são executados sem interface oficial confirmada.",CapabilityStatus.UNSUPPORTED);}
    private void renderLcd(){info("Brilho digital, brilho do sistema e backlight físico são recursos diferentes.");cap("Backlight físico / CABC / SRE","Requer sysfs ou serviço vendor confirmado e reversível.",CapabilityStatus.UNKNOWN);cap("Local dimming / uniformidade","Disponível apenas se o painel e backend expuserem suporte.",CapabilityStatus.UNKNOWN);cap("Overdrive / redução de ghosting","Nunca aplicado por tentativa e erro.",CapabilityStatus.UNKNOWN);cap("Black/white level por software","Disponível na aba Cor como transformação linear.",CapabilityStatus.SUPPORTED);}
    private void renderAccessibility(DisplayConfiguration c){addGrayscale(c);addInversion(c);addFilter(c);cap("Daltonizer nativo","Transação será usada apenas após resolução de capacidade.",CapabilityStatus.UNTESTED);}
    private void renderProfiles(DisplayUiState state){info("Perfis armazenam a configuração completa, tecnologia do painel, backend e metadados versionados.");cap("Perfis JSON versionados","Persistência atômica, presets e importação/exportação disponíveis.",CapabilityStatus.SUPPORTED);for(DisplayProfile profile:state.profiles){Button b=button((profile.id.equals(state.configuration.activeProfileId)?"✓ ":"")+profile.name);b.setOnClickListener(v->viewModel.applyProfile(profile.id));content.addView(b);if(!profile.builtIn){LinearLayout actions=new LinearLayout(requireContext());Button duplicate=button("Duplicar");duplicate.setOnClickListener(v->{viewModel.duplicateProfile(profile.id);requireActivity().recreate();});Button delete=button("Excluir");delete.setOnClickListener(v->{viewModel.deleteProfile(profile.id);requireActivity().recreate();});actions.addView(duplicate);actions.addView(delete);content.addView(actions);}}Button create=button("Criar perfil da configuração atual");create.setOnClickListener(v->{viewModel.createProfile("Personalizado "+System.currentTimeMillis());requireActivity().recreate();});content.addView(create);Button export=button("Copiar todos os perfis (JSON)");export.setOnClickListener(v->copy(viewModel.exportProfiles()));content.addView(export);Button importButton=button("Importar perfis da área de transferência");importButton.setOnClickListener(v->{String json=clipboard();if(json!=null&&viewModel.importProfiles(json)){toast("Perfis importados");requireActivity().recreate();}else toast("JSON de perfis inválido");});content.addView(importButton);}
    private void renderAutomation(){cap("Por aplicativo","A transformação será global quando o app-alvo estiver em primeiro plano.",CapabilityStatus.EXPERIMENTAL);cap("Horário e dia da semana","Modelo de regras preparado; agendamento completo pendente.",CapabilityStatus.EXPERIMENTAL);cap("Nascer/pôr do sol, sensor e bateria","Requer permissões e condições WorkManager.",CapabilityStatus.REQUIRES_MODULE);cap("HDR, jogo e vídeo","Depende de detecção confiável do fabricante/compositor.",CapabilityStatus.UNKNOWN);}
    private void renderCompatibility(DisplayUiState state){info("Backend ativo: "+state.backendName);cap("Matriz SurfaceFlinger 1015","Backend legado; precisa de teste funcional no aparelho.",CapabilityStatus.UNTESTED);cap("Saturação SurfaceFlinger 1022","Mantém o controle global atual.",CapabilityStatus.UNTESTED);cap("Gerenciamento de cor 1023","Mantém o comportamento atual.",CapabilityStatus.UNTESTED);cap("Gama / LUT 1D / LUT 3D","Não simuladas por matriz; exigem backend não linear.",CapabilityStatus.REQUIRES_MODULE);cap("Relatório TXT/JSON","Exportação JSON de estado disponível em Avançado.",CapabilityStatus.EXPERIMENTAL);}
    private void renderAdvanced(DisplayConfiguration c,DisplayUiState state){
        EffectControlCard custom=addEffect("Matriz personalizada 4×4","Editor column-major. A última linha deve permanecer 0,0,0,1.",c.customMatrixEnabled,v->viewModel.update(x->x.customMatrixEnabled=v),()->reset(x->x.customMatrix=com.xda.sa2ration.domain.Matrix4.identity().toArray()));for(int i=0;i<16;i++){final int index=i;custom.addNumeric("M["+i+"]",c.customMatrix[i],-4,4,.01,-1000,1000,v->viewModel.update(x->x.customMatrix[index]=v));}
        custom.addAction("Copiar matriz deste estágio",v->copy(join(c.customMatrix)));
        custom.addAction("Colar matriz",v->pasteMatrix());
        info("Matriz final composta:\n"+join(state.finalMatrix.toArray()));
        Button copyFinal=button("Copiar matriz final");copyFinal.setOnClickListener(v->copy(join(state.finalMatrix.toArray())));content.addView(copyFinal);
        Button export=button("Copiar configuração JSON");export.setOnClickListener(v->copy(viewModel.exportConfiguration()));content.addView(export);
        Button importButton=button("Importar configuração JSON da área de transferência");importButton.setOnClickListener(v->pasteConfiguration());content.addView(importButton);
        cap("Gama real e gama RGB","Interface preparada; matriz não implementa potência por pixel.",CapabilityStatus.REQUIRES_MODULE);cap("Curvas RGB e LUT 1D","Requer serviço/módulo complementar com LUT.",CapabilityStatus.REQUIRES_MODULE);cap("LUT 3D .cube","Parser/backend futuro; indisponível sem compositor auxiliar.",CapabilityStatus.REQUIRES_MODULE);cap("Nitidez, debanding, dithering, tone mapping HDR","Não lineares/espaciais; não são falsamente aproximados.",CapabilityStatus.REQUIRES_MODULE);
    }

    private void pasteMatrix(){String text=clipboard();if(text==null)return;String[] tokens=text.trim().split("[\\s,;]+",-1);if(tokens.length!=16){toast("A matriz precisa de 16 valores");return;}try{double[] m=new double[16];for(int i=0;i<16;i++)m[i]=Double.parseDouble(tokens[i]);viewModel.update(x->{x.customMatrix=m;x.customMatrixEnabled=true;});requireActivity().recreate();}catch(NumberFormatException e){toast("Matriz inválida");}}
    private void pasteConfiguration(){String text=clipboard();if(text!=null&&viewModel.importConfiguration(text)){toast("Configuração importada");requireActivity().recreate();}else toast("JSON inválido");}
    private String clipboard(){ClipboardManager manager=(ClipboardManager)requireContext().getSystemService(Context.CLIPBOARD_SERVICE);return manager.hasPrimaryClip()?String.valueOf(manager.getPrimaryClip().getItemAt(0).coerceToText(requireContext())):null;}
    private void copy(String text){((ClipboardManager)requireContext().getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("Sa2ration",text));toast("Copiado");}
    private String join(double[] values){StringBuilder b=new StringBuilder();for(int i=0;i<values.length;i++){if(i>0)b.append(i%4==0?'\n':' ');b.append(String.format(Locale.US,"%.6f",values[i]));}return b.toString();}

    private interface ChannelSetter{void set(DisplayConfiguration c,double value);}
    private void rgb(EffectControlCard card,String prefix,double r,double g,double b,ChannelSetter rs,ChannelSetter gs,ChannelSetter bs,double min,double max,double manualMin,double manualMax){card.addNumeric(prefix+" R",r,min,max,.01,manualMin,manualMax,v->viewModel.update(x->rs.set(x,v)));card.addNumeric(prefix+" G",g,min,max,.01,manualMin,manualMax,v->viewModel.update(x->gs.set(x,v)));card.addNumeric(prefix+" B",b,min,max,.01,manualMin,manualMax,v->viewModel.update(x->bs.set(x,v)));}
    private EffectControlCard addEffect(String title,String desc,boolean enabled,EffectControlCard.ToggleListener listener,Runnable reset){EffectControlCard card=new EffectControlCard(requireContext(),title,desc,enabled,listener,reset);content.addView(card);return card;}
    private void cap(String title,String desc,CapabilityStatus status){content.addView(new CapabilityCard(requireContext(),title,desc,status));}
    private void info(String text){TextView view=new TextView(requireContext());view.setText(text);view.setPadding(dp(8),dp(8),dp(8),dp(12));view.setTextSize(13);content.addView(view);}
    private void addTitle(String text){TextView title=new TextView(requireContext());title.setText(text);title.setTextSize(22);title.setPadding(dp(4),0,dp(4),dp(12));content.addView(title);}
    private Button button(String label){Button button=new Button(requireContext());button.setText(label);return button;}
    private void reset(DisplayViewModel.Mutation mutation){viewModel.update(mutation);requireActivity().recreate();}
    private void toast(String text){Toast.makeText(requireContext(),text,Toast.LENGTH_SHORT).show();}
    private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
}
