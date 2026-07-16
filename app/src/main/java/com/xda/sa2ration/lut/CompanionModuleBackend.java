package com.xda.sa2ration.lut;

import android.content.Context;
import com.xda.sa2ration.backend.CapabilityStatus;
import com.xda.sa2ration.shell.RootShellExecutor;
import com.xda.sa2ration.shell.ShellCommand;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Locale;

public final class CompanionModuleBackend implements NonLinearDisplayBackend {
    private final Context context;private final RootShellExecutor executor;private CompanionModuleStatus status;
    public CompanionModuleBackend(Context context,RootShellExecutor executor){this.context=context.getApplicationContext();this.executor=executor;status=new CompanionModuleDetector(executor).detect();}
    public void refresh(){status=new CompanionModuleDetector(executor).detect();}
    public CompanionModuleStatus status(){return status;}
    public CapabilityStatus gammaStatus(){return status.gamma;}public CapabilityStatus lut1dStatus(){return status.lut1d;}public CapabilityStatus lut3dStatus(){return status.lut3d;}
    public String unavailableReason(){if(!status.installed)return"Módulo Sa2ration Companion não instalado.";if(!status.hasAdapter())return"Módulo instalado sem adaptador compatível.";return"Operação não declarada pelo adaptador "+status.adapterId;}
    @Override public boolean apply(Lut1D lut){if(status.lut1d!=CapabilityStatus.SUPPORTED&&status.lut1d!=CapabilityStatus.EXPERIMENTAL)return false;File file=new File(context.getCacheDir(),"sa2ration-lut1d.txt");try(BufferedWriter out=new BufferedWriter(new FileWriter(file))){out.write("SA2RATION_LUT1D "+lut.size());out.newLine();for(int i=0;i<lut.size();i++){out.write(String.format(Locale.US,"%.9f %.9f %.9f",lut.red[i],lut.green[i],lut.blue[i]));out.newLine();}}catch(IOException error){return false;}boolean ok=executor.executeBlocking(ShellCommand.builder(CompanionModuleDetector.CLI+" apply-lut1d "+quote(file.getAbsolutePath())).timeoutMs(30_000).build()).isSuccess();file.delete();return ok;}
    @Override public boolean apply(CubeLut3D lut,double intensity){if((status.lut3d!=CapabilityStatus.SUPPORTED&&status.lut3d!=CapabilityStatus.EXPERIMENTAL)||!Double.isFinite(intensity)||intensity<0||intensity>1)return false;File file=new File(context.getCacheDir(),"sa2ration-lut3d.cube");try(BufferedWriter out=new BufferedWriter(new FileWriter(file))){out.write("TITLE \"Sa2ration\"");out.newLine();out.write("LUT_3D_SIZE "+lut.size);out.newLine();for(double[]value:lut.values){out.write(String.format(Locale.US,"%.9f %.9f %.9f",value[0],value[1],value[2]));out.newLine();}}catch(IOException error){return false;}boolean ok=executor.executeBlocking(ShellCommand.builder(CompanionModuleDetector.CLI+" apply-lut3d "+quote(file.getAbsolutePath())+" "+String.format(Locale.US,"%.6f",intensity)).timeoutMs(60_000).build()).isSuccess();file.delete();return ok;}
    public boolean reset(){return status.installed&&executor.executeBlocking(ShellCommand.builder(CompanionModuleDetector.CLI+" reset").timeoutMs(15_000).build()).isSuccess();}
    private String quote(String value){return"'"+value.replace("'","'\\''")+"'";}
}
