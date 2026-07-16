package com.xda.sa2ration.ui.widget;

import android.content.Context;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.color.MaterialColors;
import com.xda.sa2ration.R;
import com.xda.sa2ration.backend.CapabilityStatus;

public final class CapabilityCard extends MaterialCardView {
    public CapabilityCard(Context context){this(context,"","",CapabilityStatus.UNKNOWN);}
    public CapabilityCard(Context context,String title,String description,CapabilityStatus status){
        super(context);setRadius(dp(20));setCardElevation(0);setStrokeWidth(dp(1));setStrokeColor(MaterialColors.getColor(this,com.google.android.material.R.attr.colorOutline,0x44000000));setCardBackgroundColor(MaterialColors.getColor(this,com.google.android.material.R.attr.colorSurfaceContainer,0xffffffff));
        LinearLayout.LayoutParams cardParams=new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.WRAP_CONTENT);cardParams.setMargins(0,0,0,getResources().getDimensionPixelSize(R.dimen.card_spacing));setLayoutParams(cardParams);
        LinearLayout root=new LinearLayout(context);root.setOrientation(LinearLayout.VERTICAL);int padding=getResources().getDimensionPixelSize(R.dimen.card_content_padding);root.setPadding(padding,padding,padding,padding);addView(root);
        TextView name=new TextView(context);name.setText(title);name.setTextSize(16);name.setTextColor(MaterialColors.getColor(name,com.google.android.material.R.attr.colorOnSurface,0xff111111));root.addView(name);
        TextView state=new TextView(context);state.setText(label(status));state.setTextSize(12);state.setTextColor(color(status));state.setPadding(0,dp(3),0,dp(4));root.addView(state);
        TextView desc=new TextView(context);desc.setText(description);desc.setTextSize(13);desc.setTextColor(MaterialColors.getColor(desc,com.google.android.material.R.attr.colorOnSurfaceVariant,0xff555555));root.addView(desc);
    }
    private String label(CapabilityStatus status){switch(status){case SUPPORTED:return getContext().getString(R.string.status_supported);case UNSUPPORTED:return getContext().getString(R.string.status_unsupported);case EXPERIMENTAL:return getContext().getString(R.string.status_experimental);case REQUIRES_MODULE:return getContext().getString(R.string.status_requires_module);case FAILED:return getContext().getString(R.string.status_failed);case UNTESTED:return getContext().getString(R.string.status_untested);default:return getContext().getString(R.string.status_unknown);}}
    private int color(CapabilityStatus status){if(status==CapabilityStatus.SUPPORTED)return getContext().getColor(R.color.sa_success);if(status==CapabilityStatus.FAILED||status==CapabilityStatus.UNSUPPORTED)return getContext().getColor(R.color.sa_error);return getContext().getColor(R.color.sa_warning);}
    private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
}
