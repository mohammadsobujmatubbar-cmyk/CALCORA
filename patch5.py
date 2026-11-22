p = "app/src/main/AndroidManifest.xml"
s = open(p, encoding="utf-8").read()
if "android:icon" in s:
    print("already patched")
elif "<application" not in s:
    print("anchor not found")
else:
    add = '<application\n        android:icon="@mipmap/ic_launcher"\n        android:roundIcon="@mipmap/ic_launcher_round"'
    open(p, "w", encoding="utf-8").write(s.replace("<application", add, 1))
    print("patched")
