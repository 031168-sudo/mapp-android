package com.minews1;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.View;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class GraphView extends View {
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    private List<HistoryDb.Reading> data;
    private int period=0; // 0 day, 1 month, 2 year
    private long from,to;
    private final SimpleDateFormat hm=new SimpleDateFormat("HH:mm",Locale.getDefault());
    private final SimpleDateFormat dm=new SimpleDateFormat("dd.MM",Locale.getDefault());
    private final SimpleDateFormat dmy=new SimpleDateFormat("dd.MM.yyyy",Locale.getDefault());

    public GraphView(Context c){super(c);setLayerType(View.LAYER_TYPE_SOFTWARE,null);}
    public void setData(List<HistoryDb.Reading> d,long f,long t,int pr){data=d;from=f;to=t;period=pr;invalidate();}

    private void text(Canvas c,String s,float x,float y,float size,int color){p.setStyle(Paint.Style.FILL);p.setColor(color);p.setTextSize(size);p.setTypeface(android.graphics.Typeface.DEFAULT);c.drawText(s,x,y,p);}
    private void round(Canvas c,float l,float t,float r,float b,int color){p.setStyle(Paint.Style.FILL);p.setColor(color);c.drawRoundRect(new RectF(l,t,r,b),20,20,p);}

    @Override protected void onDraw(Canvas c){
        super.onDraw(c); float w=getWidth(),h=getHeight(); c.drawColor(0xfff1f1f3);
        float margin=14, gap=12, top=8, bottom=8;
        float cardH=(h-top-bottom-gap)/2f;
        drawChart(c,margin,top,w-margin,top+cardH,"Температура",true);
        drawChart(c,margin,top+cardH+gap,w-margin,h-bottom,"Влажность",false);
    }

    private void drawChart(Canvas c,float cl,float ct,float cr,float cb,String title,boolean tempChart){
        round(c,cl,ct,cr,cb,0xffffffff);
        text(c,title,cl+18,ct+30,21,tempChart?0xff159b83:0xff3b78b5);
        float l=cl+18,r=cr-42,t=ct+55,b=cb-38;
        if(data==null||data.size()==0){text(c,"Нет данных за выбранный период",l,t+35,17,0xff888890);drawPeriodLabel(c,l,r,b);return;}
        float min=Float.MAX_VALUE,max=-Float.MAX_VALUE;
        for(HistoryDb.Reading x:data){float v=tempChart?x.temp:x.hum;min=Math.min(min,v);max=Math.max(max,v);}
        float pad=tempChart?Math.max(0.5f,(max-min)*0.15f):Math.max(1f,(max-min)*0.12f); if(max==min){pad=tempChart?1:2;} min-=pad;max+=pad;
        p.setStrokeWidth(1);p.setColor(0xffe3e3e6);p.setStyle(Paint.Style.STROKE);
        for(int i=0;i<=4;i++){float y=t+(b-t)*i/4f;c.drawLine(l,y,r,y,p);float val=max-(max-min)*i/4f;text(c,tempChart?String.format(Locale.getDefault(),"%.1f",val):String.format(Locale.getDefault(),"%.0f",val),r+7,y+5,12,0xff8a8a91);}
        Path path=new Path(); for(int i=0;i<data.size();i++){HistoryDb.Reading x=data.get(i);float xx=l+(r-l)*(i/(float)Math.max(1,data.size()-1));float v=tempChart?x.temp:x.hum;float yy=b-(v-min)/(max-min)*(b-t);if(i==0)path.moveTo(xx,yy);else path.lineTo(xx,yy);}
        int line=tempChart?0xff32bea5:0xff4b8fd1; p.setStyle(Paint.Style.FILL);p.setColor(tempChart?0x2232bea5:0x224b8fd1);Path fill=new Path(path);fill.lineTo(r,b);fill.lineTo(l,b);fill.close();c.drawPath(fill,p);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(3.5f);p.setColor(line);c.drawPath(path,p);
        HistoryDb.Reading last=data.get(data.size()-1);float lx=r;float lv=tempChart?last.temp:last.hum;float ly=b-(lv-min)/(max-min)*(b-t);p.setStyle(Paint.Style.FILL);p.setColor(0xffffffff);c.drawCircle(lx,ly,7,p);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(3);p.setColor(line);c.drawCircle(lx,ly,7,p);
        String value=tempChart?String.format(Locale.getDefault(),"%.2f °C",last.temp):String.format(Locale.getDefault(),"%.1f %%",last.hum);float bw=p.measureText(value)+26;float bx=Math.max(l,Math.min(r-bw,lx-bw+10));float by=Math.max(ct+36,ly-48);round(c,bx,by,bx+bw,by+38,line);text(c,value,bx+13,by+25,16,0xffffffff);
        drawPeriodLabel(c,l,r,b);
    }

    private void drawPeriodLabel(Canvas c,float l,float r,float b){
        String a,btxt;
        if(period==0){a=hm.format(new Date(from));btxt=hm.format(new Date(to-1));}
        else if(period==1){a=dm.format(new Date(from));btxt=dm.format(new Date(to-1));}
        else {a=dmy.format(new Date(from));btxt=dmy.format(new Date(to-1));}
        text(c,a,l,b+25,12,0xff99999f);float tw=p.measureText(btxt);text(c,btxt,r-tw,b+25,12,0xff99999f);
    }
}
