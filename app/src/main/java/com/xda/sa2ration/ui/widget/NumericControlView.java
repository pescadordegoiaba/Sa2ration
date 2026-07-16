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
    private final double manualMin;
    private final double manualMax;
    private final Listener listener;
    private boolean synchronizing;
    private boolean compact;
    private double value;

    public NumericControlView(Context context){this(context,"",0,-1,1,.01,-100,100,value->{});}

    public NumericControlView(Context context,String label,double initial,double sliderMin,double sliderMax,
                              double step,double manualMin,double manualMax,Listener listener){
        super(context);this.manualMin=manualMin;this.manualMax=manualMax;this.listener=listener;
        setOrientation(VERTICAL);setPadding(0,dp(6),0,dp(6));
        TextView title=new TextView(context);title.setText(label);title.setTextSize(14);title.setTextColor(materialColor(com.google.android.material.R.attr.colorOnSurface));addView(title);
        row=new LinearLayout(context);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(0,dp(2),0,0);addView(row,new LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.WRAP_CONTENT));
        slider=new Slider(context);slider.setValueFrom((float)sliderMin);slider.setValueTo((float)sliderMax);slider.setMinimumHeight(dp(48));
        if(step>0)slider.setStepSize((float)step);row.addView(slider);
        inputLayout=new TextInputLayout(context,null,com.google.android.material.R.attr.textInputOutlinedStyle);inputLayout.setHint(R.string.numeric_value_hint);inputLayout.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE);
        input=new TextInputEditText(inputLayout.getContext());input.setSingleLine(true);input.setSelectAllOnFocus(true);input.setMinHeight(dp(48));
        input.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL|InputType.TYPE_NUMBER_FLAG_SIGNED);input.setImeOptions(EditorInfo.IME_ACTION_DONE);inputLayout.addView(input);row.addView(inputLayout);
        slider.addOnChangeListener((s,v,fromUser)->{if(fromUser&&!synchronizing){value=v;syncInput();listener.onValue(value);}});
        input.setOnEditorActionListener((v,action,event)->{if(action==EditorInfo.IME_ACTION_DONE){commitInput();return true;}return false;});
        input.setOnFocusChangeListener((v,hasFocus)->{if(!hasFocus)commitInput();});
        setCompact(shouldStack(getResources().getConfiguration().screenWidthDp));setValue(initial);
    }

    @Override protected void onSizeChanged(int width,int height,int oldWidth,int oldHeight){super.onSizeChanged(width,height,oldWidth,oldHeight);if(width>0)setCompact(shouldStack(Math.round(width/getResources().getDisplayMetrics().density)));}
    private boolean shouldStack(int widthDp){return widthDp<340||getResources().getConfiguration().fontScale>=1.25f;}
    private void setCompact(boolean stack){if(compact==stack&&slider.getLayoutParams()!=null)return;compact=stack;row.setOrientation(stack?VERTICAL:HORIZONTAL);if(stack){slider.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.WRAP_CONTENT));inputLayout.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.WRAP_CONTENT));}else{slider.setLayoutParams(new LayoutParams(0,LayoutParams.WRAP_CONTENT,1));inputLayout.setLayoutParams(new LayoutParams(getResources().getDimensionPixelSize(R.dimen.control_input_width),LayoutParams.WRAP_CONTENT));}requestLayout();}
    public void setValue(double newValue){value=finiteClamp(newValue);synchronizing=true;slider.setValue((float)Math.max(slider.getValueFrom(),Math.min(slider.getValueTo(),value)));syncInput();synchronizing=false;}
    public double getValue(){return value;}
    private void commitInput(){if(synchronizing)return;try{String raw=String.valueOf(input.getText()).trim().replace(',','.');setValue(Double.parseDouble(raw));listener.onValue(value);}catch(NumberFormatException ignored){syncInput();}}
    private double finiteClamp(double candidate){if(!Double.isFinite(candidate))return value;return Math.max(manualMin,Math.min(manualMax,candidate));}
    private void syncInput(){synchronizing=true;input.setText(String.format(Locale.US,"%.4f",value));input.setSelection(input.length());synchronizing=false;}
    private int materialColor(int attribute){return com.google.android.material.color.MaterialColors.getColor(this,attribute,0xff000000);}
    private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
}
