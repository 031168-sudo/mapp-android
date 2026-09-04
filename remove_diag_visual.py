from pathlib import Path

p = Path("app/src/main/assets/code.js")
s = p.read_text(encoding="utf-8-sig")
visual = 'window.__statusDiag&&(window.__statusDiag.cid=(t.data||{}).cid),t.$root.prepend($("<div id=\\"statusDiag\\" style=\\"font-size:11px;padding:4px;border:1px solid #999;margin:2px;word-break:break-all;\\"></div>")),window.__statusDiagRender();'
assert visual in s, "visible diagnostic block not found"
s = s.replace(visual, 'window.__statusDiag&&(window.__statusDiag.cid=(t.data||{}).cid);', 1)
p.write_text(s, encoding="utf-8")
print("Removed only visible status diagnostic block")
