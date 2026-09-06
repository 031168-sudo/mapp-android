package com.minews1;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;
import java.util.List;

public class GraphView extends View {
    private final Paint p=new Paint(3); private List<HistoryDb.Reading> data;
    public GraphView(Context c){super(c);p.setTypeface(android.graphics.Typeface.create("sans",0));}
    public void setData(List<HistoryDb.Reading> d){data=d;invalidate();}
    @Override protected void onDraw(Canvas c){super.onDraw(c);float w=getWidth(),h=getHeight();p.setStyle(Paint.Style.FILL);p.setColor(0xff222222);p.setTextSize(30);c.drawText("Температура / влажность",24,40,p);if(data==null||data.size()<2){p.setTextSize(22);c.drawText("Пока недостаточно данных",24,90,p);return;}float l=65,r=w-55,t=70,b=h-65;float minT=Float.MAX_VALUE,maxT=-Float.MAX_VALUE,minH=Float.MAX_VALUE,maxH=-Float.MAX_VALUE;for(HistoryDb.Reading x:data){minT=Math.min(minT,x.temp);maxT=Math.max(maxT,x.temp);minH=Math.min(minH,x.hum);maxH=Math.max(maxH,x.hum);}if(maxT-minT<1){minT-=0.5f;maxT+=0.5f;}if(maxH-minH<2){minH-=1;maxH+=1;}p.setStrokeWidth(2);p.setColor(0xffbbbbbb);c.drawLine(l,t,l,b,p);c.drawLine(l,b,r,b,p);p.setTextSize(18);p.setColor(0xff444444);c.drawText(String.format("%.1f°C",maxT),8,t+6,p);c.drawText(String.format("%.1f°C",minT),8,b,p);c.drawText(String.format("%.0f%%",maxH),r+5,t+6,p);c.drawText(String.format("%.0f%%",minH),r+5,b,p);Path pt=new Path(),ph=new Path();for(int i=0;i<data.size();i++){HistoryDb.Reading x=data.get(i);float xx=l+(r-l)*(i/(float)(data.size()-1));float yt=b-(x.temp-minT)/(maxT-minT)*(b-t);float yh=b-(x.hum-minH)/(maxH-minH)*(b-t);if(i==0){pt.moveTo(xx,yt);ph.moveTo(xx,yh);}else{pt.lineTo(xx,yt);ph.lineTo(xx,yh);}}p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(4);p.setColor(0xffd32f2f);c.drawPath(pt,p);p.setColor(0xff1976d2);c.drawPath(ph,p);p.setStyle(Paint.Style.FILL);p.setTextSize(18);c.drawText("°C",l+10,t+25,p);p.setColor(0xff1976d2);c.drawText("% RH",l+55,t+25,p);long a=data.get(0).ts,z=data.get(data.size()-1).ts;p.setColor(0xff555555);c.drawText(new java.text.SimpleDateFormat("dd.MM HH:mm").format(new java.util.Date(a)),l,b+38,p);String end=new java.text.SimpleDateFormat("dd.MM HH:mm").format(new java.util.Date(z));float tw=p.measureText(end);c.drawText(end,r-tw,b+38,p);}
}
