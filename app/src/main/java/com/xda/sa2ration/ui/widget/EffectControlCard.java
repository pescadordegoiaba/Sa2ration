package com.xda.sa2ration.ui.widget;

import android.content.Context;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputLayout;
import com.xda.sa2ration.R;

public final class EffectControlCard extends MaterialCardView {
    public interface ToggleListener {void onToggle(boolean enabled);}
    private final LinearLayout modifiers;
    private final SwitchMaterial toggle;

    public EffectControlCard(Context context){this(context,"","",false,enabled->{},null);}
    public EffectControlCard(Context context,String title,String description,boolean enabled,ToggleListener listener,Runnable resetAction){
        super(context);setRadius(dp(20));setCardElevation(dp(1));setStrokeWidth(dp(1));setStrokeColor(MaterialColors.getColor(this,com.google.android.material.R.attr.colorOutline,0x22000000));setCardBackgroundColor(MaterialColors.getColor(this,com.google.android.material.R.attr.colorSurfaceContainer,0xffffffff));
        LinearLayout.LayoutParams cardParams=new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.WRAP_CONTENT);cardParams.setMargins(0,0,0,getResources().getDimensionPixelSize(R.dimen.card_spacing));setLayoutParams(cardParams);
        LinearLayout root=new LinearLayout(context);root.setOrientation(LinearLayout.VERTICAL);int padding=getResources().getDimensionPixelSize(R.dimen.card_content_padding);root.setPadding(padding,padding,padding,padding);addView(root);
        toggle=new SwitchMaterial(context);toggle.setText(title);toggle.setTextSize(16);toggle.setMinHeight(dp(48));toggle.setChecked(enabled);root.addView(toggle,new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.WRAP_CONTENT));
        TextView desc=new TextView(context);desc.setText(description);desc.setTextSize(13);desc.setTextColor(MaterialColors.getColor(desc,com.google.android.material.R.attr.colorOnSurfaceVariant,0xff555555));desc.setPadding(0,0,0,dp(8));root.addView(desc);
        modifiers=new LinearLayout(context);modifiers.setOrientation(LinearLayout.VERTICAL);modifiers.setVisibility(enabled?View.VISIBLE:View.GONE);root.addView(modifiers,new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.WRAP_CONTENT));
        if(resetAction!=null){MaterialButton reset=outlinedButton(context);reset.setText(R.string.reset_feature);reset.setOnClickListener(v->resetAction.run());modifiers.addView(reset);}
        toggle.setOnCheckedChangeListener((button,isChecked)->{modifiers.setVisibility(isChecked?View.VISIBLE:View.GONE);listener.onToggle(isChecked);});
    }
    public NumericControlView addNumeric(String label,double initial,double sliderMin,double sliderMax,double step,double manualMin,double manualMax,NumericControlView.Listener listener){NumericControlView view=new NumericControlView(getContext(),label,initial,sliderMin,sliderMax,step,manualMin,manualMax,listener);modifiers.addView(view,new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.WRAP_CONTENT));return view;}
    public MaterialAutoCompleteTextView addDropdown(String hint,String[]values,String selected,android.widget.AdapterView.OnItemClickListener listener){TextInputLayout layout=new TextInputLayout(getContext(),null,com.google.android.material.R.attr.textInputOutlinedStyle);layout.setHint(hint);layout.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE);MaterialAutoCompleteTextView dropdown=new MaterialAutoCompleteTextView(layout.getContext());dropdown.setInputType(0);dropdown.setMinHeight(dp(48));dropdown.setAdapter(new ArrayAdapter<>(getContext(),android.R.layout.simple_dropdown_item_1line,values));dropdown.setText(selected,false);dropdown.setOnItemClickListener(listener);layout.addView(dropdown);LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.WRAP_CONTENT);params.setMargins(0,dp(6),0,dp(6));modifiers.addView(layout,params);return dropdown;}
    public MaterialCheckBox addCheckBox(String label,boolean checked,android.widget.CompoundButton.OnCheckedChangeListener listener){MaterialCheckBox box=new MaterialCheckBox(getContext());box.setText(label);box.setMinHeight(dp(48));box.setChecked(checked);box.setOnCheckedChangeListener(listener);modifiers.addView(box);return box;}
    public MaterialButton addAction(String label,View.OnClickListener listener){MaterialButton button=outlinedButton(getContext());button.setText(label);button.setOnClickListener(listener);modifiers.addView(button);return button;}
    public void setEffectEnabled(boolean enabled){toggle.setChecked(enabled);}public LinearLayout modifiers(){return modifiers;}
    private MaterialButton outlinedButton(Context context){MaterialButton button=new MaterialButton(context,null,com.google.android.material.R.attr.materialButtonOutlinedStyle);button.setMinHeight(dp(48));button.setMaxLines(2);return button;}
    private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
}
