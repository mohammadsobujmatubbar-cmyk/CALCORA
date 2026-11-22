p = "app/src/main/java/com/calcora/app/MainActivity.kt"
s = open(p, encoding="utf-8").read()
a = "Screen.More -> SettingsScreen(settings)"
if "VaultScreen" in s:
    print("already patched")
elif a not in s:
    print("anchor not found")
else:
    s = s.replace(a, a + "\n                    Screen.Vault -> VaultScreen()", 1)
    open(p, "w", encoding="utf-8").write(s)
    print("patched")
