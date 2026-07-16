package com.xda.sa2ration.ui.widget;

import android.content.Context;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputLayout;
import com.xda.sa2ration.R;

public final class EffectControlCard extends MaterialCardView {
    public interface ToggleListener { void onToggle(boolean enabled); }
    private final LinearLayout modifiers;
    private final SwitchMaterial toggle;

    public EffectControlCard(Context context){this(context,"","",false,enabled->{},null);}

    public EffectControlCard(Context context,String title,String description,boolean enabled,
                             ToggleListener listener,Runnable resetAction) {
        super(context);setUseCompatPadding(true);setCardElevation(dp(2));
        LinearLayout root=new LinearLayout(context);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(16),dp(12),dp(16),dp(12));addView(root);
        toggle=new SwitchMaterial(context);toggle.setText(title);toggle.setTextSize(16);toggle.setChecked(enabled);root.addView(toggle);
        TextView desc=new TextView(context);desc.setText(description);desc.setTextSize(12);desc.setAlpha(.75f);root.addView(desc);
        modifiers=new LinearLayout(context);modifiers.setOrientation(LinearLayout.VERTICAL);modifiers.setVisibility(enabled?View.VISIBLE:View.GONE);root.addView(modifiers);
        if(resetAction!=null){Button reset=new Button(context);reset.setText(R.string.reset_feature);reset.setOnClickListener(v->resetAction.run());modifiers.addView(reset);}
        toggle.setOnCheckedChangeListener((button,isChecked)->{modifiers.setVisibility(isChecked?View.VISIBLE:View.GONE);listener.onToggle(isChecked);});
    }

    public NumericControlView addNumeric(String label,double initial,double sliderMin,double sliderMax,
                                         double step,double manualMin,double manualMax,NumericControlView.Listener listener){
        NumericControlView view=new NumericControlView(getContext(),label,initial,sliderMin,sliderMax,step,manualMin,manualMax,listener);
        modifiers.addView(view);return view;
    }

    public MaterialAutoCompleteTextView addDropdown(String hint,String[] values,String selected,
                                                     android.widget.AdapterView.OnItemClickListener listener){
        TextInputLayout layout=new TextInputLayout(getContext());layout.setHint(hint);
        MaterialAutoCompleteTextView dropdown=new MaterialAutoCompleteTextView(getContext());dropdown.setInputType(0);
        dropdown.setAdapter(new ArrayAdapter<>(getContext(),android.R.layout.simple_dropdown_item_1line,values));dropdown.setText(selected,false);
        dropdown.setOnItemClickListener(listener);layout.addView(dropdown);modifiers.addView(layout);return dropdown;
    }

    public CheckBox addCheckBox(String label,boolean checked,android.widget.CompoundButton.OnCheckedChangeListener listener){
        CheckBox box=new CheckBox(getContext());box.setText(label);box.setChecked(checked);box.setOnCheckedChangeListener(listener);modifiers.addView(box);return box;
    }

    public Button addAction(String label,View.OnClickListener listener){
        Button button=new Button(getContext());button.setText(label);button.setOnClickListener(listener);modifiers.addView(button);return button;
    }

    public void setEffectEnabled(boolean enabled){toggle.setChecked(enabled);}
    public LinearLayout modifiers(){return modifiers;}
    private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
}
