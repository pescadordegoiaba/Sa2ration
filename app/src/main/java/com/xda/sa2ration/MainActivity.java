package com.xda.sa2ration;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

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
        binding=ActivityMainBinding.inflate(getLayoutInflater());setContentView(binding.getRoot());setSupportActionBar(binding.toolbar);
        binding.toolbar.setOnMenuItemClickListener(item->{
            if(item.getItemId()==R.id.action_settings){startActivity(new Intent(this,SettingsActivity.class));return true;}return false;
        });
        viewModel=new ViewModelProvider(this).get(DisplayViewModel.class);
        viewModel.state().observe(this,this::renderState);
    }

    private void renderState(DisplayUiState state){
        if(!state.loading&&binding.pager.getAdapter()==null){DashboardPagerAdapter adapter=new DashboardPagerAdapter(this,state.panel);binding.pager.setAdapter(adapter);binding.pager.setOffscreenPageLimit(1);new TabLayoutMediator(binding.tabs,binding.pager,(tab,position)->tab.setText(adapter.category(position).title)).attach();}
        if(state.awaitingConfirmation)showSafetyDialog(state.countdownSeconds);else if(safetyDialog!=null){safetyDialog.dismiss();safetyDialog=null;}
        if(!state.message.isEmpty()&&!state.message.equals(lastMessage)&&!state.awaitingConfirmation){lastMessage=state.message;Toast.makeText(this,state.message,Toast.LENGTH_SHORT).show();}
    }

    private void showSafetyDialog(int seconds){
        String message="A configuração é extrema. Reversão automática em "+seconds+" segundos.";
        if(safetyDialog==null){
            safetyDialog=new AlertDialog.Builder(this).setTitle("Aplicação temporária").setMessage(message).setCancelable(false)
                    .setPositiveButton("Manter",(d,w)->viewModel.confirmTemporary())
                    .setNegativeButton("Reverter",(d,w)->viewModel.revertTemporary()).create();safetyDialog.show();
        }else safetyDialog.setMessage(message);
    }
}
