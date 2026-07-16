package com.xda.sa2ration.ui.calibration;

import android.content.Context;import android.graphics.Canvas;import android.graphics.Color;import android.graphics.LinearGradient;import android.graphics.Paint;import android.graphics.Shader;import android.view.View;

public final class CalibrationPatternView extends View {
    private static final int[] SMPTE_COLORS={Color.LTGRAY,Color.YELLOW,Color.CYAN,Color.GREEN,Color.MAGENTA,Color.RED,Color.BLUE};
    private static final int[] PRIMARY_COLORS={Color.RED,Color.GREEN,Color.BLUE,Color.CYAN,Color.MAGENTA,Color.YELLOW,Color.WHITE,Color.BLACK};
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);private CalibrationPattern pattern=CalibrationPattern.SMPTE;
    public CalibrationPatternView(Context context){super(context);setLayerType(View.LAYER_TYPE_SOFTWARE,null);}
    public void setPattern(CalibrationPattern value){pattern=value;invalidate();}
    @Override protected void onMeasure(int widthSpec,int heightSpec){int width=MeasureSpec.getSize(widthSpec);int desired=Math.round(width*.625f);int height=MeasureSpec.getMode(heightSpec)==MeasureSpec.EXACTLY?MeasureSpec.getSize(heightSpec):Math.min(desired,MeasureSpec.getSize(heightSpec));setMeasuredDimension(width,Math.max(height,240));}
    @Override protected void onDraw(Canvas canvas){super.onDraw(canvas);switch(pattern){case SMPTE:bars(canvas,SMPTE_COLORS);break;case GRAYSCALE:gray(canvas);break;case BLACK_CLIPPING:clipping(canvas,false);break;case WHITE_CLIPPING:clipping(canvas,true);break;case GRADIENT:gradient(canvas);break;case PRIMARY_COLORS:bars(canvas,PRIMARY_COLORS);break;case OLED_TEST:oled(canvas);break;case LCD_TEST:lcd(canvas);break;}}
    private void bars(Canvas c,int[]colors){float width=getWidth()/(float)colors.length;for(int i=0;i<colors.length;i++){paint.setShader(null);paint.setColor(colors[i]);c.drawRect(i*width,0,(i+1)*width,getHeight(),paint);}}
    private void gray(Canvas c){int steps=16;float width=getWidth()/(float)steps;for(int i=0;i<steps;i++){int value=Math.round(255*i/(steps-1f));paint.setColor(Color.rgb(value,value,value));paint.setShader(null);c.drawRect(i*width,0,(i+1)*width,getHeight(),paint);}}
    private void clipping(Canvas c,boolean white){c.drawColor(white?Color.WHITE:Color.BLACK);int steps=12;float width=getWidth()/(float)steps;float top=getHeight()*.2f,bottom=getHeight()*.8f;for(int i=0;i<steps;i++){int value=white?255-i: i;paint.setColor(Color.rgb(value,value,value));c.drawRect(i*width+2,top,(i+1)*width-2,bottom,paint);}}
    private void gradient(Canvas c){paint.setShader(new LinearGradient(0,0,getWidth(),0,Color.BLACK,Color.WHITE,Shader.TileMode.CLAMP));c.drawRect(0,0,getWidth(),getHeight()/2f,paint);paint.setShader(new LinearGradient(0,0,getWidth(),0,new int[]{Color.RED,Color.YELLOW,Color.GREEN,Color.CYAN,Color.BLUE,Color.MAGENTA,Color.RED},null,Shader.TileMode.CLAMP));c.drawRect(0,getHeight()/2f,getWidth(),getHeight(),paint);paint.setShader(null);}
    private void oled(Canvas c){c.drawColor(Color.BLACK);paint.setColor(Color.WHITE);c.drawCircle(getWidth()*.5f,getHeight()*.5f,Math.min(getWidth(),getHeight())*.08f,paint);paint.setColor(Color.rgb(1,1,1));c.drawRect(0,0,getWidth()*.25f,getHeight()*.25f,paint);paint.setColor(Color.RED);c.drawRect(getWidth()*.75f,0,getWidth(),getHeight()*.25f,paint);}
    private void lcd(Canvas c){paint.setShader(new LinearGradient(0,0,0,getHeight(),Color.WHITE,Color.BLACK,Shader.TileMode.CLAMP));c.drawRect(0,0,getWidth(),getHeight(),paint);paint.setShader(null);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(2);paint.setColor(Color.GRAY);for(int i=1;i<10;i++){float x=getWidth()*i/10f;c.drawLine(x,0,x,getHeight(),paint);}paint.setStyle(Paint.Style.FILL);}
}
