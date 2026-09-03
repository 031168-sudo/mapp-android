from pathlib import Path
import re

codes=list(Path("project").rglob("code.js"))
if not codes:
    print("DIAG code.js not found")
    raise SystemExit(0)

code=codes[0]
s=code.read_text(encoding="utf-8-sig")
print("DIAG code.js", code, "len", len(s))

terms=[
    "getUpdateAsync=function",
    "exp.login=function",
    "userDataUpdatedSubscription",
    "106:[function",
    "107:[function",
    'server.async({name:"update"'
]
for term in terms:
    pos=[m.start() for m in re.finditer(re.escape(term),s)]
    print("\nDIAG TERM",repr(term),"COUNT",len(pos),"POS",pos[:10])
    for p in pos[:2]:
        print(s[max(0,p-250):min(len(s),p+700)])

# Native update export.
if "requestStatusNativeUpdate=getUpdateAsync" not in s:
    m=re.search(r"getUpdateAsync=function\(\).*?;exp\.login=function\(",s)
    if not m:
        print("PATCH native update pattern NOT FOUND")
        raise SystemExit(0)
    cut=m.end()-len("exp.login=function(")
    s=s[:cut]+"requestStatusNativeUpdate=getUpdateAsync,exp.login=function("+s[m.end():]

bstart=s.find("106:[function")
bend=s.find("107:[function",bstart)
if bstart<0 or bend<0:
    print("PATCH Status module boundary NOT FOUND")
    code.write_text(s,encoding="utf-8")
    raise SystemExit(0)

b=s[bstart:bend]
needle="s=e.model.userDataUpdatedSubscription.subscribe(a),u.enhanceWithin(),"
if needle not in b:
    print("PATCH Status subscription pattern NOT FOUND")
    print("STATUS MODULE SAMPLE:")
    print(b[:6000])
    raise SystemExit(0)

if "window.setInterval(function(){o.model.requestStatusNativeUpdate()},60000)" not in b:
    b=b.replace("var a,s;r.attach=function(){","var a,s,q;r.attach=function(){",1)
    b=b.replace(needle,"s=e.model.userDataUpdatedSubscription.subscribe(a),q=window.setInterval(function(){o.model.requestStatusNativeUpdate()},60000),u.enhanceWithin(),",1)
    detach="r.detach=function(){s&&(s.stop(),s=null)}}"
    if detach not in b:
        print("PATCH Status detach pattern NOT FOUND")
        raise SystemExit(0)
    b=b.replace(detach,"r.detach=function(){s&&(s.stop(),s=null),q&&(window.clearInterval(q),q=null)}}",1)
    s=s[:bstart]+b+s[bend:]

code.write_text(s,encoding="utf-8")
print("PATCH completed")
