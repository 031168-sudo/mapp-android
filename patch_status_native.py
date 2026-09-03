from pathlib import Path
import re

p = Path("project/app/src/main/assets/code.js")
s = p.read_text(encoding="utf-8-sig")

# Module 10 = data/session model.
m10_start = s.find("10:[function")
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
        raise SystemExit("logout anchor not found")
    m10 = m10.replace(needle, replacement, 1)
    s = s[:m10_start] + m10 + s[m10_end:]

# Module 106 = Status view. Restrict every modification to this module.
m106_start = s.find("106:[function")
m106_end = s.find("],107:[function", m106_start)
if m106_start < 0 or m106_end < 0:
    raise SystemExit("Status module not found")
m106 = s[m106_start:m106_end]

if "__statusRefreshTimer" not in m106:
    # Attach the timer immediately after the existing subscription call.
    pat = r"(\.userDataUpdatedSubscription\.subscribe\([^)]*\))"
    m106, count = re.subn(
        pat,
        r'\1,t.__statusRefreshTimer=window.setInterval(function(){e.model.refresh&&e.model.refresh()},6e4)',
        m106,
        count=1
    )
    if count != 1:
        raise SystemExit("Status subscription not found")

    # Clean up ONLY the timer owned by the Status view.
    pat = r"(r\.detach=function\(\)\{)"
    m106, count = re.subn(
        pat,
        r'\1t.__statusRefreshTimer&&(window.clearInterval(t.__statusRefreshTimer),t.__statusRefreshTimer=null),',
        m106,
        count=1
    )
    if count != 1:
        raise SystemExit("Status detach not found")

    s = s[:m106_start] + m106 + s[m106_end:]

p.write_text(s, encoding="utf-8")
print("clean one-minute Status refresh patch applied")
