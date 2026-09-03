from pathlib import Path
import re

root=Path("project")
codes=list(root.rglob("code.js"))
if not codes:
    print("DIAG: code.js not found")
    raise SystemExit(0)
code=codes[0]
s=code.read_text(encoding="utf-8-sig")
print("DIAG: code.js =", code, "bytes=", len(s))

for term in ["getUpdateAsync=function", "exp.login=function", "userDataUpdatedSubscription", "106:[function", "107:[function", "Status", "server.async({name:"update""]:
    print("\n=== TERM",term,"===")
    positions=[m.start() for m in re.finditer(re.escape(term),s)]
    print("count",len(positions),"positions",positions[:20])
    for p in positions[:3]:
        print(s[max(0,p-300):min(len(s),p+900)])

# Patch only if the exact known Status structure is present.
if "requestStatusNativeUpdate=getUpdateAsync" not in s:
    m=re.search(r"getUpdateAsync=function\(\).*?;exp\.login=function\(",s)
    if not m:
        print("PATCH: native update/login pattern not found; leaving project unchanged")
        raise SystemExit(0)
    s=s[:m.end()-len("exp.login=function(")]+"requestStatusNativeUpdate=getUpdateAsync,exp.login=function("+s[m.end():]

bstart=s.find("106:[function")
bend=s.find("107:[function",bstart)
if bstart<0 or bend<0:
    print("PATCH: Status module boundary not found; leaving native wrapper only")
    code.write_text(s,encoding="utf-8")
    raise SystemExit(0)
b=s[bstart:bend]
if "window.setInterval(function(){o.model.requestStatusNativeUpdate()},60000)" not in b:
    b=b.replace("var a,s;r.attach=function(){","var a,s,q;r.attach=function(){",1)
    needle="s=e.model.userDataUpdatedSubscription.subscribe(a),u.enhanceWithin(),"
    if needle not in b:
        print("PATCH: subscription pattern not found; leaving project unchanged")
        raise SystemExit(0)
    b=b.replace(needle,"s=e.model.userDataUpdatedSubscription.subscribe(a),q=window.setInterval(function(){o.model.requestStatusNativeUpdate()},60000),u.enhanceWithin(),",1)
    detach="r.detach=function(){s&&(s.stop(),s=null)}}"
    if detach not in b:
        print("PATCH: detach pattern not found; leaving project unchanged")
        raise SystemExit(0)
    b=b.replace(detach,"r.detach=function(){s&&(s.stop(),s=null),q&&(window.clearInterval(q),q=null)}}",1)
    s=s[:bstart]+b+s[bend:]
code.write_text(s,encoding="utf-8")
print("PATCH: completed")
