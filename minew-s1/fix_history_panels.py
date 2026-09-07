from pathlib import Path

# Restore the large external date navigation in MainActivity.
p = Path('app/src/main/java/com/minews1/MainActivity.java')
s = p.read_text(encoding='utf-8')

old = '''historyGraph=new GraphView(this);historyGraph.setDeviceId(historyDeviceId);root.addView(historyGraph,new LinearLayout.LayoutParams(-1,0,1));setContentView(root);refreshHistory();'''
new = '''historyGraph=new GraphView(this);historyGraph.setDeviceId(historyDeviceId);root.addView(historyGraph,new LinearLayout.LayoutParams(-1,0,1));LinearLayout nav=new LinearLayout(this);nav.setGravity(Gravity.CENTER_VERTICAL);nav.setBackground(card(Color.WHITE));Button prev=button("◀"),next=button("▶");historyDateLabel=text("",19);nav.addView(prev,new LinearLayout.LayoutParams(dp(58),dp(58)));nav.addView(historyDateLabel,new LinearLayout.LayoutParams(0,dp(58),1));nav.addView(next,new LinearLayout.LayoutParams(dp(58),dp(58)));LinearLayout.LayoutParams np=new LinearLayout.LayoutParams(-1,dp(66));np.topMargin=dp(8);root.addView(nav,np);prev.setOnClickListener(v->{historyDate.add(historyPeriod==0?java.util.Calendar.DAY_OF_MONTH:historyPeriod==1?java.util.Calendar.MONTH:java.util.Calendar.YEAR,-1);refreshHistory();});next.setOnClickListener(v->{historyDate.add(historyPeriod==0?java.util.Calendar.DAY_OF_MONTH:historyPeriod==1?java.util.Calendar.MONTH:java.util.Calendar.YEAR,1);refreshHistory();});setContentView(root);refreshHistory();'''
if old not in s:
    raise SystemExit('MainActivity history graph block not found')
s = s.replace(old, new, 1)

old_label = 'title=title.substring(0,1).toUpperCase(Locale.getDefault())+title.substring(1);List<HistoryDb.Reading> data=db.range(historyDeviceId,start.getTimeInMillis(),end.getTimeInMillis());'
new_label = 'title=title.substring(0,1).toUpperCase(Locale.getDefault())+title.substring(1);historyDateLabel.setText(title);List<HistoryDb.Reading> data=db.range(historyDeviceId,start.getTimeInMillis(),end.getTimeInMillis());'
if old_label not in s:
    raise SystemExit('MainActivity history date label assignment point not found')
s = s.replace(old_label, new_label, 1)
p.write_text(s, encoding='utf-8')

# Remove only the tiny date panel drawn inside GraphView. The large MainActivity panel above remains.
g = Path('app/src/main/java/com/minews1/GraphView.java')
gs = g.read_text(encoding='utf-8')
old_draw = 'drawChart(c,14,top+cardH+gap,w-14,bottom,"Влажность",false);drawDateNav(c,w,h);'
new_draw = 'drawChart(c,14,top+cardH+gap,w-14,bottom,"Влажность",false);'
if old_draw not in gs:
    raise SystemExit('GraphView tiny date panel draw call not found')
gs = gs.replace(old_draw, new_draw, 1)
g.write_text(gs, encoding='utf-8')
