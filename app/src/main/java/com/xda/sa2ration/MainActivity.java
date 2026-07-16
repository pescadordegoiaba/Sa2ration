package com.xda.sa2ration;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Build;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.tabs.TabLayoutMediator;
import com.xda.sa2ration.databinding.ActivityMainBinding;
import com.xda.sa2ration.ui.DashboardCategory;
import com.xda.sa2ration.ui.DashboardPagerAdapter;
import com.xda.sa2ration.ui.DisplayUiState;
import com.xda.sa2ration.ui.DisplayViewModel;
import com.xda.sa2ration.ui.settings.SettingsActivity;

public final class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    private DisplayViewModel viewModel;
    private AlertDialog safetyDialog;
    private String lastMessage="";

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(),false);
        binding=ActivityMainBinding.inflate(getLayoutInflater());setContentView(binding.getRoot());setSupportActionBar(binding.toolbar);
        configureSystemBars();
        applyWindowInsets();
        binding.toolbar.setOnMenuItemClickListener(item->{
            if(item.getItemId()==R.id.action_settings){startActivity(new Intent(this,SettingsActivity.class));return true;}return false;
        });
        viewModel=new ViewModelProvider(this).get(DisplayViewModel.class);
        viewModel.state().observe(this,this::renderState);
    }

    private void renderState(DisplayUiState state){
        if(!state.loading&&binding.pager.getAdapter()==null){DashboardPagerAdapter adapter=new DashboardPagerAdapter(this,state.panel);binding.pager.setAdapter(adapter);binding.pager.setOffscreenPageLimit(1);new TabLayoutMediator(binding.tabs,binding.pager,(tab,position)->tab.setText(adapter.category(position).titleRes)).attach();}
        if(state.awaitingConfirmation)showSafetyDialog(state.countdownSeconds);else if(safetyDialog!=null){safetyDialog.dismiss();safetyDialog=null;}
        if(!state.message.isEmpty()&&!state.message.equals(lastMessage)&&!state.awaitingConfirmation){lastMessage=state.message;Toast.makeText(this,state.message,Toast.LENGTH_SHORT).show();}
    }

    private void showSafetyDialog(int seconds){
        String message=getResources().getQuantityString(R.plurals.temporary_application_message,seconds,seconds);
        if(safetyDialog==null){
            safetyDialog=new MaterialAlertDialogBuilder(this).setTitle(R.string.temporary_application_title).setMessage(message).setCancelable(false)
                    .setPositiveButton(R.string.keep_changes,(d,w)->viewModel.confirmTemporary())
                    .setNegativeButton(R.string.revert_changes,(d,w)->viewModel.revertTemporary()).create();safetyDialog.show();
        }else safetyDialog.setMessage(message);
    }

    private void configureSystemBars(){
        int mode=getResources().getConfiguration().uiMode&Configuration.UI_MODE_NIGHT_MASK;
        boolean light=mode!=Configuration.UI_MODE_NIGHT_YES;
        WindowCompat.getInsetsController(getWindow(),binding.root).setAppearanceLightStatusBars(light);
        WindowCompat.getInsetsController(getWindow(),binding.root).setAppearanceLightNavigationBars(light);
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.Q)getWindow().setNavigationBarContrastEnforced(false);
    }

    private void applyWindowInsets(){
        binding.pager.setClipToPadding(false);
        ViewCompat.setOnApplyWindowInsetsListener(binding.root,(view,insets)->{
            Insets bars=insets.getInsets(WindowInsetsCompat.Type.systemBars()|WindowInsetsCompat.Type.displayCutout());
            Insets ime=insets.getInsets(WindowInsetsCompat.Type.ime());
            binding.appbar.setPadding(bars.left,bars.top,bars.right,0);
            binding.pager.setPadding(bars.left,0,bars.right,Math.max(bars.bottom,ime.bottom));
            return insets;
        });
        ViewCompat.requestApplyInsets(binding.root);
    }
}
