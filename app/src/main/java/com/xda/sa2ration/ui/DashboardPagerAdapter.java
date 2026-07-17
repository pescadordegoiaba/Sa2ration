package com.xda.sa2ration.ui;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class DashboardPagerAdapter extends FragmentStateAdapter {
    private final List<DashboardCategory> categories=new ArrayList<>();
    public DashboardPagerAdapter(FragmentActivity activity){super(activity);
        // Hardware-only and duplicate placeholder pages were consolidated into Compatibility.
        // This keeps every functional editor reachable without presenting unsupported cards as options.
        categories.addAll(Arrays.asList(DashboardCategory.SIMPLE,DashboardCategory.COLOR,
                DashboardCategory.RGB,DashboardCategory.PROFILES,
                DashboardCategory.COMPATIBILITY,DashboardCategory.ADVANCED));
    }
    @NonNull @Override public Fragment createFragment(int position){return DashboardFragment.newInstance(categories.get(position));}
    @Override public int getItemCount(){return categories.size();}
    public DashboardCategory category(int position){return categories.get(position);}
}
