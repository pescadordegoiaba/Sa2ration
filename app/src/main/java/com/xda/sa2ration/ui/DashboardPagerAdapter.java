package com.xda.sa2ration.ui;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import java.util.ArrayList;
import java.util.List;
import com.xda.sa2ration.panel.DisplayPanelInfo;

public final class DashboardPagerAdapter extends FragmentStateAdapter {
    private final List<DashboardCategory> categories=new ArrayList<>();
    public DashboardPagerAdapter(FragmentActivity activity,DisplayPanelInfo panel){super(activity);
        for(DashboardCategory category:DashboardCategory.values()){
            if(category==DashboardCategory.OLED&&!panel.isOledFamily())continue;
            if(category==DashboardCategory.LCD&&!panel.isLcdFamily())continue;
            categories.add(category);
        }
    }
    @NonNull @Override public Fragment createFragment(int position){return DashboardFragment.newInstance(categories.get(position));}
    @Override public int getItemCount(){return categories.size();}
    public DashboardCategory category(int position){return categories.get(position);}
}
