package com.xda.sa2ration.ui.widget;

import android.content.Context;
import android.text.InputType;
import android.view.Gravity;
import android.view.inputmethod.EditorInfo;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.slider.Slider;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.xda.sa2ration.R;

import java.util.Locale;

/** Slider + numeric field that stacks vertically when width or font scale is constrained. */
public final class NumericControlView extends LinearLayout {
    public interface Listener { void onValue(double value); }
    private final LinearLayout row;
    private final Slider slider;
    private final TextInputLayout inputLayout;
    private final TextInputEditText input;
    private final TextView valuePreview;
    private final double manualMin;
    private final double manualMax;
    private final Listener listener;
    private boolean synchronizing;
    private boolean compact;
    private boolean layoutInitialized;
    private double value;

    public NumericControlView(Context context){this(context,"",0,-1,1,.01,-100,100,value->{});}

    public NumericControlView(Context context,String label,double initial,double sliderMin,double sliderMax,
                              double step,double manualMin,double manualMax,Listener listener){
        super(context);this.manualMin=manualMin;this.manualMax=manualMax;this.listener=listener;
        setOrientation(VERTICAL);setPadding(0,dp(6),0,dp(6));
        LinearLayout heading=new LinearLayout(context);heading.setOrientation(HORIZONTAL);heading.setGravity(Gravity.CENTER_VERTICAL);
        TextView title=new TextView(context);title.setText(label);title.setTextSize(14);title.setTextColor(materialColor(com.google.android.material.R.attr.colorOnSurface));heading.addView(title,new LayoutParams(0,LayoutParams.WRAP_CONTENT,1));
        valuePreview=new TextView(context);valuePreview.setTextSize(14);valuePreview.setTextColor(materialColor(com.google.android.material.R.attr.colorPrimary));valuePreview.setPadding(dp(8),0,0,0);valuePreview.setContentDescription(label);heading.addView(valuePreview,new LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT));addView(heading,new LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.WRAP_CONTENT));
        row=new LinearLayout(context);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(0,dp(2),0,0);addView(row,new LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.WRAP_CONTENT));
        slider=new Slider(context);slider.setValueFrom((float)sliderMin);slider.setValueTo((float)sliderMax);slider.setMinimumHeight(dp(48));
        if(step>0)slider.setStepSize((float)step);row.addView(slider,new LayoutParams(0,LayoutParams.WRAP_CONTENT,1));
        inputLayout=new TextInputLayout(context,null,com.google.android.material.R.attr.textInputOutlinedStyle);inputLayout.setHint(R.string.numeric_value_hint);inputLayout.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE);
        input=new TextInputEditText(inputLayout.getContext());input.setSingleLine(true);input.setSelectAllOnFocus(true);input.setMinHeight(dp(48));
        input.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL|InputType.TYPE_NUMBER_FLAG_SIGNED);input.setImeOptions(EditorInfo.IME_ACTION_DONE);inputLayout.addView(input);row.addView(inputLayout,new LayoutParams(getResources().getDimensionPixelSize(R.dimen.control_input_width),LayoutParams.WRAP_CONTENT));
        slider.addOnChangeListener((s,v,fromUser)->{if(fromUser&&!synchronizing){value=v;syncInput();listener.onValue(value);}});
        input.setOnEditorActionListener((v,action,event)->{if(action==EditorInfo.IME_ACTION_DONE){commitInput();return true;}return false;});
        input.setOnFocusChangeListener((v,hasFocus)->{if(!hasFocus)commitInput();});
        setCompact(shouldStack(getResources().getConfiguration().screenWidthDp));setValue(initial);
    }

    @Override protected void onSizeChanged(int width,int height,int oldWidth,int oldHeight){super.onSizeChanged(width,height,oldWidth,oldHeight);if(width>0)setCompact(shouldStack(Math.round(width/getResources().getDisplayMetrics().density)));}
    private boolean shouldStack(int widthDp){return widthDp<340||getResources().getConfiguration().fontScale>=1.25f;}
    private void setCompact(boolean stack){if(layoutInitialized&&compact==stack)return;layoutInitialized=true;compact=stack;row.setOrientation(stack?VERTICAL:HORIZONTAL);if(stack){slider.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.WRAP_CONTENT));LinearLayout.LayoutParams inputParams=new LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.WRAP_CONTENT);inputParams.setMargins(0,dp(4),0,0);inputLayout.setLayoutParams(inputParams);}else{slider.setLayoutParams(new LayoutParams(0,LayoutParams.WRAP_CONTENT,1));LinearLayout.LayoutParams inputParams=new LayoutParams(getResources().getDimensionPixelSize(R.dimen.control_input_width),LayoutParams.WRAP_CONTENT);inputParams.setMargins(dp(8),0,0,0);inputLayout.setLayoutParams(inputParams);}slider.setVisibility(VISIBLE);inputLayout.setVisibility(VISIBLE);requestLayout();}
    public void setValue(double newValue){value=finiteClamp(newValue);synchronizing=true;slider.setValue((float)Math.max(slider.getValueFrom(),Math.min(slider.getValueTo(),value)));syncInput();synchronizing=false;}
    public double getValue(){return value;}
    private void commitInput(){if(synchronizing)return;try{String raw=String.valueOf(input.getText()).trim().replace(',','.');setValue(Double.parseDouble(raw));listener.onValue(value);}catch(NumberFormatException ignored){syncInput();}}
    private double finiteClamp(double candidate){if(!Double.isFinite(candidate))return value;return Math.max(manualMin,Math.min(manualMax,candidate));}
    private void syncInput(){synchronizing=true;String formatted=String.format(Locale.US,"%.4f",value);valuePreview.setText(formatted);input.setText(formatted);input.setSelection(input.length());synchronizing=false;}
    private int materialColor(int attribute){return com.google.android.material.color.MaterialColors.getColor(this,attribute,0xff000000);}
    private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
}
