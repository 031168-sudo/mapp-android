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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends Activity {
    private static final String MINEW_MAC="78:05:41:06:91:BA";
    private static final ParcelUuid MINEW_UUID=ParcelUuid.fromString("0000ffe1-0000-1000-8000-00805f9b34fb");
    private static final ParcelUuid XIAOMI_UUID=ParcelUuid.fromString("0000fe95-0000-1000-8000-00805f9b34fb");
    private final Handler main=new Handler(Looper.getMainLooper());
    private final Map<String,SensorState> states=new HashMap<>();
    private BluetoothLeScanner scanner; private ScanCallback callback; private HistoryDb db; private DeviceDb deviceDb;
    private LinearLayout deviceCards; private TextView mainStatus; private boolean screenIsHistory=false; private int historyPeriod=0;
    private java.util.Calendar historyDate=java.util.Calendar.getInstance(); private TextView historyDateLabel; private GraphView historyGraph; private String historyDeviceId=MINEW_MAC;
    private Button dayButton,monthButton,yearButton;

    private static class SensorState { float t=Float.NaN,h=Float.NaN; int battery=-1; long last=0; int packets=0; }
    private static class Found { String mac,name,type; int rssi; Found(String m,String n,String t,int r){mac=m;name=n;type=t;rssi=r;} }
    private int dp(float v){return Math.round(v*getResources().getDisplayMetrics().density);}
    private TextView text(String s,float size){TextView v=new TextView(this);v.setText(s);v.setTextSize(size);v.setGravity(Gravity.CENTER);v.setTextColor(Color.rgb(45,45,50));return v;}
    private TextView label(String s){TextView v=text(s,14);v.setTextColor(Color.rgb(105,105,115));return v;}
    private Button button(String s){Button b=new Button(this);b.setText(s);b.setTextSize(14);b.setAllCaps(false);return b;}
    private GradientDrawable card(int color){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(dp(18));return g;}
    private void applyInsets(final View v){if(Build.VERSION.SDK_INT>=35){v.setOnApplyWindowInsetsListener((view,insets)->{android.graphics.Insets i=insets.getInsets(WindowInsets.Type.systemBars());view.setPadding(dp(18),dp(14)+i.top,dp(18),dp(14)+i.bottom);return insets;});v.requestApplyInsets();}}

    @Override public void onCreate(Bundle b){super.onCreate(b);db=new HistoryDb(this);deviceDb=new DeviceDb(this);deviceDb.ensureMinew();showMain();if(needPermissions())requestPermissions(new String[]{Manifest.permission.BLUETOOTH_SCAN,Manifest.permission.BLUETOOTH_CONNECT},10);else startMonitoring();}
    private boolean needPermissions(){return Build.VERSION.SDK_INT>=31&&(checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN)!=PackageManager.PERMISSION_GRANTED||checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)!=PackageManager.PERMISSION_GRANTED);}
    @Override public void onRequestPermissionsResult(int r,String[] p,int[] g){super.onRequestPermissionsResult(r,p,g);if(r==10&&!needPermissions())startMonitoring();else if(r==10)setMainStatus("Разрешение Bluetooth не выдано");}

    private void showMain(){
        screenIsHistory=false; ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setGravity(Gravity.CENTER_HORIZONTAL);root.setPadding(dp(18),dp(14),dp(18),dp(14));applyInsets(root);scroll.addView(root,new ScrollView.LayoutParams(-1,-2));
        TextView title=text("Температура и влажность",28);title.setTypeface(Typeface.DEFAULT,Typeface.BOLD);root.addView(title,new LinearLayout.LayoutParams(-1,dp(46)));
        root.addView(label("BLE-мониторинг датчиков"),new LinearLayout.LayoutParams(-1,dp(28)));
        deviceCards=new LinearLayout(this);deviceCards.setOrientation(LinearLayout.VERTICAL);root.addView(deviceCards,new LinearLayout.LayoutParams(-1,-2));
        Button devices=button("Устройства  +");devices.setTextSize(17);devices.setOnClickListener(v->showDevices());LinearLayout.LayoutParams devicesLp=new LinearLayout.LayoutParams(-1,dp(56));devicesLp.topMargin=dp(12);root.addView(devices,devicesLp);
        Button history=button("История измерений  →");history.setTextSize(17);history.setOnClickListener(v->showHistory());LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(-1,dp(56));hp.topMargin=dp(10);root.addView(history,hp);
        mainStatus=label("Поиск датчиков…");mainStatus.setTextSize(14);LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,dp(32));sp.topMargin=dp(5);root.addView(mainStatus,sp);
        setContentView(scroll);renderDevices();
    }
    private void renderDevices(){if(deviceCards==null)return;deviceCards.removeAllViews();for(DeviceDb.Device d:deviceDb.all()){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setGravity(Gravity.CENTER);c.setPadding(dp(14),dp(8),dp(14),dp(8));c.setBackground(card(d.type.equals("xiaomi")?Color.rgb(248,250,255):Color.rgb(244,248,255)));TextView name=label(d.name);name.setTextSize(18);name.setTypeface(Typeface.DEFAULT,Typeface.BOLD);name.setTextColor(Color.rgb(55,80,115));c.addView(name,new LinearLayout.LayoutParams(-1,dp(27)));TextView values=text("—.— °C    —.— % RH",31);values.setTypeface(Typeface.DEFAULT,Typeface.BOLD);values.setTag(d.id);c.addView(values,new LinearLayout.LayoutParams(-1,dp(48)));TextView info=label(d.type.equals("xiaomi")?"Xiaomi LYWSDCGQ/01ZM  •  "+d.id:d.id);info.setTextSize(12);c.addView(info,new LinearLayout.LayoutParams(-1,dp(25)));TextView status=label("Ожидание данных");status.setTag("status:"+d.id);c.addView(status,new LinearLayout.LayoutParams(-1,dp(22)));c.setOnClickListener(v->showHistoryFor(d.id,d.name));LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,dp(135));cp.bottomMargin=dp(10);deviceCards.addView(c,cp);updateCard(d.id);}}
    private void updateCard(String id){if(deviceCards==null)return;SensorState s=states.get(id);if(s==null)return;for(int i=0;i<deviceCards.getChildCount();i++){View child=deviceCards.getChildAt(i);if(!(child instanceof LinearLayout))continue;LinearLayout c=(LinearLayout)child;TextView vals=(TextView)c.getChildAt(1),status=(TextView)c.getChildAt(3);if(!id.equals(vals.getTag()))continue;vals.setText(Float.isNaN(s.t)?"—.— °C    —.— % RH":String.format(Locale.US,"%.2f °C    %.1f %% RH",s.t,s.h));status.setText(s.last>0?"●  BLE • "+new SimpleDateFormat("HH:mm:ss",Locale.getDefault()).format(new Date(s.last))+(s.battery>=0?"  •  "+s.battery+"%":""):"Ожидание данных");status.setTextColor(s.last>0?Color.rgb(45,135,80):Color.rgb(110,110,115));}}
    private void setMainStatus(String s){if(mainStatus!=null)mainStatus.setText(s);}

    private void showDevices(){
        screenIsHistory=true;stopScan();main.removeCallbacksAndMessages(null);
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(Color.rgb(242,242,244));root.setPadding(dp(12),dp(8),dp(12),dp(8));applyInsets(root);
        LinearLayout top=new LinearLayout(this);top.setGravity(Gravity.CENTER_VERTICAL);Button back=button("←");back.setTextSize(28);back.setOnClickListener(v->{showMain();startMonitoring();});top.addView(back,new LinearLayout.LayoutParams(dp(54),dp(52)));TextView title=text("Добавление устройства",23);title.setTypeface(Typeface.DEFAULT,Typeface.BOLD);top.addView(title,new LinearLayout.LayoutParams(0,dp(52),1));root.addView(top,new LinearLayout.LayoutParams(-1,dp(52)));
        TextView hint=label("Нажмите «Сканировать». Приложение покажет найденные BLE-датчики. Выберите устройство и добавьте его.");hint.setGravity(Gravity.LEFT|Gravity.CENTER_VERTICAL);hint.setTextSize(15);root.addView(hint,new LinearLayout.LayoutParams(-1,dp(60)));
        Button scan=button("Сканировать BLE");scan.setTextSize(17);root.addView(scan,new LinearLayout.LayoutParams(-1,dp(54)));
        TextView status=label("Готово к сканированию");root.addView(status,new LinearLayout.LayoutParams(-1,dp(36)));
        ScrollView listScroll=new ScrollView(this);LinearLayout list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);listScroll.addView(list,new ScrollView.LayoutParams(-1,-2));root.addView(listScroll,new LinearLayout.LayoutParams(-1,0,1));setContentView(root);
        scan.setOnClickListener(v->{list.removeAllViews();status.setText("Сканирование…");startDeviceScan(list,status,scan);});
    }
    private void startDeviceScan(final LinearLayout list,final TextView status,final Button scanButton){
        if(needPermissions()){status.setText("Нет разрешения Bluetooth");return;}BluetoothManager bm=(BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);BluetoothAdapter a=bm.getAdapter();if(a==null||!a.isEnabled()){status.setText("Включите Bluetooth");return;}scanner=a.getBluetoothLeScanner();if(scanner==null){status.setText("BLE недоступен");return;}
        final LinkedHashMap<String,Found> found=new LinkedHashMap<>();callback=new ScanCallback(){@Override public void onScanResult(int type,ScanResult r){Found f=identify(r);if(f==null)return;Found old=found.get(f.mac);if(old==null||f.rssi>old.rssi){found.put(f.mac,f);main.post(()->renderFound(list,found));}}@Override public void onScanFailed(int e){main.post(()->status.setText("Ошибка BLE-сканирования: "+e));}};try{scanner.startScan(null,new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).setReportDelay(0).build(),callback);}catch(SecurityException e){status.setText("Нет разрешения Bluetooth");return;}scanButton.setEnabled(false);main.postDelayed(()->{stopScan();scanButton.setEnabled(true);status.setText("Найдено устройств: "+found.size());},10000);}
    private Found identify(ScanResult r){String scanMac;try{scanMac=r.getDevice().getAddress();}catch(SecurityException e){return null;}if(scanMac==null)return null;String name=r.getScanRecord()==null?null:r.getScanRecord().getDeviceName();if(name==null||name.trim().isEmpty())try{name=r.getDevice().getName();}catch(SecurityException ignored){}if(name==null)name="BLE устройство";byte[] xd=r.getScanRecord()==null?null:r.getScanRecord().getServiceData(XIAOMI_UUID);byte[] mine=r.getScanRecord()==null?null:r.getScanRecord().getServiceData(MINEW_UUID);if(mine!=null)return new Found(scanMac,name.toLowerCase(Locale.US).contains("minew")?name:"Minew S1","minew",r.getRssi());if(isXiaomi(xd)){String embedded=xiaomiMac(xd);if(embedded!=null)return new Found(embedded,"Xiaomi LYWSDCGQ/01ZM","xiaomi",r.getRssi());}return null;}
    private boolean isXiaomi(byte[] d){if(d==null||d.length<18)return false;if(u(d,0)!=0x50||u(d,1)!=0x20||u(d,2)!=0xaa||u(d,3)!=0x01)return false;int type=u(d,11);return type==0x0D||type==0x0A||type==0x06||type==0x04;}
    private String xiaomiMac(byte[] d){if(d==null||d.length<11)return null;return String.format(Locale.US,"%02X:%02X:%02X:%02X:%02X:%02X",u(d,5),u(d,6),u(d,7),u(d,8),u(d,9),u(d,10));}
    private int u(byte[] b,int i){return b[i]&255;}
    private void renderFound(LinearLayout list,LinkedHashMap<String,Found> found){list.removeAllViews();for(Found f:found.values()){LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.VERTICAL);row.setPadding(dp(12),dp(8),dp(8),dp(8));row.setBackground(card(Color.WHITE));TextView n=label(f.name);n.setTextSize(18);n.setTypeface(Typeface.DEFAULT,Typeface.BOLD);n.setGravity(Gravity.LEFT);row.addView(n,new LinearLayout.LayoutParams(-1,dp(28)));TextView m=label(f.type.equals("xiaomi")?"Xiaomi LYWSDCGQ/01ZM":"Minew S1");m.setGravity(Gravity.LEFT);row.addView(m,new LinearLayout.LayoutParams(-1,dp(23)));TextView addr=label(f.mac+"   "+f.rssi+" dBm");addr.setGravity(Gravity.LEFT);row.addView(addr,new LinearLayout.LayoutParams(0,dp(25),1));Button add=button(deviceDb.find(f.mac)!=null?"Добавлено":"Добавить");add.setTextSize(14);add.setEnabled(deviceDb.find(f.mac)==null);row.addView(add,new LinearLayout.LayoutParams(-1,dp(46)));LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(-1,dp(135));rp.bottomMargin=dp(10);list.addView(row,rp);add.setOnClickListener(v->{deviceDb.add(f.mac,f.type.equals("xiaomi")?"Xiaomi LYWSDCGQ/01ZM":"Minew S1",f.type);states.put(f.mac,new SensorState());add.setText("Добавлено");add.setEnabled(false);});}}

    private void startMonitoring(){if(needPermissions())return;screenIsHistory=false;startScan();main.postDelayed(new Runnable(){public void run(){if(!screenIsHistory){stopScan();startScan();main.postDelayed(this,12000);}}},12000);}
    private void startScan(){if(screenIsHistory||needPermissions())return;BluetoothManager bm=(BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);BluetoothAdapter a=bm.getAdapter();if(a==null||!a.isEnabled()){setMainStatus("Включите Bluetooth");return;}scanner=a.getBluetoothLeScanner();if(scanner==null){setMainStatus("BLE недоступен");return;}callback=new ScanCallback(){@Override public void onScanResult(int type,ScanResult r){parseMeasurement(r);}@Override public void onScanFailed(int e){main.post(()->setMainStatus("Ошибка BLE-сканирования: "+e));}};try{scanner.startScan(null,new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).setReportDelay(0).build(),callback);}catch(SecurityException e){setMainStatus("Нет разрешения Bluetooth");}}
    private void stopScan(){if(scanner!=null&&callback!=null){try{scanner.stopScan(callback);}catch(SecurityException ignored){}callback=null;}}
    private void parseMeasurement(ScanResult r){if(r.getScanRecord()==null)return;String scanMac;try{scanMac=r.getDevice().getAddress();}catch(SecurityException e){return;}DeviceDb.Device d=deviceDb.find(scanMac);byte[] xdata=r.getScanRecord().getServiceData(XIAOMI_UUID);if(xdata!=null&&isXiaomi(xdata)){String embedded=xiaomiMac(xdata);DeviceDb.Device xd=deviceDb.find(embedded);if(xd!=null&&"xiaomi".equals(xd.type))d=xd;}if(d==null)return;float t=Float.NaN,h=Float.NaN;int battery=-1;
        if("minew".equals(d.type)){byte[] data=r.getScanRecord().getServiceData(MINEW_UUID);if(data==null||data.length<13||(data[0]&255)!=0xA1||(data[1]&255)!=0x01)return;int tr=(short)(((data[3]&255)<<8)|(data[4]&255));int hr=(short)(((data[5]&255)<<8)|(data[6]&255));t=tr/256f;h=hr/256f;String embedded=String.format(Locale.US,"%02X:%02X:%02X:%02X:%02X:%02X",data[12]&255,data[11]&255,data[10]&255,data[9]&255,data[8]&255,data[7]&255);if(!d.id.equalsIgnoreCase(scanMac)&&!d.id.equalsIgnoreCase(embedded))return;}
        else if("xiaomi".equals(d.type)){if(!isXiaomi(xdata))return;int type=u(xdata,11);if(type==0x0D){if(xdata.length<18)return;int tr=(short)(u(xdata,14)|(u(xdata,15)<<8));int hr=(short)(u(xdata,16)|(u(xdata,17)<<8));t=tr/10f;h=hr/10f;}else if(type==0x0A){if(xdata.length<15)return;battery=u(xdata,14);}else return;}
        final String id=d.id;final float ft=t,fh=h;final int fb=battery;main.post(()->updateSensor(id,ft,fh,fb));}
    private void updateSensor(String id,float t,float h,int battery){SensorState s=states.get(id);if(s==null){s=new SensorState();states.put(id,s);}if(!Float.isNaN(t))s.t=t;if(!Float.isNaN(h))s.h=h;if(battery>=0)s.battery=battery;s.last=System.currentTimeMillis();s.packets++;if(!Float.isNaN(s.t)&&!Float.isNaN(s.h))db.add(id,s.last,s.t,s.h);updateCard(id);setMainStatus("●  BLE-сканирование активно");}

    private void showHistory(){List<DeviceDb.Device> ds=deviceDb.all();if(ds.isEmpty())return;showHistoryFor(ds.get(0).id,ds.get(0).name);}
    private void showHistoryFor(String id,String name){historyDeviceId=id;historyDate.setTimeInMillis(System.currentTimeMillis());screenIsHistory=true;stopScan();main.removeCallbacksAndMessages(null);LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(Color.rgb(242,242,244));root.setPadding(dp(10),dp(8),dp(10),dp(8));applyInsets(root);
        LinearLayout top=new LinearLayout(this);top.setGravity(Gravity.CENTER_VERTICAL);Button back=button("←");back.setTextSize(28);back.setOnClickListener(v->{showMain();startMonitoring();});top.addView(back,new LinearLayout.LayoutParams(dp(54),dp(52)));TextView title=text("График: "+name,21);title.setTypeface(Typeface.DEFAULT,Typeface.BOLD);top.addView(title,new LinearLayout.LayoutParams(0,dp(52),1));root.addView(top,new LinearLayout.LayoutParams(-1,dp(52)));
        LinearLayout tabs=new LinearLayout(this);tabs.setGravity(Gravity.CENTER);dayButton=button("День");monthButton=button("Месяц");yearButton=button("Год");tabs.addView(dayButton,new LinearLayout.LayoutParams(0,dp(48),1));tabs.addView(monthButton,new LinearLayout.LayoutParams(0,dp(48),1));tabs.addView(yearButton,new LinearLayout.LayoutParams(0,dp(48),1));dayButton.setOnClickListener(v->{historyPeriod=0;refreshHistory();});monthButton.setOnClickListener(v->{historyPeriod=1;refreshHistory();});yearButton.setOnClickListener(v->{historyPeriod=2;refreshHistory();});root.addView(tabs,new LinearLayout.LayoutParams(-1,dp(54)));
        historyGraph=new GraphView(this);historyGraph.setDeviceId(historyDeviceId);root.addView(historyGraph,new LinearLayout.LayoutParams(-1,0,1));LinearLayout nav=new LinearLayout(this);nav.setGravity(Gravity.CENTER_VERTICAL);nav.setBackground(card(Color.WHITE));Button prev=button("◀"),next=button("▶");historyDateLabel=text("",19);nav.addView(prev,new LinearLayout.LayoutParams(dp(58),dp(58)));nav.addView(historyDateLabel,new LinearLayout.LayoutParams(0,dp(58),1));nav.addView(next,new LinearLayout.LayoutParams(dp(58),dp(58)));LinearLayout.LayoutParams np=new LinearLayout.LayoutParams(-1,dp(66));np.topMargin=dp(8);root.addView(nav,np);prev.setOnClickListener(v->{historyDate.add(historyPeriod==0?java.util.Calendar.DAY_OF_MONTH:historyPeriod==1?java.util.Calendar.MONTH:java.util.Calendar.YEAR,-1);refreshHistory();});next.setOnClickListener(v->{historyDate.add(historyPeriod==0?java.util.Calendar.DAY_OF_MONTH:historyPeriod==1?java.util.Calendar.MONTH:java.util.Calendar.YEAR,1);refreshHistory();});setContentView(root);refreshHistory();}
    private void setTab(Button b,boolean selected){b.setTextColor(selected?Color.WHITE:Color.rgb(80,80,88));GradientDrawable g=new GradientDrawable();g.setColor(selected?Color.rgb(50,190,165):Color.TRANSPARENT);g.setCornerRadius(dp(22));b.setBackground(g);}
    private void refreshHistory(){setTab(dayButton,historyPeriod==0);setTab(monthButton,historyPeriod==1);setTab(yearButton,historyPeriod==2);java.util.Calendar start=(java.util.Calendar)historyDate.clone(),end=(java.util.Calendar)historyDate.clone();String title;if(historyPeriod==0){start.set(java.util.Calendar.HOUR_OF_DAY,0);start.set(java.util.Calendar.MINUTE,0);start.set(java.util.Calendar.SECOND,0);start.set(java.util.Calendar.MILLISECOND,0);end=(java.util.Calendar)start.clone();end.add(java.util.Calendar.DAY_OF_MONTH,1);title=new SimpleDateFormat("dd.MM.yyyy",Locale.getDefault()).format(start.getTime());}else if(historyPeriod==1){start.set(java.util.Calendar.DAY_OF_MONTH,1);start.set(java.util.Calendar.HOUR_OF_DAY,0);start.set(java.util.Calendar.MINUTE,0);start.set(java.util.Calendar.SECOND,0);start.set(java.util.Calendar.MILLISECOND,0);end=(java.util.Calendar)start.clone();end.add(java.util.Calendar.MONTH,1);title=new SimpleDateFormat("LLLL yyyy",Locale.getDefault()).format(start.getTime());}else{start.set(java.util.Calendar.MONTH,java.util.Calendar.JANUARY);start.set(java.util.Calendar.DAY_OF_MONTH,1);start.set(java.util.Calendar.HOUR_OF_DAY,0);start.set(java.util.Calendar.MINUTE,0);start.set(java.util.Calendar.SECOND,0);start.set(java.util.Calendar.MILLISECOND,0);end=(java.util.Calendar)start.clone();end.add(java.util.Calendar.YEAR,1);title=new SimpleDateFormat("yyyy",Locale.getDefault()).format(start.getTime());}title=title.substring(0,1).toUpperCase(Locale.getDefault())+title.substring(1);historyDateLabel.setText(title);List<HistoryDb.Reading> data=db.range(historyDeviceId,start.getTimeInMillis(),end.getTimeInMillis());historyGraph.setData(data,start.getTimeInMillis(),end.getTimeInMillis(),historyPeriod);}
    @Override protected void onPause(){super.onPause();stopScan();main.removeCallbacksAndMessages(null);}
    @Override protected void onResume(){super.onResume();if(!screenIsHistory&&deviceCards!=null&&!needPermissions())main.postDelayed(()->{if(!screenIsHistory)startMonitoring();},150);}
}
