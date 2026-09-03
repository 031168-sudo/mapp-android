from pathlib import Path

p=Path("project/app/src/main/assets/code.js")
s=p.read_text(encoding="utf-8-sig")

marker=";app.production = true;"
diag=''';window.__statusDiag=window.__statusDiag||{updates:0,responses:0,signals:0,status:0,refresh:0,refreshResponses:0,last:"",cid:""};
window.__statusDiagRender=function(){
 var d=window.__statusDiag, el=document.getElementById("statusDiag");
 if(el) el.textContent="Диагностика: update="+d.updates+" | ответ="+d.responses+" | refresh="+d.refresh+" | refreshОтвет="+d.refreshResponses+" | signalStreams="+d.signals+" | Status="+d.status+" | CID="+d.cid+" | "+d.last;
};'''
if "window.__statusDiag=window.__statusDiag||" not in s:
    s=diag+s

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


# Full-snapshot refresh: deliberately reuse the exact login operation that supplies
# the complete initial dataset, including signalStreams for every device.
old='exp.logout=function(){sid&&server.async({name:"logout"}),reset()}'
new='exp.refresh=function(){if(!savedUsername)return null;window.__statusDiag.refresh++,window.__statusDiag.last="refresh "+moment().format("HH:mm:ss"),window.__statusDiagRender();var n={username:savedUsername,password:savedPassword,timezone:moment().utcOffset(),clientVersion:buildVersion};return server.async({name:"login",data:n,validate:function(e){return e&&e.sid&&_.isArray(e.userPrivileges)},timeout:6e4}).done(function(e){runPatches(e.patches||[]),sid=e.sid,userPrivileges=e.userPrivileges,window.__statusDiag.refreshResponses++,window.__statusDiag.last="refresh response "+moment().format("HH:mm:ss"),window.__statusDiagRender(),onNewData(e)})},exp.logout=function(){sid&&server.async({name:"logout"}),reset()}'
if 'exp.refresh=function(){if(!savedUsername)' not in s:
    if old not in s: raise SystemExit("source logout pattern not found")
    s=s.replace(old,new,1)

# Start a one-minute full-snapshot refresh while the Status page is attached.
old='s=e.model.userDataUpdatedSubscription.subscribe(a),u.enhanceWithin()'
new='s=e.model.userDataUpdatedSubscription.subscribe(a),t.__statusRefreshTimer=window.setInterval(function(){e.model.refresh&&e.model.refresh()},6e4),u.enhanceWithin()'
if old not in s: raise SystemExit("status subscription source pattern not found")
s=s.replace(old,new,1)

old='},r.detach=function(){s&&(s.stop(),s=null)}};return t.mlr={title:{en:"Status",ru:"Статус"}'
new='},r.detach=function(){t.__statusRefreshTimer&&(window.clearInterval(t.__statusRefreshTimer),t.__statusRefreshTimer=null),s&&(s.stop(),s=null)}};return t.mlr={title:{en:"Status",ru:"Статус"}'
if old not in s: raise SystemExit("exact status detach pattern not found")
s=s.replace(old,new,1)

p.write_text(s,encoding="utf-8")
print("diagnostic patch applied")
