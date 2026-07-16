package com.xda.sa2ration.ui.widget;

import android.content.Context;
import android.text.InputType;
import android.view.inputmethod.EditorInfo;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.slider.Slider;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Locale;

public final class NumericControlView extends LinearLayout {
    public interface Listener { void onValue(double value); }
    private final Slider slider;
    private final TextInputEditText input;
    private final double manualMin;
    private final double manualMax;
    private final Listener listener;
    private boolean synchronizing;
    private double value;

    public NumericControlView(Context context){this(context,"",0,-1,1,.01,-100,100,value->{});}

    public NumericControlView(Context context, String label, double initial, double sliderMin,
                              double sliderMax, double step, double manualMin, double manualMax,
                              Listener listener) {
        super(context);
        this.manualMin=manualMin; this.manualMax=manualMax; this.listener=listener;
        setOrientation(VERTICAL);
        int pad=dp(4); setPadding(0,pad,0,pad);
        TextView title=new TextView(context); title.setText(label); title.setTextSize(13); addView(title);
        LinearLayout row=new LinearLayout(context); row.setOrientation(HORIZONTAL); row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        slider=new Slider(context); slider.setValueFrom((float)sliderMin);slider.setValueTo((float)sliderMax);
        if(step>0)slider.setStepSize((float)step);
        row.addView(slider,new LayoutParams(0,LayoutParams.WRAP_CONTENT,1));
        TextInputLayout inputLayout=new TextInputLayout(context); inputLayout.setHint("Valor");
        input=new TextInputEditText(context); input.setSingleLine(true);input.setSelectAllOnFocus(true);
        input.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL|InputType.TYPE_NUMBER_FLAG_SIGNED);
        input.setImeOptions(EditorInfo.IME_ACTION_DONE); inputLayout.addView(input);
        row.addView(inputLayout,new LayoutParams(dp(104),LayoutParams.WRAP_CONTENT)); addView(row);
        slider.addOnChangeListener((s,v,fromUser)->{if(fromUser&&!synchronizing){value=v;syncInput();listener.onValue(value);}});
        input.setOnEditorActionListener((v,action,event)->{if(action==EditorInfo.IME_ACTION_DONE){commitInput();return true;}return false;});
        input.setOnFocusChangeListener((v,hasFocus)->{if(!hasFocus)commitInput();});
        setValue(initial);
    }

    public void setValue(double newValue) {
        value=finiteClamp(newValue); synchronizing=true;
        slider.setValue((float)Math.max(slider.getValueFrom(),Math.min(slider.getValueTo(),value)));
        syncInput(); synchronizing=false;
    }

    public double getValue(){return value;}

    private void commitInput(){
        if(synchronizing)return;
        try{String raw=String.valueOf(input.getText()).trim().replace(',','.');setValue(Double.parseDouble(raw));listener.onValue(value);}
        catch(NumberFormatException ignored){syncInput();}
    }

    private double finiteClamp(double candidate){
        if(!Double.isFinite(candidate))return value;
        return Math.max(manualMin,Math.min(manualMax,candidate));
    }

    private void syncInput(){
        synchronizing=true;input.setText(String.format(Locale.US,"%.4f",value));input.setSelection(input.length());synchronizing=false;
    }

    private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
}
