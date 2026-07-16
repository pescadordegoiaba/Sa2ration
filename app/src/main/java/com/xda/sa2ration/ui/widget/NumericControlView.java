package com.xda.sa2ration.ui.widget;

import android.content.Context;
import android.content.res.ColorStateList;
import android.text.InputType;
import android.view.Gravity;
import android.view.inputmethod.EditorInfo;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.color.MaterialColors;
import com.google.android.material.slider.Slider;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.xda.sa2ration.R;

import java.util.Locale;

/** Numeric editor with deterministic full-width measurement on every screen size. */
public final class NumericControlView extends LinearLayout {
    public interface Listener {void onValue(double value);}
    private final Slider slider;
    private final TextInputEditText input;
    private final TextView valuePreview;
    private final double manualMin;
    private final double manualMax;
    private final Listener listener;
    private boolean synchronizing;
    private double value;

    public NumericControlView(Context context){this(context,"",0,-1,1,.01,-100,100,value->{});}

    public NumericControlView(Context context,String label,double initial,double sliderMin,double sliderMax,
                              double step,double manualMin,double manualMax,Listener listener){
        super(context);this.manualMin=manualMin;this.manualMax=manualMax;this.listener=listener;
        setOrientation(VERTICAL);setPadding(0,dp(8),0,dp(10));setMinimumHeight(dp(152));

        LinearLayout heading=new LinearLayout(context);heading.setOrientation(HORIZONTAL);heading.setGravity(Gravity.CENTER_VERTICAL);
        TextView title=new TextView(context);title.setText(label);title.setTextSize(14);title.setTextColor(color(com.google.android.material.R.attr.colorOnSurface));heading.addView(title,new LayoutParams(0,LayoutParams.WRAP_CONTENT,1));
        valuePreview=new TextView(context);valuePreview.setTextSize(14);valuePreview.setTextColor(color(com.google.android.material.R.attr.colorPrimary));valuePreview.setPadding(dp(8),0,0,0);valuePreview.setContentDescription(label);heading.addView(valuePreview,new LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT));addView(heading,new LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.WRAP_CONTENT));

        int primary=color(com.google.android.material.R.attr.colorPrimary);
        int surface=color(com.google.android.material.R.attr.colorSurface);
        int surfaceVariant=color(com.google.android.material.R.attr.colorSurfaceVariant);
        int onSurface=color(com.google.android.material.R.attr.colorOnSurface);
        int onSurfaceVariant=color(com.google.android.material.R.attr.colorOnSurfaceVariant);

        slider=new Slider(context);slider.setValueFrom((float)sliderMin);slider.setValueTo((float)sliderMax);slider.setMinimumHeight(dp(56));slider.setTrackHeight(dp(4));slider.setThumbRadius(dp(10));slider.setTrackActiveTintList(ColorStateList.valueOf(primary));slider.setTrackInactiveTintList(ColorStateList.valueOf(surfaceVariant));slider.setThumbTintList(ColorStateList.valueOf(primary));
        if(step>0)slider.setStepSize((float)step);LayoutParams sliderParams=new LayoutParams(LayoutParams.MATCH_PARENT,dp(56));sliderParams.setMargins(0,dp(2),0,dp(4));addView(slider,sliderParams);

        TextInputLayout inputLayout=new TextInputLayout(context,null,com.google.android.material.R.attr.textInputOutlinedStyle);inputLayout.setHint(R.string.numeric_value_hint);inputLayout.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE);inputLayout.setBoxBackgroundColor(surface);inputLayout.setBoxStrokeColor(primary);inputLayout.setHintTextColor(ColorStateList.valueOf(onSurfaceVariant));inputLayout.setMinimumHeight(dp(64));inputLayout.setVisibility(VISIBLE);
        input=new TextInputEditText(inputLayout.getContext());input.setSingleLine(true);input.setSelectAllOnFocus(true);input.setMinimumHeight(dp(56));input.setTextSize(16);input.setTextColor(onSurface);input.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL|InputType.TYPE_NUMBER_FLAG_SIGNED);input.setImeOptions(EditorInfo.IME_ACTION_DONE);inputLayout.addView(input,new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.WRAP_CONTENT));LayoutParams inputParams=new LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.WRAP_CONTENT);addView(inputLayout,inputParams);

        slider.setVisibility(VISIBLE);slider.addOnChangeListener((control,newValue,fromUser)->{if(fromUser&&!synchronizing){value=newValue;syncText();listener.onValue(value);}});
        input.setOnEditorActionListener((view,action,event)->{if(action==EditorInfo.IME_ACTION_DONE){commitInput();return true;}return false;});input.setOnFocusChangeListener((view,hasFocus)->{if(!hasFocus)commitInput();});setValue(initial);
    }

    public void setValue(double newValue){value=finiteClamp(newValue);synchronizing=true;slider.setValue((float)Math.max(slider.getValueFrom(),Math.min(slider.getValueTo(),value)));syncText();synchronizing=false;}
    public double getValue(){return value;}
    private void commitInput(){if(synchronizing)return;try{String raw=String.valueOf(input.getText()).trim().replace(',','.');setValue(Double.parseDouble(raw));listener.onValue(value);}catch(NumberFormatException ignored){syncText();}}
    private double finiteClamp(double candidate){if(!Double.isFinite(candidate))return value;return Math.max(manualMin,Math.min(manualMax,candidate));}
    private void syncText(){synchronizing=true;String formatted=String.format(Locale.US,"%.4f",value);valuePreview.setText(formatted);input.setText(formatted);input.setSelection(input.length());synchronizing=false;}
    private int color(int attribute){return MaterialColors.getColor(this,attribute,0xff000000);}
    private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
}
