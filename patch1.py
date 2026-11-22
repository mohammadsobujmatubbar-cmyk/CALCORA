p = "app/src/main/AndroidManifest.xml"
s = open(p, encoding="utf-8").read()
if "RECORD_AUDIO" in s:
    print("already patched")
else:
    add = '''    <uses-permission android:name="android.permission.RECORD_AUDIO" />
    <queries>
        <intent>
            <action android:name="android.speech.RecognitionService" />
        </intent>
    </queries>

'''
    i = s.index("<application")
    s = s[:i] + add + "    " + s[i:]
    open(p, "w", encoding="utf-8").write(s)
    print("patched")
