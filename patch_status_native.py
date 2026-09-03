from pathlib import Path

p=Path("project/app/src/main/assets/code.js")
s=p.read_text(encoding="utf-8-sig")

marker=";app.production = true;"
diag=''';window.__statusDiag=window.__statusDiag||{updates:0,responses:0,signals:0,status:0,last:"",cid:""};
window.__statusDiagRender=function(){
 var d=window.__statusDiag, el=document.getElementById("statusDiag");
 if(el) el.textContent="Диагностика: update="+d.updates+" | ответ="+d.responses+" | signalStreams="+d.signals+" | Status="+d.status+" | CID="+d.cid+" | "+d.last;
};'''
if "window.__statusDiag=window.__statusDiag||" not in s:
    if marker not in s: raise SystemExit("diagnostic insertion marker not found")
    s=s.replace(marker,diag+marker,1)

old='console.log("starting update operation"),getUpdateAsyncOp=server.async({name:"update",data:{nextUserStreamIndex:userStream.nextIdx(),nextSignalStreamsIndexes:_.mapValues(signalStreams,function(e){return e.nextIdx()})},timeout:12e4})'
new='window.__statusDiag.updates++,window.__statusDiag.last="update "+moment().format("HH:mm:ss"),window.__statusDiagRender(),console.log("starting update operation"),getUpdateAsyncOp=server.async({name:"update",data:{nextUserStreamIndex:userStream.nextIdx(),nextSignalStreamsIndexes:_.mapValues(signalStreams,function(e){return e.nextIdx()})},timeout:12e4})'
if old not in s: raise SystemExit("update call pattern not found")
s=s.replace(old,new,1)

old='failedUpdatesCount=0,onNewData(e)'
new='failedUpdatesCount=0,window.__statusDiag.responses++,window.__statusDiag.last="response "+moment().format("HH:mm:ss"),window.__statusDiagRender(),onNewData(e)'
if old not in s: raise SystemExit("response pattern not found")
s=s.replace(old,new,1)

old='onNewData=function(e){var t=updateUserData(e.userStream),n=pushSignalData(e.signalStreams);(t||n)&&fireNotifications()}'
new='onNewData=function(e){var t=updateUserData(e.userStream),n=pushSignalData(e.signalStreams);n&&(window.__statusDiag.signals++,window.__statusDiag.last="signalStreams "+moment().format("HH:mm:ss")),window.__statusDiagRender(),(t||n)&&fireNotifications()}'
if old not in s: raise SystemExit("onNewData pattern not found")
s=s.replace(old,new,1)

old="t.setTitle(i.title),t.$root.parents('[data-role=\"content\"]').css(\"padding\",\"0\");"
new="t.setTitle(i.title),t.$root.parents('[data-role=\"content\"]').css(\"padding\",\"0\"),window.__statusDiag&&(window.__statusDiag.cid=(t.data||{}).cid),t.$root.prepend($(\"<div id=\\\"statusDiag\\\" style=\\\"font-size:12px;padding:4px;border:1px solid #999;margin:2px;\\\"></div>\")),window.__statusDiagRender();"
if old not in s: raise SystemExit("status attach pattern not found")
s=s.replace(old,new,1)

old='a=function(){var e=o.model.getLastSignal(l);t.attachChildView(m,[t.signalView,e])}'
new='a=function(){var e=o.model.getLastSignal(l);window.__statusDiag.status++,window.__statusDiag.last="Status "+moment().format("HH:mm:ss"),window.__statusDiagRender(),t.attachChildView(m,[t.signalView,e])}'
if old not in s: raise SystemExit("status callback pattern not found")
s=s.replace(old,new,1)

p.write_text(s,encoding="utf-8")
print("diagnostic patch applied")
