from pathlib import Path
import re

p = Path("project/app/src/main/assets/code.js")
s = p.read_text(encoding="utf-8-sig")

# Model module: add a public refresh() that uses the SAME full login request
# already used after authentication, so the server returns the complete snapshot.
m10_start = s.find("10:[function")
m10_end = s.find("],10:[function", 0)
# Find module 10's actual body end by locating the next module marker.
m10_end = s.find("],11:[function", m10_start)
if m10_start < 0 or m10_end < 0:
    raise SystemExit("model module not found")
m10 = s[m10_start:m10_end]

if "exp.refresh=function()" not in m10:
    needle = "},exp.logout=function(){"
    replacement = """},exp.refresh=function(){
if(!savedUsername||!savedPassword)return null;
return server.async({name:"login",data:{username:savedUsername,password:savedPassword,timezone:moment().utcOffset(),clientVersion:buildVersion},validate:function(e){return e&&e.sid&&_.isArray(e.userPrivileges)},timeout:6e4}).done(function(e){
runPatches(e.patches||[]),sid=e.sid,userPrivileges=e.userPrivileges,onNewData(e),getUpdateAsync()
})
},exp.logout=function(){"""
    if needle not in m10:
        raise SystemExit("logout anchor not found in model module")
    m10 = m10.replace(needle, replacement, 1)
    s = s[:m10_start] + m10 + s[m10_end:]

# Status module: ONLY this module gets the timer. No global replacement of detach().
m106_start = s.find("106:[function")
m106_end = s.find("],107:[function", m106_start)
if m106_start < 0 or m106_end < 0:
    raise SystemExit("Status module not found")
m106 = s[m106_start:m106_end]

needle = "s=e.model.userDataUpdatedSubscription.subscribe(a),u.enhanceWithin()"
replacement = "s=e.model.userDataUpdatedSubscription.subscribe(a),t.__statusRefreshTimer=window.setInterval(function(){e.model.refresh&&e.model.refresh()},6e4),u.enhanceWithin()"
if needle not in m106:
    raise SystemExit("Status subscription anchor not found")
m106 = m106.replace(needle, replacement, 1)

needle = "},r.detach=function(){s&&(s.stop(),s=null)}};"
replacement = "},r.detach=function(){t.__statusRefreshTimer&&(window.clearInterval(t.__statusRefreshTimer),t.__statusRefreshTimer=null),s&&(s.stop(),s=null)}};"
if needle not in m106:
    raise SystemExit("Status detach anchor not found")
m106 = m106.replace(needle, replacement, 1)

s = s[:m106_start] + m106 + s[m106_end:]
p.write_text(s, encoding="utf-8")
print("clean status refresh patch applied")
