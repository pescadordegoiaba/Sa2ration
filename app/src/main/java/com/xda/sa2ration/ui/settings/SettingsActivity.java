package com.xda.sa2ration.ui.settings;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.content.res.Configuration;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.NestedScrollView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputLayout;
import com.google.gson.GsonBuilder;
import com.xda.sa2ration.diagnostics.DetectionEvidence;
import com.xda.sa2ration.persistence.ConfigurationRepository;
import com.xda.sa2ration.ui.DisplayUiState;
import com.xda.sa2ration.ui.DisplayViewModel;
import com.xda.sa2ration.R;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

public final class SettingsActivity extends AppCompatActivity {
    private LinearLayout content;private DisplayViewModel viewModel;private DisplayUiState latest;private boolean rendered;
    private final ActivityResultLauncher<String> exportBackupJson=registerForActivityResult(new ActivityResultContracts.CreateDocument("application/json"),uri->{if(uri!=null)write(uri,ConfigurationRepository.getInstance(this).exportJson());});
    private final ActivityResultLauncher<String> exportDiagnosticJson=registerForActivityResult(new ActivityResultContracts.CreateDocument("application/json"),uri->{if(uri!=null)write(uri,diagnosticJson(latest));});
    private final ActivityResultLauncher<String> exportText=registerForActivityResult(new ActivityResultContracts.CreateDocument("text/plain"),uri->{if(uri!=null)write(uri,diagnosticText(latest));});

    @Override protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);WindowCompat.setDecorFitsSystemWindows(getWindow(),false);
        LinearLayout page=new LinearLayout(this);page.setOrientation(LinearLayout.VERTICAL);page.setBackgroundColor(MaterialColors.getColor(page,com.google.android.material.R.attr.colorSurface,0xfffafafa));
        MaterialToolbar toolbar=new MaterialToolbar(this);toolbar.setTitle(R.string.settings_title);toolbar.setNavigationIcon(R.drawable.ic_arrow_back);toolbar.setNavigationContentDescription(R.string.back);toolbar.setNavigationOnClickListener(v->finish());toolbar.setBackgroundColor(MaterialColors.getColor(toolbar,com.google.android.material.R.attr.colorSurface,0xfffafafa));toolbar.setMinimumHeight(dp(64));page.addView(toolbar,new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT));
        NestedScrollView scroll=new NestedScrollView(this);scroll.setFillViewport(true);scroll.setClipToPadding(false);content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);int base=getResources().getDimensionPixelSize(R.dimen.screen_horizontal_padding);int centered=dp(Math.max(0,(getResources().getConfiguration().screenWidthDp-840)/2));int horizontal=Math.max(base,centered);int vertical=getResources().getDimensionPixelSize(R.dimen.screen_vertical_padding);content.setPadding(horizontal,vertical,horizontal,vertical+dp(16));scroll.addView(content,new NestedScrollView.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT));page.addView(scroll,new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,0,1));setContentView(page);
        int mode=getResources().getConfiguration().uiMode&Configuration.UI_MODE_NIGHT_MASK;boolean light=mode!=Configuration.UI_MODE_NIGHT_YES;WindowCompat.getInsetsController(getWindow(),page).setAppearanceLightStatusBars(light);WindowCompat.getInsetsController(getWindow(),page).setAppearanceLightNavigationBars(light);
        ViewCompat.setOnApplyWindowInsetsListener(page,(view,insets)->{Insets bars=insets.getInsets(WindowInsetsCompat.Type.systemBars()|WindowInsetsCompat.Type.displayCutout());Insets ime=insets.getInsets(WindowInsetsCompat.Type.ime());toolbar.setPadding(bars.left,bars.top,bars.right,0);scroll.setPadding(bars.left,0,bars.right,Math.max(bars.bottom,ime.bottom));return insets;});ViewCompat.requestApplyInsets(page);
        TextView loading=new TextView(this);loading.setText(R.string.detecting_environment);content.addView(loading);
        viewModel=new ViewModelProvider(this).get(DisplayViewModel.class);viewModel.state().observe(this,state->{latest=state;if(!state.loading&&!rendered){rendered=true;render(state);}});
    }

    @Override public boolean onSupportNavigateUp(){finish();return true;}

    private void render(DisplayUiState state){content.removeAllViews();
        section("1. Root","Implementação: "+state.root.implementation+"\nArquitetura: "+state.root.architecture+"\nFuncional (id -u = 0): "+yes(state.root.functionalRoot)+"\nConfiança: "+state.root.confidence+"%\nVersão: "+empty(state.root.versionName)+"\nBinário: "+empty(state.root.binaryPath));
        dropdown("Seleção manual de root",new String[]{"AUTO","MAGISK","KERNELSU","SUKISU_ULTRA","RESUKISU","KERNELSU_FORK","APATCH","OTHER"},state.selectedRoot,v->viewModel.selectRoot(v));
        action("Detectar novamente",v->{rendered=false;content.removeAllViews();viewModel.redetect();});action("Testar acesso root",v->{rendered=false;content.removeAllViews();viewModel.redetect();});
        action("Mostrar diagnóstico completo",v->copy(diagnosticText(state)));

        StringBuilder panel=new StringBuilder("Tecnologia detectada: ").append(state.panel.detectedTechnology).append("\nTecnologia efetiva: ").append(state.panel.effectiveTechnology).append("\nConfiança: ").append(state.panel.confidence).append("%\nNome: ").append(state.panel.panelName).append("\nFabricante: ").append(state.panel.manufacturer).append("\nBackend provável: ").append(state.panel.probableBackend);
        if(state.panel.manualConflict)panel.append("\nAVISO: seleção manual diverge das evidências.");section("2. Painel",panel.toString());
        dropdown("Seleção do painel",new String[]{"AUTO","LCD","TFT_LCD","IPS_LCD","PLS_LCD","LTPS_LCD","LTPO_LCD","OLED","POLED","AMOLED","SUPER_AMOLED","DYNAMIC_AMOLED","LTPO_OLED","MINI_LED","OTHER","UNKNOWN"},state.selectedPanel,v->viewModel.selectPanel(v));

        section("3. Backend","Ativo: "+state.backendName+"\nPreferido: "+state.selectedBackend+"\nSurfaceFlinger: "+(state.surfaceFlinger!=null&&state.surfaceFlinger.serviceAvailable?"disponível":"não confirmado")+"\nCompanion: "+(state.companionModule.installed?"instalado "+state.companionModule.version:"não instalado")+"\nAdaptador não linear: "+state.companionModule.adapterId+"\nGama: "+state.companionModule.gamma+" · LUT 1D: "+state.companionModule.lut1d+" · LUT 3D: "+state.companionModule.lut3d);
        dropdown("Backend preferido",new String[]{"AUTO","SURFACEFLINGER","GENERIC_AOSP","SAMSUNG","QUALCOMM","MEDIATEK","EXYNOS","XIAOMI","ONEPLUS","OPPO","REALME","MOTOROLA","PIXEL"},state.selectedBackend,v->viewModel.selectBackend(v));

        section("4. Segurança","Alterações extremas usam confirmação de 15 segundos. A configuração estável é separada da configuração em edição.");
        action("Restaurar display neutro",v->viewModel.resetAll());
        info("Modo de segurança no boot: crie /data/adb/sa2ration/disable ou defina persist.sa2ration.safe_mode=1. O app nunca grava em sysfs desconhecido.");

        section("5. Persistência","Estado atual e estável são gravados transacionalmente em AndroidX DataStore. Perfis usam JSON versionado.");
        SwitchMaterial restore=new SwitchMaterial(this);restore.setText(R.string.restore_stable_boot);restore.setChecked(state.configuration.restoreAtBoot);restore.setOnCheckedChangeListener((b,on)->viewModel.update(x->x.restoreAtBoot=on));content.addView(restore);
        action("Exportar backup JSON",v->exportBackupJson.launch("sa2ration-backup.json"));
        section("6. Inicialização","BOOT_COMPLETED, USER_UNLOCKED e MY_PACKAGE_REPLACED agendam trabalho único pelo WorkManager. Após 3 falhas consecutivas, o boot não restaura efeitos.");
        section("7. Diagnóstico",diagnosticText(state));action("Exportar diagnóstico TXT",v->exportText.launch("sa2ration-diagnostic.txt"));action("Exportar diagnóstico JSON",v->exportDiagnosticJson.launch("sa2ration-diagnostic.json"));
        section("8. Logs","Comandos root registram somente comando seguro/identificador, exit code, timeout e duração. Entradas marcadas como sensíveis são redigidas.");
        section("9. Sobre","Sa2ration Advanced\nPipeline linear modular para Android 15 com root. Recursos sem backend real permanecem indisponíveis ou experimentais.");
    }

    private String diagnosticText(DisplayUiState s){if(s==null)return"Sem diagnóstico";StringBuilder b=new StringBuilder("SA2RATION DIAGNOSTIC\n\nROOT\nimplementation=").append(s.root.implementation).append("\narchitecture=").append(s.root.architecture).append("\nfunctional=").append(s.root.functionalRoot).append("\nconfidence=").append(s.root.confidence).append("\nversion=").append(s.root.versionName).append("\nexitCode=").append(s.root.exitCode).append("\ntimeout=").append(s.root.timedOut).append("\ndurationMs=").append(s.root.durationMs).append("\n");for(DetectionEvidence e:s.root.evidence())b.append("evidence: ").append(e.source).append(" - ").append(e.detail).append("\n");b.append("\nPANEL\ndetected=").append(s.panel.detectedTechnology).append("\neffective=").append(s.panel.effectiveTechnology).append("\nconfidence=").append(s.panel.confidence).append("\nname=").append(s.panel.panelName).append("\nmanufacturer=").append(s.panel.manufacturer).append("\nbackend=").append(s.panel.probableBackend).append("\n");for(DetectionEvidence e:s.panel.evidence())b.append("evidence: ").append(e.source).append(" - ").append(e.detail).append("\n");b.append("\nSURFACEFLINGER\navailable=").append(s.surfaceFlinger!=null&&s.surfaceFlinger.serviceAvailable).append("\nbackend=").append(s.backendName).append("\n");return b.toString();}
    private String diagnosticJson(DisplayUiState s){if(s==null)return"{}";Map<String,Object> root=new LinkedHashMap<>();root.put("implementation",s.root.implementation);root.put("architecture",s.root.architecture);root.put("functionalRoot",s.root.functionalRoot);root.put("versionName",s.root.versionName);root.put("versionCode",s.root.versionCode);root.put("confidence",s.root.confidence);root.put("binaryPath",s.root.binaryPath);root.put("exitCode",s.root.exitCode);root.put("timedOut",s.root.timedOut);root.put("durationMs",s.root.durationMs);root.put("evidence",s.root.evidence());root.put("warnings",s.root.warnings());Map<String,Object> panel=new LinkedHashMap<>();panel.put("detectedTechnology",s.panel.detectedTechnology);panel.put("effectiveTechnology",s.panel.effectiveTechnology);panel.put("confidence",s.panel.confidence);panel.put("panelName",s.panel.panelName);panel.put("manufacturer",s.panel.manufacturer);panel.put("probableBackend",s.panel.probableBackend);panel.put("manualOverride",s.panel.manuallyOverridden);panel.put("manualConflict",s.panel.manualConflict);panel.put("evidence",s.panel.evidence());Map<String,Object> surface=new LinkedHashMap<>();surface.put("available",s.surfaceFlinger!=null&&s.surfaceFlinger.serviceAvailable);surface.put("backend",s.backendName);if(s.surfaceFlinger!=null){surface.put("transactions",s.surfaceFlinger.transactions);surface.put("capabilities",s.surfaceFlinger.capabilities.asMap());surface.put("diagnostic",s.surfaceFlinger.diagnostic);}Map<String,Object> report=new LinkedHashMap<>();report.put("schemaVersion",1);report.put("generatedAtEpochMs",System.currentTimeMillis());report.put("root",root);report.put("panel",panel);report.put("surfaceFlinger",surface);return new GsonBuilder().setPrettyPrinting().create().toJson(report);}
    private void section(String title,String text){MaterialCardView card=new MaterialCardView(this);card.setRadius(dp(20));card.setCardElevation(dp(1));card.setStrokeWidth(dp(1));card.setStrokeColor(MaterialColors.getColor(card,com.google.android.material.R.attr.colorOutline,0x33000000));card.setCardBackgroundColor(MaterialColors.getColor(card,com.google.android.material.R.attr.colorSurfaceContainer,0xffffffff));LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);int padding=getResources().getDimensionPixelSize(R.dimen.card_content_padding);root.setPadding(padding,padding,padding,padding);TextView h=new TextView(this);h.setText(title);h.setTextSize(18);h.setTextColor(MaterialColors.getColor(h,com.google.android.material.R.attr.colorOnSurface,0xff111111));root.addView(h);TextView d=new TextView(this);d.setText(text);d.setTextSize(13);d.setTextColor(MaterialColors.getColor(d,com.google.android.material.R.attr.colorOnSurfaceVariant,0xff555555));d.setPadding(0,dp(6),0,0);root.addView(d);card.addView(root);LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT);params.setMargins(0,0,0,getResources().getDimensionPixelSize(R.dimen.card_spacing));content.addView(card,params);}
    private void dropdown(String hint,String[] values,String selected,java.util.function.Consumer<String> listener){TextInputLayout layout=new TextInputLayout(this,null,com.google.android.material.R.attr.textInputOutlinedStyle);layout.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE);layout.setHint(hint);MaterialAutoCompleteTextView input=new MaterialAutoCompleteTextView(layout.getContext());input.setInputType(0);input.setMinHeight(dp(48));input.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_dropdown_item_1line,values));input.setText(selected,false);input.setOnItemClickListener((p,v,pos,id)->listener.accept(values[pos]));layout.addView(input);LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT);params.setMargins(0,dp(4),0,dp(8));content.addView(layout,params);}
    private void action(String text,android.view.View.OnClickListener listener){MaterialButton b=new MaterialButton(this);b.setText(text);b.setMinHeight(dp(48));b.setMaxLines(2);b.setOnClickListener(listener);LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT);params.setMargins(0,dp(3),0,dp(3));content.addView(b,params);}
    private void info(String text){TextView v=new TextView(this);v.setText(text);v.setTextSize(13);v.setTextColor(MaterialColors.getColor(v,com.google.android.material.R.attr.colorOnSurfaceVariant,0xff555555));v.setPadding(dp(8),dp(8),dp(8),dp(12));content.addView(v);}
    private void copy(String text){((ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("Sa2ration diagnóstico",text));Toast.makeText(this,"Diagnóstico copiado",Toast.LENGTH_SHORT).show();}
    private void write(Uri uri,String text){try(OutputStream out=getContentResolver().openOutputStream(uri)){if(out==null)throw new IllegalStateException("Sem stream");out.write(text.getBytes(StandardCharsets.UTF_8));Toast.makeText(this,"Arquivo exportado",Toast.LENGTH_SHORT).show();}catch(Exception e){Toast.makeText(this,"Falha: "+e.getMessage(),Toast.LENGTH_LONG).show();}}
    private String yes(boolean value){return value?"Sim":"Não";}private String empty(String value){return value==null||value.isEmpty()?"não informado":value;}private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
}
