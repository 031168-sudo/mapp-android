from pathlib import Path

g = Path('app/src/main/java/com/minews1/GraphView.java')
s = g.read_text(encoding='utf-8')

# Remove the large "История измерений" heading and make the date/tabs four times larger.
s = s.replace('float top=94,bottom=h-54,gap=12,cardH=(bottom-top-gap)/2f;', 'float top=170,bottom=h-54,gap=12,cardH=(bottom-top-gap)/2f;', 1)
s = s.replace('txt(c,"История измерений",18,28,24,0xff24242a);String title=period==0?dayTitle.format(selected.getTime()):period==1?monthTitle.format(selected.getTime()):yearTitle.format(selected.getTime());p.setTextSize(17);float tw=p.measureText(title);txt(c,title,(w-tw)/2,56,17,0xff3c3c43);String[] tabs={"День","Месяц","Год"};float total=w-28,bw=total/3f;for(int i=0;i<3;i++){float l=14+i*bw,r=l+bw-4;round(c,l,66,r,91,i==period?0xff2f7d70:0xffffffff,12);p.setTextSize(14);tw=p.measureText(tabs[i]);txt(c,tabs[i],l+(r-l-tw)/2,83,14,i==period?0xffffffff:0xff66666e);}', 'String title=period==0?dayTitle.format(selected.getTime()):period==1?monthTitle.format(selected.getTime()):yearTitle.format(selected.getTime());p.setTextSize(68);float tw=p.measureText(title);txt(c,title,(w-tw)/2,76,68,0xff3c3c43);String[] tabs={"День","Месяц","Год"};float total=w-28,bw=total/3f;for(int i=0;i<3;i++){float l=14+i*bw,r=l+bw-4;round(c,l,88,r,158,i==period?0xff2f7d70:0xffffffff,18);p.setTextSize(56);tw=p.measureText(tabs[i]);txt(c,tabs[i],l+(r-l-tw)/2,140,56,i==period?0xffffffff:0xff66666e);}', 1)
s = s.replace('txt(c,title,cl+16,ct+28,19,tempChart?0xff159b83:0xff3b78b5);float l=cl+18,r=cr-58,t=ct+48,b=cb-34;', 'txt(c,title,cl+16,ct+90,76,tempChart?0xff159b83:0xff3b78b5);float l=cl+18,r=cr-58,t=ct+112,b=cb-34;', 1)
# Move right-axis numbers left and right-align them so the full values fit inside the chart card.
s = s.replace('float val=max-(max-min)*i/4f;txt(c,tempChart?String.format(Locale.getDefault(),"%.1f",val):String.format(Locale.getDefault(),"%.0f",val),r+7,y+5,45,0xff77777f);', 'float val=max-(max-min)*i/4f;String axisLabel=tempChart?String.format(Locale.getDefault(),"%.1f",val):String.format(Locale.getDefault(),"%.0f",val);p.setStyle(Paint.Style.FILL);p.setColor(0xff77777f);p.setTextSize(45);p.setTextAlign(Paint.Align.RIGHT);c.drawText(axisLabel,cr-10,y+5,p);p.setTextAlign(Paint.Align.LEFT);', 1)
s = s.replace('if(y>=66&&y<=92){', 'if(y>=88&&y<=158){', 1)

g.write_text(s, encoding='utf-8')
