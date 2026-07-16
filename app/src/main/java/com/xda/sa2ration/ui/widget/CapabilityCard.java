package com.xda.sa2ration.ui.widget;

import android.content.Context;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.google.android.material.card.MaterialCardView;
import com.xda.sa2ration.backend.CapabilityStatus;

public final class CapabilityCard extends MaterialCardView {
    public CapabilityCard(Context context){this(context,"","",CapabilityStatus.UNKNOWN);}
    public CapabilityCard(Context context,String title,String description,CapabilityStatus status){
        super(context);setUseCompatPadding(true);
        LinearLayout root=new LinearLayout(context);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(16),dp(12),dp(16),dp(12));addView(root);
        TextView name=new TextView(context);name.setText(title);name.setTextSize(16);root.addView(name);
        TextView state=new TextView(context);state.setText(label(status));state.setTextSize(12);state.setTextColor(color(status));root.addView(state);
        TextView desc=new TextView(context);desc.setText(description);desc.setTextSize(12);desc.setAlpha(.75f);root.addView(desc);
    }
    private String label(CapabilityStatus s){switch(s){case SUPPORTED:return"Suportado";case UNSUPPORTED:return"Não suportado";case EXPERIMENTAL:return"Experimental";case REQUIRES_MODULE:return"Requer módulo complementar";case FAILED:return"Falhou";case UNTESTED:return"Não testado";default:return"Desconhecido";}}
    private int color(CapabilityStatus s){return s==CapabilityStatus.SUPPORTED?0xff2e7d32:(s==CapabilityStatus.FAILED||s==CapabilityStatus.UNSUPPORTED?0xffc62828:0xffef6c00);}
    private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
}
