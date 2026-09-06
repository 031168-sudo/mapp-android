package com.minews1;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelUuid;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final String MAC = "78:05:41:06:91:BA";
    private static final ParcelUuid UUID = ParcelUuid.fromString("0000ffe1-0000-1000-8000-00805f9b34fb");
    private final Handler main = new Handler(Looper.getMainLooper());
    private BluetoothLeScanner scanner;
    private ScanCallback callback;
    private HistoryDb db;
    private TextView temp, hum, liveStatus, lastTime, packetCount;
    private long lastSave = 0;
    private float lastT = Float.NaN, lastH = Float.NaN;
    private int packets = 0;
    private boolean screenIsHistory = false;
    private int historyPeriod = 0;
    private final Calendar historyDate = Calendar.getInstance();
    private TextView historyDateLabel;
    private GraphView historyGraph;
    private Button dayButton, monthButton, yearButton;

    private int dp(float value) { return Math.round(value * getResources().getDisplayMetrics().density); }

    private final Runnable restartScan = new Runnable() {
        @Override public void run() {
            if (!screenIsHistory) { stopScan(); startScan(); main.postDelayed(this, 12000); }
        }
    };

    @Override public void onCreate(Bundle b) {
        super.onCreate(b); db = new HistoryDb(this); showMain();
        if (needPermissions()) requestPermissions(new String[]{Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT},10); else startMonitoring();
    }
    private boolean needPermissions() {
        return Build.VERSION.SDK_INT >= 31 && (checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN)!=PackageManager.PERMISSION_GRANTED || checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)!=PackageManager.PERMISSION_GRANTED);
    }
    @Override public void onRequestPermissionsResult(int r,String[] p,int[] g){super.onRequestPermissionsResult(r,p,g);if(r==10&&!needPermissions())startMonitoring();else if(r==10)setStatus("Разрешение Bluetooth не выдано");}

    private TextView text(String s,float size){TextView v=new TextView(this);v.setText(s);v.setTextSize(size);v.setGravity(Gravity.CENTER);v.setTextColor(Color.rgb(45,45,50));return v;}
    private TextView label(String s){TextView v=text(s,15);v.setTextColor(Color.rgb(105,105,115));return v;}
    private Button button(String s){Button b=new Button(this);b.setText(s);b.setTextSize(14);b.setAllCaps(false);return b;}
    private GradientDrawable card(int color){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(dp(18));return g;}
    private void applyInsets(final View v){if(Build.VERSION.SDK_INT>=35){v.setOnApplyWindowInsetsListener((view,insets)->{android.graphics.Insets i=insets.getInsets(WindowInsets.Type.systemBars());view.setPadding(dp(18),dp(14)+i.top,dp(18),dp(14)+i.bottom);return insets;});v.requestApplyInsets();}}

    private void showMain(){
        screenIsHistory=false; ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setGravity(Gravity.CENTER_HORIZONTAL);root.setPadding(dp(18),dp(14),dp(18),dp(14));applyInsets(root);scroll.addView(root,new ScrollView.LayoutParams(-1,-2));
        TextView title=text("Minew S1",30);title.setTypeface(Typeface.DEFAULT,Typeface.BOLD);root.addView(title,new LinearLayout.LayoutParams(-1,dp(46)));
        root.addView(label("Датчик температуры и влажности"),new LinearLayout.LayoutParams(-1,dp(28)));
        LinearLayout tc=new LinearLayout(this);tc.setOrientation(LinearLayout.VERTICAL);tc.setGravity(Gravity.CENTER);tc.setBackground(card(Color.rgb(244,248,255)));tc.setPadding(dp(12),dp(8),dp(12),dp(8));TextView tl=label("ТЕМПЕРАТУРА");tl.setTextColor(Color.rgb(55,105,170));temp=text("—.— °C",48);temp.setTypeface(Typeface.DEFAULT,Typeface.BOLD);tc.addView(tl,new LinearLayout.LayoutParams(-1,dp(28)));tc.addView(temp,new LinearLayout.LayoutParams(-1,dp(64)));root.addView(tc,new LinearLayout.LayoutParams(-1,dp(116)));
        LinearLayout hc=new LinearLayout(this);hc.setOrientation(LinearLayout.VERTICAL);hc.setGravity(Gravity.CENTER);hc.setBackground(card(Color.rgb(244,251,247)));hc.setPadding(dp(12),dp(8),dp(12),dp(8));TextView hl=label("ВЛАЖНОСТЬ");hl.setTextColor(Color.rgb(50,125,85));hum=text("—.— % RH",48);hum.setTypeface(Typeface.DEFAULT,Typeface.BOLD);hc.addView(hl,new LinearLayout.LayoutParams(-1,dp(28)));hc.addView(hum,new LinearLayout.LayoutParams(-1,dp(64)));LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(-1,dp(116));hp.topMargin=dp(12);root.addView(hc,hp);
        LinearLayout statusCard=new LinearLayout(this);statusCard.setOrientation(LinearLayout.VERTICAL);statusCard.setGravity(Gravity.CENTER);statusCard.setBackground(card(Color.rgb(248,248,250)));statusCard.setPadding(dp(10),dp(5),dp(10),dp(5));liveStatus=text("Поиск датчика…",16);liveStatus.setTypeface(Typeface.DEFAULT,Typeface.BOLD);lastTime=label("Ожидание первого пакета");packetCount=label("Пакетов: 0");statusCard.addView(liveStatus,new LinearLayout.LayoutParams(-1,dp(28)));statusCard.addView(lastTime,new LinearLayout.LayoutParams(-1,dp(24)));statusCard.addView(packetCount,new LinearLayout.LayoutParams(-1,dp(24)));LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,dp(86));sp.topMargin=dp(12);root.addView(statusCard,sp);
        Button history=button("История измерений  →");history.setTextSize(17);history.setOnClickListener(v->showHistory());LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(-1,dp(56));bp.topMargin=dp(12);root.addView(history,bp);
        TextView id=label("MAC  "+MAC);id.setTextSize(13);root.addView(id,new LinearLayout.LayoutParams(-1,dp(30)));setContentView(scroll);
    }

    private void showHistory(){
        screenIsHistory=true;stopScan();main.removeCallbacks(restartScan);historyDate.setTimeInMillis(System.currentTimeMillis());
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(Color.rgb(242,242,244));root.setPadding(dp(10),dp(8),dp(10),dp(8));applyInsets(root);
        LinearLayout top=new LinearLayout(this);top.setGravity(Gravity.CENTER_VERTICAL);Button back=button("←");back.setTextSize(28);back.setOnClickListener(v->{showMain();startMonitoring();});top.addView(back,new LinearLayout.LayoutParams(dp(54),dp(52)));TextView title=text("График данных",24);title.setTypeface(Typeface.DEFAULT,Typeface.BOLD);top.addView(title,new LinearLayout.LayoutParams(0,dp(52),1));TextView edit=label("⋮");edit.setTextSize(28);top.addView(edit,new LinearLayout.LayoutParams(dp(40),dp(52)));root.addView(top,new LinearLayout.LayoutParams(-1,dp(52)));
        LinearLayout tabs=new LinearLayout(this);tabs.setGravity(Gravity.CENTER);dayButton=button("День");monthButton=button("Месяц");yearButton=button("Год");tabs.addView(dayButton,new LinearLayout.LayoutParams(0,dp(48),1));tabs.addView(monthButton,new LinearLayout.LayoutParams(0,dp(48),1));tabs.addView(yearButton,new LinearLayout.LayoutParams(0,dp(48),1));dayButton.setOnClickListener(v->{historyPeriod=0;refreshHistory();});monthButton.setOnClickListener(v->{historyPeriod=1;refreshHistory();});yearButton.setOnClickListener(v->{historyPeriod=2;refreshHistory();});root.addView(tabs,new LinearLayout.LayoutParams(-1,dp(54)));
        historyGraph=new GraphView(this);LinearLayout.LayoutParams gp=new LinearLayout.LayoutParams(-1,0,1);gp.topMargin=dp(8);root.addView(historyGraph,gp);
        LinearLayout nav=new LinearLayout(this);nav.setGravity(Gravity.CENTER_VERTICAL);nav.setBackground(card(Color.WHITE));Button prev=button("◀");Button next=button("▶");historyDateLabel=text("",19);nav.addView(prev,new LinearLayout.LayoutParams(dp(58),dp(58)));nav.addView(historyDateLabel,new LinearLayout.LayoutParams(0,dp(58),1));nav.addView(next,new LinearLayout.LayoutParams(dp(58),dp(58)));LinearLayout.LayoutParams np=new LinearLayout.LayoutParams(-1,dp(66));np.topMargin=dp(8);root.addView(nav,np);prev.setOnClickListener(v->{historyDate.add(historyPeriod==0?Calendar.DAY_OF_MONTH:historyPeriod==1?Calendar.MONTH:Calendar.YEAR,-1);refreshHistory();});next.setOnClickListener(v->{historyDate.add(historyPeriod==0?Calendar.DAY_OF_MONTH:historyPeriod==1?Calendar.MONTH:Calendar.YEAR,1);refreshHistory();});
        setContentView(root);refreshHistory();
    }

    private void setTab(Button b,boolean selected){b.setTextColor(selected?Color.WHITE:Color.rgb(80,80,88));GradientDrawable g=new GradientDrawable();g.setColor(selected?Color.rgb(50,190,165):Color.TRANSPARENT);g.setCornerRadius(dp(22));b.setBackground(g);}
    private void refreshHistory(){
        setTab(dayButton,historyPeriod==0);setTab(monthButton,historyPeriod==1);setTab(yearButton,historyPeriod==2);
        Calendar start=(Calendar)historyDate.clone();Calendar end=(Calendar)historyDate.clone();String title;
        if(historyPeriod==0){start.set(Calendar.HOUR_OF_DAY,0);start.set(Calendar.MINUTE,0);start.set(Calendar.SECOND,0);start.set(Calendar.MILLISECOND,0);end=(Calendar)start.clone();end.add(Calendar.DAY_OF_MONTH,1);title=new SimpleDateFormat("dd.MM.yyyy",Locale.getDefault()).format(start.getTime());}
        else if(historyPeriod==1){start.set(Calendar.DAY_OF_MONTH,1);start.set(Calendar.HOUR_OF_DAY,0);start.set(Calendar.MINUTE,0);start.set(Calendar.SECOND,0);start.set(Calendar.MILLISECOND,0);end=(Calendar)start.clone();end.add(Calendar.MONTH,1);title=new SimpleDateFormat("LLLL yyyy",Locale.getDefault()).format(start.getTime());}
        else{start.set(Calendar.MONTH,Calendar.JANUARY);start.set(Calendar.DAY_OF_MONTH,1);start.set(Calendar.HOUR_OF_DAY,0);start.set(Calendar.MINUTE,0);start.set(Calendar.SECOND,0);start.set(Calendar.MILLISECOND,0);end=(Calendar)start.clone();end.add(Calendar.YEAR,1);title=new SimpleDateFormat("yyyy",Locale.getDefault()).format(start.getTime());}
        title=title.substring(0,1).toUpperCase(Locale.getDefault())+title.substring(1);historyDateLabel.setText(title);
        List<HistoryDb.Reading> data=db.range(start.getTimeInMillis(),end.getTimeInMillis());historyGraph.setData(data,start.getTimeInMillis(),end.getTimeInMillis(),historyPeriod);
    }

    private void startMonitoring(){packets=0;startScan();main.removeCallbacks(restartScan);main.postDelayed(restartScan,12000);}
    private void startScan(){if(screenIsHistory||needPermissions())return;BluetoothManager bm=(BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);BluetoothAdapter a=bm.getAdapter();if(a==null||!a.isEnabled()){setStatus("Включите Bluetooth");return;}scanner=a.getBluetoothLeScanner();if(scanner==null){setStatus("BLE недоступен");return;}callback=new ScanCallback(){@Override public void onScanResult(int type,ScanResult res){parse(res);}@Override public void onScanFailed(int errorCode){main.post(()->setStatus("Ошибка BLE-сканирования: "+errorCode));}};try{scanner.startScan(null,new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).setReportDelay(0).build(),callback);}catch(SecurityException e){setStatus("Нет разрешения Bluetooth");}}
    private void stopScan(){if(scanner!=null&&callback!=null){try{scanner.stopScan(callback);}catch(SecurityException ignored){}callback=null;}}
    private void parse(ScanResult res){if(res.getScanRecord()==null)return;byte[] d=res.getScanRecord().getServiceData(UUID);if(d==null||d.length<13||(d[0]&255)!=0xA1||(d[1]&255)!=0x01)return;int tr=(short)(((d[3]&255)<<8)|(d[4]&255));int hr=(short)(((d[5]&255)<<8)|(d[6]&255));float t=tr/256f,h=hr/256f;String embedded=String.format(Locale.US,"%02X:%02X:%02X:%02X:%02X:%02X",d[12]&255,d[11]&255,d[10]&255,d[9]&255,d[8]&255,d[7]&255);String addr="";try{addr=res.getDevice().getAddress();}catch(SecurityException ignored){}if(!MAC.equalsIgnoreCase(addr)&&!MAC.equalsIgnoreCase(embedded))return;main.post(()->update(t,h));}
    private void update(float t,float h){packets++;temp.setText(String.format(Locale.US,"%.2f °C",t));hum.setText(String.format(Locale.US,"%.1f %% RH",h));liveStatus.setText("●  Датчик подключён по BLE");liveStatus.setTextColor(Color.rgb(40,130,75));lastTime.setText("Последний пакет: "+new SimpleDateFormat("HH:mm:ss",Locale.getDefault()).format(new Date()));packetCount.setText("Пакетов получено: "+packets);long now=System.currentTimeMillis();if(now-lastSave>=10000||Float.isNaN(lastT)||Math.abs(t-lastT)>=0.05f||Math.abs(h-lastH)>=0.05f){db.add(now,t,h);lastSave=now;lastT=t;lastH=h;}}
    private void setStatus(String s){if(liveStatus!=null)liveStatus.setText(s);}
    @Override protected void onPause(){super.onPause();stopScan();main.removeCallbacks(restartScan);}
    @Override protected void onResume(){super.onResume();if(!screenIsHistory&&temp!=null){if(needPermissions())return;main.postDelayed(()->{if(!screenIsHistory)startMonitoring();},150);}}
}
