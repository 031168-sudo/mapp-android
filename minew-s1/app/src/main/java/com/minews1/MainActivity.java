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

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private final Runnable restartScan = new Runnable() {
        @Override public void run() {
            if (!screenIsHistory) {
                stopScan();
                startScan();
                main.postDelayed(this, 12000);
            }
        }
    };

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        db = new HistoryDb(this);
        showMain();
        if (needPermissions()) {
            requestPermissions(new String[]{Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT}, 10);
        } else {
            startMonitoring();
        }
    }

    private boolean needPermissions() {
        return Build.VERSION.SDK_INT >= 31 &&
                (checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED ||
                 checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED);
    }

    @Override public void onRequestPermissionsResult(int r, String[] p, int[] g) {
        super.onRequestPermissionsResult(r, p, g);
        if (r == 10 && !needPermissions()) startMonitoring();
        else if (r == 10) setStatus("Разрешение Bluetooth не выдано");
    }

    private TextView text(String s, float size) {
        TextView v = new TextView(this);
        v.setText(s);
        v.setTextSize(size);
        v.setGravity(Gravity.CENTER);
        v.setTextColor(Color.rgb(45,45,50));
        return v;
    }

    private TextView label(String s) {
        TextView v = text(s, 15);
        v.setTextColor(Color.rgb(105,105,115));
        return v;
    }

    private Button button(String s) {
        Button b = new Button(this);
        b.setText(s);
        b.setTextSize(14);
        b.setAllCaps(false);
        return b;
    }

    private GradientDrawable card(int color) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(18));
        return g;
    }

    private void applyInsets(final View v) {
        if (Build.VERSION.SDK_INT >= 35) {
            v.setOnApplyWindowInsetsListener((view, insets) -> {
                android.graphics.Insets i = insets.getInsets(WindowInsets.Type.systemBars());
                view.setPadding(dp(18), dp(14) + i.top, dp(18), dp(14) + i.bottom);
                return insets;
            });
            v.requestApplyInsets();
        }
    }

    private void showMain() {
        screenIsHistory = false;
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(dp(18), dp(14), dp(18), dp(14));
        applyInsets(root);
        scroll.addView(root, new ScrollView.LayoutParams(-1, -2));

        TextView title = text("Minew S1", 30);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        root.addView(title, new LinearLayout.LayoutParams(-1, dp(46)));

        TextView sub = label("Датчик температуры и влажности");
        root.addView(sub, new LinearLayout.LayoutParams(-1, dp(28)));

        LinearLayout tc = new LinearLayout(this);
        tc.setOrientation(LinearLayout.VERTICAL);
        tc.setGravity(Gravity.CENTER);
        tc.setBackground(card(Color.rgb(244,248,255)));
        tc.setPadding(dp(12), dp(8), dp(12), dp(8));
        TextView tl = label("ТЕМПЕРАТУРА");
        tl.setTextColor(Color.rgb(55,105,170));
        temp = text("—.— °C", 48);
        temp.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        tc.addView(tl, new LinearLayout.LayoutParams(-1, dp(28)));
        tc.addView(temp, new LinearLayout.LayoutParams(-1, dp(64)));
        root.addView(tc, new LinearLayout.LayoutParams(-1, dp(116)));

        LinearLayout hc = new LinearLayout(this);
        hc.setOrientation(LinearLayout.VERTICAL);
        hc.setGravity(Gravity.CENTER);
        hc.setBackground(card(Color.rgb(244,251,247)));
        hc.setPadding(dp(12), dp(8), dp(12), dp(8));
        TextView hl = label("ВЛАЖНОСТЬ");
        hl.setTextColor(Color.rgb(50,125,85));
        hum = text("—.— % RH", 48);
        hum.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        hc.addView(hl, new LinearLayout.LayoutParams(-1, dp(28)));
        hc.addView(hum, new LinearLayout.LayoutParams(-1, dp(64)));
        LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(-1, dp(116));
        hp.topMargin = dp(12);
        root.addView(hc, hp);

        LinearLayout statusCard = new LinearLayout(this);
        statusCard.setOrientation(LinearLayout.VERTICAL);
        statusCard.setGravity(Gravity.CENTER);
        statusCard.setBackground(card(Color.rgb(248,248,250)));
        statusCard.setPadding(dp(10), dp(5), dp(10), dp(5));
        liveStatus = text("Поиск датчика…", 16);
        liveStatus.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        lastTime = label("Ожидание первого пакета");
        packetCount = label("Пакетов: 0");
        statusCard.addView(liveStatus, new LinearLayout.LayoutParams(-1, dp(28)));
        statusCard.addView(lastTime, new LinearLayout.LayoutParams(-1, dp(24)));
        statusCard.addView(packetCount, new LinearLayout.LayoutParams(-1, dp(24)));
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(-1, dp(86));
        sp.topMargin = dp(12);
        root.addView(statusCard, sp);

        Button history = button("История измерений  →");
        history.setTextSize(17);
        history.setOnClickListener(v -> showHistory(24));
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(-1, dp(56));
        bp.topMargin = dp(12);
        root.addView(history, bp);

        TextView id = label("MAC  " + MAC);
        id.setTextSize(13);
        root.addView(id, new LinearLayout.LayoutParams(-1, dp(30)));
        setContentView(scroll);
    }

    private void showHistory(long hours) {
        screenIsHistory = true;
        stopScan();
        main.removeCallbacks(restartScan);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(12), dp(10), dp(12), dp(8));
        applyInsets(root);

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        Button back = button("←");
        back.setTextSize(24);
        back.setOnClickListener(v -> { showMain(); startMonitoring(); });
        top.addView(back, new LinearLayout.LayoutParams(dp(58), dp(54)));
        TextView title = text("История", 25);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setGravity(Gravity.CENTER_VERTICAL);
        top.addView(title, new LinearLayout.LayoutParams(0, dp(54), 1));
        root.addView(top, new LinearLayout.LayoutParams(-1, dp(54)));

        LinearLayout bar = new LinearLayout(this);
        bar.setGravity(Gravity.CENTER);
        String[] labels = {"1 ч", "6 ч", "24 ч", "7 дней"};
        long[] vals = {1,6,24,168};
        for (int i=0;i<labels.length;i++) {
            final long x=vals[i];
            Button q=button(labels[i]);
            q.setOnClickListener(v->loadGraph(x));
            bar.addView(q,new LinearLayout.LayoutParams(0,dp(50),1));
        }
        root.addView(bar,new LinearLayout.LayoutParams(-1,dp(54)));

        GraphView graph = new GraphView(this);
        root.addView(graph,new LinearLayout.LayoutParams(-1,0,1));
        setContentView(root);
        loadGraph(hours);
    }

    private void loadGraph(long hours) {
        View content = ((android.view.ViewGroup)findViewById(android.R.id.content)).getChildAt(0);
        if (content instanceof android.view.ViewGroup) {
            android.view.ViewGroup root=(android.view.ViewGroup)content;
            for(int i=0;i<root.getChildCount();i++) if(root.getChildAt(i) instanceof GraphView) {
                List<HistoryDb.Reading> data=db.since(System.currentTimeMillis()-hours*3600000L);
                ((GraphView)root.getChildAt(i)).setData(data);
                break;
            }
        }
    }

    private void startMonitoring() {
        packets = 0;
        startScan();
        main.removeCallbacks(restartScan);
        main.postDelayed(restartScan, 12000);
    }

    private void startScan() {
        if (screenIsHistory || needPermissions()) return;
        BluetoothManager bm=(BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter a=bm.getAdapter();
        if(a==null||!a.isEnabled()){setStatus("Включите Bluetooth");return;}
        scanner=a.getBluetoothLeScanner();
        if(scanner==null){setStatus("BLE недоступен");return;}
        callback=new ScanCallback(){
            @Override public void onScanResult(int type, ScanResult res){ parse(res); }
            @Override public void onScanFailed(int errorCode){ main.post(()->setStatus("Ошибка BLE-сканирования: "+errorCode)); }
        };
        try {
            scanner.startScan(null,new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).setReportDelay(0).build(),callback);
        } catch(SecurityException e){setStatus("Нет разрешения Bluetooth");}
    }

    private void stopScan(){
        if(scanner!=null&&callback!=null){
            try{scanner.stopScan(callback);}catch(SecurityException ignored){}
            callback=null;
        }
    }

    private void parse(ScanResult res) {
        if(res.getScanRecord()==null)return;
        byte[] d=res.getScanRecord().getServiceData(UUID);
        if(d==null||d.length<13||(d[0]&255)!=0xA1||(d[1]&255)!=0x01)return;
        int tr=(short)(((d[3]&255)<<8)|(d[4]&255));
        int hr=(short)(((d[5]&255)<<8)|(d[6]&255));
        float t=tr/256f,h=hr/256f;
        String embedded=String.format(Locale.US,"%02X:%02X:%02X:%02X:%02X:%02X",d[12]&255,d[11]&255,d[10]&255,d[9]&255,d[8]&255,d[7]&255);
        String addr="";
        try{addr=res.getDevice().getAddress();}catch(SecurityException ignored){}
        if(!MAC.equalsIgnoreCase(addr)&&!MAC.equalsIgnoreCase(embedded))return;
        main.post(()->update(t,h));
    }

    private void update(float t,float h){
        packets++;
        temp.setText(String.format(Locale.US,"%.2f °C",t));
        hum.setText(String.format(Locale.US,"%.1f %% RH",h));
        liveStatus.setText("●  Датчик подключён по BLE");
        liveStatus.setTextColor(Color.rgb(40,130,75));
        lastTime.setText("Последний пакет: "+new SimpleDateFormat("HH:mm:ss",Locale.getDefault()).format(new Date()));
        packetCount.setText("Пакетов получено: "+packets);
        long now=System.currentTimeMillis();
        if(now-lastSave>=10000||Float.isNaN(lastT)||Math.abs(t-lastT)>=0.05f||Math.abs(h-lastH)>=0.05f){
            db.add(now,t,h);
            lastSave=now;
            lastT=t;
            lastH=h;
        }
    }

    private void setStatus(String s){
        if(liveStatus!=null)liveStatus.setText(s);
    }

    @Override protected void onPause(){
        super.onPause();
        stopScan();
        main.removeCallbacks(restartScan);
    }

    @Override protected void onResume(){
        super.onResume();
        if(!screenIsHistory&&temp!=null){
            if(needPermissions())return;
            main.postDelayed(()->{if(!screenIsHistory)startMonitoring();},150);
        }
    }
}
