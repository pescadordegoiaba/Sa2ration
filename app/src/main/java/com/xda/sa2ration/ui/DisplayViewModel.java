package com.xda.sa2ration.ui;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.xda.sa2ration.backend.SurfaceFlingerBackend;
import com.xda.sa2ration.backend.SurfaceFlingerCapabilities;
import com.xda.sa2ration.domain.ColorTransformPipeline;
import com.xda.sa2ration.domain.DefaultTransformStages;
import com.xda.sa2ration.domain.DisplayConfiguration;
import com.xda.sa2ration.domain.Matrix4;
import com.xda.sa2ration.persistence.ConfigurationRepository;
import com.xda.sa2ration.domain.StoredApplicationState;
import com.xda.sa2ration.panel.DisplayPanelDetector;
import com.xda.sa2ration.panel.DisplayPanelInfo;
import com.xda.sa2ration.root.RootDetectionResult;
import com.xda.sa2ration.root.RootEnvironmentDetector;
import com.xda.sa2ration.recovery.DisplayApplyCoordinator;
import com.xda.sa2ration.shell.RootShellExecutor;
import com.xda.sa2ration.shell.ShellResult;
import com.xda.sa2ration.profile.DisplayProfile;
import com.xda.sa2ration.profile.ProfileRepository;

import java.util.ArrayList;
import java.util.List;
import com.xda.sa2ration.lut.CompanionModuleDetector;
import com.xda.sa2ration.lut.CompanionModuleStatus;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class DisplayViewModel extends AndroidViewModel {
    public interface Mutation { void apply(DisplayConfiguration configuration); }

    private final MutableLiveData<DisplayUiState> state = new MutableLiveData<>(DisplayUiState.loading());
    private final ExecutorService io = Executors.newSingleThreadExecutor(r -> new Thread(r, "sa2ration-viewmodel"));
    private final RootShellExecutor rootExecutor = new RootShellExecutor();
    private final ColorTransformPipeline pipeline = DefaultTransformStages.createPipeline();
    private final ConfigurationRepository repository;
    private final DisplayApplyCoordinator applyCoordinator;
    private final ProfileRepository profileRepository;
    private volatile List<DisplayProfile> profiles=new ArrayList<>();
    private volatile CompanionModuleStatus companionModule=new CompanionModuleStatus();
    private volatile RootDetectionResult rootDetection=new RootDetectionResult();
    private volatile DisplayPanelInfo panelInfo=new DisplayPanelInfo();
    private volatile SurfaceFlingerCapabilities surfaceFlingerCapabilities;
    private volatile String selectedRoot="AUTO",selectedPanel="AUTO",selectedBackend="AUTO";

    public DisplayViewModel(@NonNull Application application) {
        super(application);
        repository = ConfigurationRepository.getInstance(application);
        profileRepository = ProfileRepository.getInstance(application);
        SurfaceFlingerBackend backend = new SurfaceFlingerBackend(rootExecutor);
        applyCoordinator = new DisplayApplyCoordinator(backend, pipeline, repository);
        io.execute(() -> {
            StoredApplicationState stored=repository.load();
            profiles=profileRepository.load();
            selectedRoot=stored.selectedRootImplementation;selectedPanel=stored.selectedPanelTechnology;selectedBackend=stored.selectedBackend;
            rootDetection=new RootEnvironmentDetector(rootExecutor).detect();
            companionModule=new CompanionModuleDetector(rootExecutor).detect();
            panelInfo=new DisplayPanelDetector(application,rootExecutor).detect(selectedPanel);
            surfaceFlingerCapabilities=SurfaceFlingerCapabilities.detect(rootExecutor);
            DisplayConfiguration configuration = stored.current.copy();
            post(configuration, false, false, 0, "");
        });
    }

    public LiveData<DisplayUiState> state() { return state; }

    public void update(Mutation mutation) {
        DisplayUiState current = state.getValue();
        if (current == null || current.loading) return;
        DisplayConfiguration configuration = current.configuration.copy();
        mutation.apply(configuration);
        configuration.sanitize();
        post(configuration, false, current.awaitingConfirmation, current.countdownSeconds, "");
        io.execute(() -> repository.saveCurrent(configuration));
    }

    public void apply() {
        DisplayUiState current=state.getValue(); if(current==null||current.loading)return;
        DisplayConfiguration configuration=current.configuration.copy();
        post(configuration,true,false,0,"Aplicando…");
        io.execute(() -> applyCoordinator.apply(configuration,true,new CoordinatorListener(configuration)));
    }

    public void confirmTemporary() {
        io.execute(() -> {
            boolean confirmed=applyCoordinator.confirm();
            DisplayUiState current=state.getValue();
            if(current!=null)post(current.configuration,false,false,0,confirmed?"Configuração confirmada":"Nada para confirmar");
        });
    }

    public void revertTemporary() { io.execute(() -> applyCoordinator.revert("Revertido pelo usuário")); }

    public void resetAll() {
        post(DisplayConfiguration.neutral(),true,false,0,"Restaurando estado neutro…");
        io.execute(() -> {
            ShellResult result=applyCoordinator.resetAtomically();
            post(DisplayConfiguration.neutral(),false,false,0,result.isSuccess()?"Display restaurado":"Falha: "+result.stderr);
        });
    }

    public void previewNeutral() { io.execute(() -> applyCoordinator.preview(DisplayConfiguration.neutral())); }
    public void previewCurrent() {
        DisplayUiState current=state.getValue();
        if(current!=null)io.execute(() -> applyCoordinator.preview(current.configuration));
    }

    public String exportConfiguration() { return repository.exportJson(); }
    public boolean importConfiguration(String json) {
        boolean result=repository.importJson(json);
        if(result){DisplayConfiguration c=repository.load().current.copy();post(c,false,false,0,"Configuração importada");}
        return result;
    }

    public void applyProfile(String id) {
        io.execute(() -> {
            DisplayProfile selected=null;
            for(DisplayProfile profile:profiles) if(profile.id.equals(id)){selected=profile;break;}
            if(selected==null)return;
            DisplayConfiguration candidate=selected.configuration.copy();
            candidate.activeProfileId=selected.id;
            repository.saveCurrent(candidate);
            post(candidate,true,false,0,"Aplicando perfil "+selected.name+"…");
            applyCoordinator.apply(candidate,true,new CoordinatorListener(candidate));
        });
    }

    public void createProfile(String name) {
        DisplayUiState current=state.getValue();if(current==null||current.loading)return;
        DisplayConfiguration source=current.configuration.copy();
        io.execute(()->{DisplayProfile created=profileRepository.create(name,source);profiles=profileRepository.load();DisplayConfiguration active=created.configuration.copy();repository.saveCurrent(active);post(active,false,false,0,"Perfil criado: "+created.name);});
    }

    public void duplicateProfile(String id){io.execute(()->{DisplayProfile created=profileRepository.duplicate(id);profiles=profileRepository.load();DisplayUiState current=state.getValue();if(current!=null)post(current.configuration,false,false,0,created==null?"Perfil não encontrado":"Perfil duplicado");});}
    public void deleteProfile(String id){io.execute(()->{boolean ok=profileRepository.delete(id);profiles=profileRepository.load();DisplayUiState current=state.getValue();if(current!=null)post(current.configuration,false,false,0,ok?"Perfil excluído":"Perfil padrão não pode ser excluído");});}
    public String exportProfiles(){return profileRepository.exportJson();}
    public boolean importProfiles(String json){boolean ok=profileRepository.importJson(json);if(ok){profiles=profileRepository.load();DisplayUiState current=state.getValue();if(current!=null)post(current.configuration,false,false,0,"Perfis importados");}return ok;}

    public void redetect(){
        io.execute(()->{rootDetection=new RootEnvironmentDetector(rootExecutor).detect();companionModule=new CompanionModuleDetector(rootExecutor).detect();panelInfo=new DisplayPanelDetector(getApplication(),rootExecutor).detect(selectedPanel);surfaceFlingerCapabilities=SurfaceFlingerCapabilities.detect(rootExecutor);DisplayUiState current=state.getValue();if(current!=null)post(current.configuration,false,false,0,"Detecção concluída");});
    }

    public void selectPanel(String selection){selectedPanel=selection;io.execute(()->{StoredApplicationState s=repository.load();s.selectedPanelTechnology=selection;repository.save(s);panelInfo=new DisplayPanelDetector(getApplication(),rootExecutor).detect(selection);DisplayUiState current=state.getValue();if(current!=null)post(current.configuration,false,false,0,"Painel selecionado: "+selection);});}
    public void selectRoot(String selection){selectedRoot=selection;io.execute(()->{StoredApplicationState s=repository.load();s.selectedRootImplementation=selection;repository.save(s);DisplayUiState current=state.getValue();if(current!=null)post(current.configuration,false,false,0,"Preferência de root salva; acesso funcional continua verificado por id -u");});}
    public void selectBackend(String selection){selectedBackend=selection;io.execute(()->{StoredApplicationState s=repository.load();s.selectedBackend=selection;repository.save(s);DisplayUiState current=state.getValue();if(current!=null)post(current.configuration,false,false,0,"Backend preferido salvo");});}

    private void post(DisplayConfiguration configuration, boolean applying, boolean awaiting, int seconds, String message) {
        Matrix4 matrix;
        try { matrix=pipeline.compose(configuration); }
        catch(RuntimeException error){ matrix=Matrix4.identity(); message="Matriz inválida: "+error.getMessage(); }
        state.postValue(new DisplayUiState(configuration,matrix,false,applying,awaiting,seconds,
                "SurfaceFlinger 1015/1022",message,rootDetection,panelInfo,surfaceFlingerCapabilities,
                selectedRoot,selectedPanel,selectedBackend,new ArrayList<>(profiles),companionModule));
    }

    private final class CoordinatorListener implements DisplayApplyCoordinator.Listener {
        private final DisplayConfiguration configuration;
        CoordinatorListener(DisplayConfiguration configuration){this.configuration=configuration;}
        @Override public void onApplied(ShellResult result, boolean needsConfirmation) {
            post(configuration,false,needsConfirmation,needsConfirmation?DisplayApplyCoordinator.ROLLBACK_SECONDS:0,
                    result.isSuccess()?(needsConfirmation?"Confirme a configuração temporária":"Configuração aplicada"):
                            "Falha ao aplicar: "+result.stderr);
        }
        @Override public void onCountdown(int secondsRemaining) {
            post(configuration,false,true,secondsRemaining,"Reversão automática em "+secondsRemaining+" s");
        }
        @Override public void onReverted(ShellResult result, String reason) {
            DisplayConfiguration stable=repository.load().stable.copy();
            post(stable,false,false,0,reason+(result.isSuccess()?"":" (falha no comando)"));
        }
    }

    @Override protected void onCleared() {
        applyCoordinator.close(); rootExecutor.close(); io.shutdownNow(); super.onCleared();
    }
}
