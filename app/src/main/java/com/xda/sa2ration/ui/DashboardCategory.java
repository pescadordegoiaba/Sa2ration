package com.xda.sa2ration.ui;

import androidx.annotation.StringRes;
import com.xda.sa2ration.R;

public enum DashboardCategory {
    SIMPLE(R.string.category_simple), COLOR(R.string.category_color), RGB(R.string.category_rgb),
    PROFILES(R.string.category_profiles), COMPATIBILITY(R.string.category_compatibility), ADVANCED(R.string.category_advanced);
    @StringRes public final int titleRes;
    DashboardCategory(@StringRes int titleRes){this.titleRes=titleRes;}
}
