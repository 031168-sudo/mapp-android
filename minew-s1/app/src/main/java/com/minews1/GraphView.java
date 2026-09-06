package com.minews1;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class GraphView extends View {
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final HistoryDb db;
    private int period = 0; // 0 day, 1 month, 2 year
    private Calendar selected = Calendar.getInstance();
    private List<HistoryDb.Reading> data = new ArrayList<>();
    private long from, to;
    private final SimpleDateFormat dayTitle = new SimpleDateFormat("dd MMMM yyyy", Locale.getDefault());
    private final SimpleDateFormat monthTitle = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
    private final SimpleDateFormat yearTitle = new SimpleDateFormat("yyyy", Locale.getDefault());
    private final SimpleDateFormat hm = new SimpleDateFormat("HH:mm", Locale.getDefault());

    public GraphView(Context c) {
        super(c);
        db = new HistoryDb(c.getApplicationContext());
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        selected.set(Calendar.HOUR_OF_DAY, 12);
        selected.set(Calendar.MINUTE, 0);
        selected.set(Calendar.SECOND, 0);
        selected.set(Calendar.MILLISECOND, 0);
        reload();
    }

    // Compatibility with MainActivity from the existing project.
    public void setData(List<HistoryDb.Reading> ignored) { reload(); invalidate(); }
    public void setData(List<HistoryDb.Reading> ignored, long f, long t, int pr) {
        from = f; to = t; period = pr; data = ignored == null ? new ArrayList<>() : ignored; invalidate();
    }

    private void reload() {
        Calendar start = (Calendar) selected.clone();
        Calendar end = (Calendar) selected.clone();
        if (period == 0) {
            start.set(Calendar.HOUR_OF_DAY, 0); start.set(Calendar.MINUTE, 0); start.set(Calendar.SECOND, 0); start.set(Calendar.MILLISECOND, 0);
            end.add(Calendar.DAY_OF_MONTH, 1);
        } else if (period == 1) {
            start.set(Calendar.DAY_OF_MONTH, 1); start.set(Calendar.HOUR_OF_DAY, 0); start.set(Calendar.MINUTE, 0); start.set(Calendar.SECOND, 0); start.set(Calendar.MILLISECOND, 0);
            end = (Calendar) start.clone(); end.add(Calendar.MONTH, 1);
        } else {
            start.set(Calendar.MONTH, Calendar.JANUARY); start.set(Calendar.DAY_OF_MONTH, 1); start.set(Calendar.HOUR_OF_DAY, 0); start.set(Calendar.MINUTE, 0); start.set(Calendar.SECOND, 0); start.set(Calendar.MILLISECOND, 0);
            end = (Calendar) start.clone(); end.add(Calendar.YEAR, 1);
        }
        from = start.getTimeInMillis(); to = end.getTimeInMillis();
        List<HistoryDb.Reading> raw = db.since(from);
        List<HistoryDb.Reading> filtered = new ArrayList<>();
        for (HistoryDb.Reading r : raw) if (r.ts < to) filtered.add(r);
        if (period == 0) data = filtered;
        else data = aggregate(filtered, period == 1 ? Calendar.DAY_OF_MONTH : Calendar.MONTH);
    }

    private List<HistoryDb.Reading> aggregate(List<HistoryDb.Reading> src, int field) {
        List<HistoryDb.Reading> out = new ArrayList<>();
        if (src.isEmpty()) return out;
        Calendar c = Calendar.getInstance();
        int key = Integer.MIN_VALUE; double st = 0, sh = 0; int n = 0; long firstTs = 0;
        for (HistoryDb.Reading r : src) {
            c.setTimeInMillis(r.ts);
            int k = field == Calendar.DAY_OF_MONTH ? c.get(Calendar.DAY_OF_MONTH) : c.get(Calendar.MONTH);
            if (k != key && n > 0) { out.add(new HistoryDb.Reading(firstTs, (float)(st/n), (float)(sh/n))); st=sh=0; n=0; }
            if (k != key) { key=k; firstTs=r.ts; }
            st += r.temp; sh += r.hum; n++;
        }
        if (n > 0) out.add(new HistoryDb.Reading(firstTs, (float)(st/n), (float)(sh/n)));
        return out;
    }

    private void txt(Canvas c, String s, float x, float y, float size, int color) {
        p.setStyle(Paint.Style.FILL); p.setColor(color); p.setTextSize(size); p.setTypeface(android.graphics.Typeface.DEFAULT); c.drawText(s, x, y, p);
    }
    private void round(Canvas c, float l, float t, float r, float b, int color, float rad) {
        p.setStyle(Paint.Style.FILL); p.setColor(color); c.drawRoundRect(new RectF(l,t,r,b),rad,rad,p);
    }

    @Override protected void onDraw(Canvas c) {
        super.onDraw(c);
        float w=getWidth(), h=getHeight();
        c.drawColor(0xfff1f1f3);
        drawHeader(c,w);
        float top=94, bottom=h-54, gap=12, cardH=(bottom-top-gap)/2f;
        drawChart(c,14,top,w-14,top+cardH,"Температура",true);
        drawChart(c,14,top+cardH+gap,w-14,bottom,"Влажность",false);
        drawDateNav(c,w,h);
    }

    private void drawHeader(Canvas c,float w) {
        txt(c,"История измерений",18,28,24,0xff24242a);
        String title = period==0 ? dayTitle.format(selected.getTime()) : period==1 ? monthTitle.format(selected.getTime()) : yearTitle.format(selected.getTime());
        float tw; p.setTextSize(17); tw=p.measureText(title); txt(c,title,(w-tw)/2,56,17,0xff3c3c43);
        String[] tabs={"День","Месяц","Год"};
        float total=w-28, bw=total/3f;
        for(int i=0;i<3;i++){
            float l=14+i*bw,r=l+bw-4;
            round(c,l,66,r,91,i==period?0xff2f7d70:0xffffffff,12);
            p.setTextSize(14); tw=p.measureText(tabs[i]); txt(c,tabs[i],l+(r-l-tw)/2,83,14,i==period?0xffffffff:0xff66666e);
        }
    }

    private void drawChart(Canvas c,float cl,float ct,float cr,float cb,String title,boolean tempChart) {
        round(c,cl,ct,cr,cb,0xffffffff,18);
        txt(c,title,cl+16,ct+28,19,tempChart?0xff159b83:0xff3b78b5);
        float l=cl+18,r=cr-48,t=ct+48,b=cb-28;
        if(data==null||data.size()<1){txt(c,"Нет данных за выбранный период",l,t+30,16,0xff888890);return;}
        float min=Float.MAX_VALUE,max=-Float.MAX_VALUE;
        for(HistoryDb.Reading x:data){float v=tempChart?x.temp:x.hum;min=Math.min(min,v);max=Math.max(max,v);}
        float pad=tempChart?Math.max(.5f,(max-min)*.15f):Math.max(1f,(max-min)*.12f); if(max==min)pad=tempChart?1:2; min-=pad;max+=pad;
        p.setStrokeWidth(1);p.setColor(0xffe5e5e8);p.setStyle(Paint.Style.STROKE);
        for(int i=0;i<=4;i++){float y=t+(b-t)*i/4f;c.drawLine(l,y,r,y,p);float val=max-(max-min)*i/4f;txt(c,tempChart?String.format(Locale.getDefault(),"%.1f",val):String.format(Locale.getDefault(),"%.0f",val),r+7,y+4,11,0xff8a8a91);}
        Path path=new Path();
        for(int i=0;i<data.size();i++){HistoryDb.Reading x=data.get(i);float xx=l+(r-l)*(i/(float)Math.max(1,data.size()-1));float v=tempChart?x.temp:x.hum;float yy=b-(v-min)/(max-min)*(b-t);if(i==0)path.moveTo(xx,yy);else path.lineTo(xx,yy);}
        int line=tempChart?0xff32bea5:0xff4b8fd1;
        p.setStyle(Paint.Style.FILL);p.setColor(tempChart?0x2632bea5:0x264b8fd1);Path fill=new Path(path);fill.lineTo(r,b);fill.lineTo(l,b);fill.close();c.drawPath(fill,p);
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(3.2f);p.setColor(line);c.drawPath(path,p);
        HistoryDb.Reading last=data.get(data.size()-1);float lv=tempChart?last.temp:last.hum;float ly=b-(lv-min)/(max-min)*(b-t);
        p.setStyle(Paint.Style.FILL);p.setColor(0xffffffff);c.drawCircle(r,ly,6,p);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2.5f);p.setColor(line);c.drawCircle(r,ly,6,p);
        String value=tempChart?String.format(Locale.getDefault(),"%.2f °C",last.temp):String.format(Locale.getDefault(),"%.1f %%",last.hum);
        p.setStyle(Paint.Style.FILL);p.setTextSize(14);float bw=p.measureText(value)+22;float bx=Math.max(l,Math.min(r-bw,r-bw+4));float by=Math.max(ct+34,Math.min(cb-40,ly-38));round(c,bx,by,bx+bw,by+32,line,12);txt(c,value,bx+11,by+21,14,0xffffffff);
        if(period==0){txt(c,"00:00",l,b+20,11,0xff99999f);txt(c,"12:00",(l+r)/2-16,b+20,11,0xff99999f);txt(c,"24:00",r-26,b+20,11,0xff99999f);}
        else if(period==1){txt(c,"1",l,b+20,11,0xff99999f);txt(c,"15",(l+r)/2-7,b+20,11,0xff99999f);txt(c,"31",r-12,b+20,11,0xff99999f);}
        else {txt(c,"Янв",l,b+20,11,0xff99999f);txt(c,"Июн",(l+r)/2-12,b+20,11,0xff99999f);txt(c,"Дек",r-22,b+20,11,0xff99999f);}
    }

    private void drawDateNav(Canvas c,float w,float h){
        float y=h-46;
        round(c,14,y,w-14,h-8,0xffffffff,16);
        txt(c,"‹",31,y+26,29,0xff44444b);
        txt(c,"›",w-45,y+26,29,0xff44444b);
        String title=period==0?dayTitle.format(selected.getTime()):period==1?monthTitle.format(selected.getTime()):yearTitle.format(selected.getTime());
        p.setTextSize(16);float tw=p.measureText(title);txt(c,title,(w-tw)/2,y+25,16,0xff303038);
    }

    @Override public boolean onTouchEvent(MotionEvent e){
        if(e.getAction()!=MotionEvent.ACTION_UP)return true;
        float x=e.getX(),y=e.getY(),w=getWidth(),h=getHeight();
        if(y>=66&&y<=92){int idx=(int)((x-14)/((w-28)/3f));if(idx>=0&&idx<3){period=idx;reload();invalidate();}return true;}
        if(y>=h-55){Calendar c=(Calendar)selected.clone();if(x<w/2){if(period==0)c.add(Calendar.DAY_OF_MONTH,-1);else if(period==1)c.add(Calendar.MONTH,-1);else c.add(Calendar.YEAR,-1);}else{if(period==0)c.add(Calendar.DAY_OF_MONTH,1);else if(period==1)c.add(Calendar.MONTH,1);else c.add(Calendar.YEAR,1);}selected=c;reload();invalidate();return true;}
        return true;
    }
}
