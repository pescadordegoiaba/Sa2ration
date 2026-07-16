package com.xda.sa2ration.ui.settings;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.NestedScrollView;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.card.MaterialCardView;
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
        super.onCreate(savedInstanceState);setTitle("Configurações");getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        NestedScrollView scroll=new NestedScrollView(this);content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);content.setPadding(dp(12),dp(12),dp(12),dp(24));scroll.addView(content);setContentView(scroll);
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

        section("3. Backend","Ativo: "+state.backendName+"\nPreferido: "+state.selectedBackend+"\nSurfaceFlinger: "+(state.surfaceFlinger!=null&&state.surfaceFlinger.serviceAvailable?"disponível":"não confirmado"));
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
    private void section(String title,String text){MaterialCardView card=new MaterialCardView(this);card.setUseCompatPadding(true);LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(16),dp(12),dp(16),dp(12));TextView h=new TextView(this);h.setText(title);h.setTextSize(18);root.addView(h);TextView d=new TextView(this);d.setText(text);d.setTextSize(13);root.addView(d);card.addView(root);content.addView(card);}
    private void dropdown(String hint,String[] values,String selected,java.util.function.Consumer<String> listener){TextInputLayout layout=new TextInputLayout(this);layout.setHint(hint);MaterialAutoCompleteTextView input=new MaterialAutoCompleteTextView(this);input.setInputType(0);input.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_dropdown_item_1line,values));input.setText(selected,false);input.setOnItemClickListener((p,v,pos,id)->listener.accept(values[pos]));layout.addView(input);content.addView(layout);}
    private void action(String text,android.view.View.OnClickListener listener){Button b=new Button(this);b.setText(text);b.setOnClickListener(listener);content.addView(b);}
    private void info(String text){TextView v=new TextView(this);v.setText(text);v.setPadding(dp(8),dp(8),dp(8),dp(8));content.addView(v);}
    private void copy(String text){((ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("Sa2ration diagnóstico",text));Toast.makeText(this,"Diagnóstico copiado",Toast.LENGTH_SHORT).show();}
    private void write(Uri uri,String text){try(OutputStream out=getContentResolver().openOutputStream(uri)){if(out==null)throw new IllegalStateException("Sem stream");out.write(text.getBytes(StandardCharsets.UTF_8));Toast.makeText(this,"Arquivo exportado",Toast.LENGTH_SHORT).show();}catch(Exception e){Toast.makeText(this,"Falha: "+e.getMessage(),Toast.LENGTH_LONG).show();}}
    private String yes(boolean value){return value?"Sim":"Não";}private String empty(String value){return value==null||value.isEmpty()?"não informado":value;}private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
}
