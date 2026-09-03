from pathlib import Path
import zipfile, re

root=Path("project")
codes=list(root.rglob("code.js"))
if not codes:
    raise SystemExit("code.js not found")
code=codes[0]
s=code.read_text(encoding="utf-8-sig")

# 1) Reuse the application's real update request.
if "requestStatusNativeUpdate=getUpdateAsync" not in s:
    m=re.search(r"getUpdateAsync=function\(\).*?;exp\.login=function\(",s)
    if not m:
        raise SystemExit("Native getUpdateAsync/login block not found")
    s=s[:m.end()-len("exp.login=function(")]+"requestStatusNativeUpdate=getUpdateAsync,exp.login=function("+s[m.end():]

# 2) Find the Status module by its stable title and model usage.
m=re.search(r"(\d+:\[function\([^\n]{0,5000}?r\.attach=function\(\)\{)",s)
if not m:
    raise SystemExit("Status module attach function not found")
module_start=m.start()
module_end=s.find("\n", s.find("107:[function",module_start))
if module_end<0:
    module_end=len(s)
# In the known project the Status module is module 106. Prefer exact boundary.
bstart=s.find("106:[function")
bend=s.find("107:[function",bstart)
if bstart>=0 and bend>0:
    b=s[bstart:bend]
else:
    raise SystemExit("Status module boundary 106/107 not found")

if "window.setInterval(function(){o.model.requestStatusNativeUpdate()},60000)" not in b:
    b=b.replace("var a,s;r.attach=function(){","var a,s,q;r.attach=function(){",1)
    needle="s=e.model.userDataUpdatedSubscription.subscribe(a),u.enhanceWithin(),"
    if needle not in b:
        raise SystemExit("Status subscription point not found")
    b=b.replace(needle,"s=e.model.userDataUpdatedSubscription.subscribe(a),q=window.setInterval(function(){o.model.requestStatusNativeUpdate()},60000),u.enhanceWithin(),",1)
    detach="r.detach=function(){s&&(s.stop(),s=null)}}"
    if detach not in b:
        raise SystemExit("Status detach point not found")
    b=b.replace(detach,"r.detach=function(){s&&(s.stop(),s=null),q&&(window.clearInterval(q),q=null)}}",1)
    s=s[:bstart]+b+s[bend:]

code.write_text(s,encoding="utf-8")
print("patched",code)
