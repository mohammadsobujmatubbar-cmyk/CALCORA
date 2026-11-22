vp = "app/src/main/java/com/calcora/app/Voice.kt"
v = open(vp, encoding="utf-8").read()
if "VoiceNum.toExpr" in v:
    print("voice already patched")
else:
    a = "    fun toExpr(raw: String): String? {"
    if a not in v:
        print("voice anchor not found")
    else:
        v = v.replace(a, "    fun toExpr(raw: String): String? = VoiceNum.toExpr(raw)\n\n    @Suppress(\"unused\")\n    fun oldToExpr(raw: String): String? {", 1)
        open(vp, "w", encoding="utf-8").write(v)
        print("voice patched")

mp = "app/src/main/java/com/calcora/app/MainActivity.kt"
s = open(mp, encoding="utf-8").read()
if "VoiceBus" in s:
    print("main already patched")
else:
    t1 = '            Toast.makeText(context, "$expr = ${fmt(v)}", Toast.LENGTH_LONG).show()\n'
    t2 = '''        val target = VoiceParser.toScreen(text)
        if (target != null) screen = target
        else Toast.makeText(context, "বুঝতে পারিনি: $text", Toast.LENGTH_SHORT).show()
'''
    t3 = "    var fresh by remember { mutableStateOf(false) }"
    if t1 not in s or t2 not in s or t3 not in s:
        print("main anchor not found")
    else:
        s = s.replace(t1, "            VoiceBus.expr = expr\n            VoiceBus.tick += 1\n", 1)
        s = s.replace(t2, "        // calculator mode: only numbers are recorded\n", 1)
        eff = '''

    LaunchedEffect(VoiceBus.tick) {
        if (VoiceBus.tick > VoiceBus.seen) {
            VoiceBus.seen = VoiceBus.tick
            expr = VoiceBus.expr
            fresh = true
        }
    }'''
        s = s.replace(t3, t3 + eff, 1)
        open(mp, "w", encoding="utf-8").write(s)
        print("main patched")
