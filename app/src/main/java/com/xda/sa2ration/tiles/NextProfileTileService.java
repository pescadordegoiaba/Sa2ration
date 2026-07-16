package com.xda.sa2ration.tiles;

import android.service.quicksettings.TileService;
import com.xda.sa2ration.backend.SurfaceFlingerBackend;
import com.xda.sa2ration.domain.DefaultTransformStages;
import com.xda.sa2ration.domain.DisplayConfiguration;
import com.xda.sa2ration.persistence.ConfigurationRepository;
import com.xda.sa2ration.profile.DisplayProfile;
import com.xda.sa2ration.profile.ProfileRepository;
import com.xda.sa2ration.shell.RootShellExecutor;
import java.util.List;

public final class NextProfileTileService extends TileService {
    @Override public void onClick(){super.onClick();new Thread(()->{ConfigurationRepository states=ConfigurationRepository.getInstance(this);List<DisplayProfile>profiles=ProfileRepository.getInstance(this).load();if(profiles.isEmpty())return;String active=states.load().stable.activeProfileId;int next=0;for(int i=0;i<profiles.size();i++)if(profiles.get(i).id.equals(active)){next=(i+1)%profiles.size();break;}DisplayConfiguration c=profiles.get(next).configuration.copy();c.activeProfileId=profiles.get(next).id;try(RootShellExecutor executor=new RootShellExecutor()){if(new SurfaceFlingerBackend(executor).apply(c,DefaultTransformStages.createPipeline().compose(c)).isSuccess())states.confirmStable(c);}},"sa2ration-tile-profile").start();}
}
