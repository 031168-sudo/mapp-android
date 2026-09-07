from pathlib import Path

p = Path('app/src/main/java/com/minews1/MainActivity.java')
s = p.read_text(encoding='utf-8')

old = '''        TextView title=text("Температура и влажность",28);title.setTypeface(Typeface.DEFAULT,Typeface.BOLD);root.addView(title,new LinearLayout.LayoutParams(-1,dp(46)));\n        root.addView(label("BLE-мониторинг датчиков"),new LinearLayout.LayoutParams(-1,dp(28)));\n        deviceCards=new LinearLayout(this);deviceCards.setOrientation(LinearLayout.VERTICAL);root.addView(deviceCards,new LinearLayout.LayoutParams(-1,-2));\n        Button devices=button("Устройства  +");devices.setTextSize(17);devices.setOnClickListener(v->showDevices());LinearLayout.LayoutParams devicesLp=new LinearLayout.LayoutParams(-1,dp(56));devicesLp.topMargin=dp(12);root.addView(devices,devicesLp);\n        Button history=button("История измерений  →");history.setTextSize(17);history.setOnClickListener(v->showHistory());LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(-1,dp(56));hp.topMargin=dp(10);root.addView(history,hp);\n        mainStatus=label("Поиск датчиков…");mainStatus.setTextSize(14);LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,dp(32));sp.topMargin=dp(5);root.addView(mainStatus,sp);'''
new = '''        TextView subtitle=label("BLE-мониторинг датчиков");subtitle.setTextSize(22);subtitle.setTypeface(Typeface.DEFAULT,Typeface.NORMAL);root.addView(subtitle,new LinearLayout.LayoutParams(-1,dp(40)));\n        deviceCards=new LinearLayout(this);deviceCards.setOrientation(LinearLayout.VERTICAL);root.addView(deviceCards,new LinearLayout.LayoutParams(-1,-2));\n        Button devices=button("Устройства  +");devices.setTextSize(17);devices.setOnClickListener(v->showDevices());LinearLayout.LayoutParams devicesLp=new LinearLayout.LayoutParams(-1,dp(56));devicesLp.topMargin=dp(12);root.addView(devices,devicesLp);\n        mainStatus=label("Поиск датчиков…");mainStatus.setTextSize(14);LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,dp(32));sp.topMargin=dp(5);root.addView(mainStatus,sp);'''
if old not in s:
    raise SystemExit('main screen block not found')
s = s.replace(old, new, 1)

old2 = 'TextView info=label(d.type.equals("xiaomi")?"Xiaomi LYWSDCGQ/01ZM  •  "+d.id:d.id);'
new2 = 'TextView info=label(d.id);'
if old2 not in s:
    raise SystemExit('device info block not found')
s = s.replace(old2, new2, 1)

old3 = 'TextView values=text("—.— °C    —.— % RH",31);'
new3 = 'TextView values=text("—.— °C    —.— %",31);'
if old3 not in s:
    raise SystemExit('values placeholder not found')
s = s.replace(old3, new3, 1)

old4 = 'String.format(Locale.US,"%.2f °C    %.1f %% RH",s.t,s.h)'
new4 = 'String.format(Locale.US,"%.2f °C    %.1f %%",s.t,s.h)'
if old4 not in s:
    raise SystemExit('values format not found')
s = s.replace(old4, new4, 1)

old5 = 'private boolean isXiaomi(byte[] d){if(d==null||d.length<18)return false;'
new5 = 'private boolean isXiaomi(byte[] d){if(d==null||d.length<15)return false;'
if old5 not in s:
    raise SystemExit('xiaomi length check not found')
s = s.replace(old5, new5, 1)

old6 = 't=tr/256f;h=hr/256f;String embedded=String.format(Locale.US,"%02X:%02X:%02X:%02X:%02X:%02X",data[12]&255,data[11]&255,data[10]&255,data[9]&255,data[8]&255,data[7]&255);'
new6 = 't=tr/256f;h=hr/256f;battery=data[2]&255;String embedded=String.format(Locale.US,"%02X:%02X:%02X:%02X:%02X:%02X",data[12]&255,data[11]&255,data[10]&255,data[9]&255,data[8]&255,data[7]&255);'
if old6 not in s:
    raise SystemExit('minew measurement block not found')
s = s.replace(old6, new6, 1)

p.write_text(s, encoding='utf-8')
