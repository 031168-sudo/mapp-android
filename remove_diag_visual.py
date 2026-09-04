from pathlib import Path

p = Path("app/src/main/assets/code.js")
s = p.read_text(encoding="utf-8-sig")

# The working patch already contains the diagnostic logic. Remove only a visible
# statusDiag element if one is present; never remove the diagnostic global or
# any refresh/update code.
if '<div id=\\"statusDiag\\"' in s:
    start = s.index('window.__statusDiag&&')
    end = s.index('window.__statusDiagRender();', start) + len('window.__statusDiagRender();')
    block = s[start:end]
    s = s[:start] + 'window.__statusDiag&&(window.__statusDiag.cid=(t.data||{}).cid);' + s[end:]
    print("Removed visible status diagnostic block:", len(block), "chars")
else:
    print("No visible status diagnostic block present; leaving working logic unchanged")

assert '<div id=\\"statusDiag\\"' not in s
p.write_text(s, encoding="utf-8")
