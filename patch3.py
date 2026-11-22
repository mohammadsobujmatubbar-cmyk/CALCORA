p = "app/src/main/java/com/calcora/app/Settings.kt"
s = open(p, encoding="utf-8").read()
old = 'listOf(15 to "15s", 30 to "30s", 60 to "1m", 120 to "2m", 300 to "5m")'
new = 'listOf(5 to "5s", 15 to "15s", 30 to "30s", 60 to "1m", 300 to "5m")'
if new in s:
    print("already patched")
elif old not in s:
    print("anchor not found")
else:
    open(p, "w", encoding="utf-8").write(s.replace(old, new, 1))
    print("patched")
