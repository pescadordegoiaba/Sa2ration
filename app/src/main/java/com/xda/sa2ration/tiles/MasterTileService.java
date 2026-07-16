package com.xda.sa2ration.tiles;

import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.os.Build;

import com.xda.sa2ration.backend.SurfaceFlingerBackend;
import com.xda.sa2ration.domain.DefaultTransformStages;
import com.xda.sa2ration.domain.DisplayConfiguration;
import com.xda.sa2ration.persistence.ConfigurationRepository;
import com.xda.sa2ration.shell.RootShellExecutor;

public final class MasterTileService extends TileService {
    @Override public void onStartListening(){super.onStartListening();new Thread(this::refresh,"sa2ration-tile-state").start();}
    @Override public void onClick(){super.onClick();new Thread(()->{ConfigurationRepository repository=ConfigurationRepository.getInstance(this);DisplayConfiguration c=repository.load().stable.copy();c.masterEnabled=!c.masterEnabled;try(RootShellExecutor executor=new RootShellExecutor()){if(new SurfaceFlingerBackend(executor).apply(c,DefaultTransformStages.createPipeline().compose(c)).isSuccess())repository.confirmStable(c);}refresh();},"sa2ration-tile-toggle").start();}
    private void refresh(){Tile tile=getQsTile();if(tile==null)return;boolean enabled=ConfigurationRepository.getInstance(this).load().stable.masterEnabled;tile.setState(enabled?Tile.STATE_ACTIVE:Tile.STATE_INACTIVE);if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.Q)tile.setSubtitle(enabled?"Ativo":"Neutro");tile.updateTile();}
}
