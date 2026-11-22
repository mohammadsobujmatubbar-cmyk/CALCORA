mp = "app/src/main/AndroidManifest.xml"
m = open(mp, encoding="utf-8").read()
if "TTS_SERVICE" in m:
    print("manifest already patched")
elif "</queries>" not in m:
    print("manifest anchor not found")
else:
    add = '    <intent>\n        <action android:name="android.intent.action.TTS_SERVICE" />\n    </intent>\n    </queries>'
    open(mp, "w", encoding="utf-8").write(m.replace("</queries>", add, 1))
    print("manifest patched")

p = "app/src/main/java/com/calcora/app/MainActivity.kt"
s = open(p, encoding="utf-8").read()
if "voiceMode" in s:
    print("already patched")
else:
    block = r'''    val voice = remember { VoiceController(context) }
    var listening by remember { mutableStateOf(false) }
    var voiceMode by remember { mutableStateOf("ask") }
    val voiceNote = remember { longArrayOf(0L, 0L) }
    DisposableEffect(Unit) { onDispose { voice.release() } }

    fun enterCalc() {
        voiceMode = "calc"
        screen = Screen.Calculator
        voice.say("ক্যালকুলেটর মোড চালু। বলুন।", "Calculator mode on. Go ahead.")
    }

    fun enterNote() {
        voiceMode = "note"
        voiceNote[0] = 0L
        voiceNote[1] = 0L
        screen = Screen.Notebooks
        voice.say("নোট খাতা চালু। যা বলবেন সব লেখা হবে।", "Notebook on. I will write everything you say.")
    }

    fun writeVoiceNote(text: String) {
        if (voiceNote[0] == 0L) {
            val book = store.books.firstOrNull { it.name == "ভয়েস নোট" }
                ?: run {
                    store.addBook("ভয়েস নোট")
                    store.books.last()
                }
            voiceNote[0] = book.id
        }
        if (voiceNote[1] == 0L) {
            voiceNote[1] = store.addNote(voiceNote[0])
        }
        val bk = store.books.firstOrNull { it.id == voiceNote[0] } ?: return
        val nt = bk.notes.firstOrNull { it.id == voiceNote[1] }
        if (nt == null) {
            voiceNote[1] = 0L
            return
        }
        val title = if (nt.title.isEmpty())
            "ভয়েস " + java.text.SimpleDateFormat("dd/MM HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
        else nt.title
        val body = if (nt.body.isEmpty()) text else nt.body + "\n" + text
        store.saveNote(bk.id, nt.id, title, body)
        Toast.makeText(context, "✓ $text", Toast.LENGTH_SHORT).show()
    }

    fun handleVoice(text: String) {
        if (VoiceParser.wantsStop(text)) {
            voice.stop()
            return
        }
        if (voiceMode == "ask") {
            if (VoiceParser.wantsCalc(text)) { enterCalc(); return }
            if (VoiceParser.wantsNote(text)) { enterNote(); return }
            if (VoiceParser.toExpr(text) != null) {
                voiceMode = "calc"
            } else {
                voice.say("স্যার, ক্যালকুলেটর মোড নাকি নোট খাতা?", "Sir, calculator mode or notebook?")
                return
            }
        }
        if (voiceMode == "note") {
            if (VoiceParser.switchToCalc(text)) enterCalc() else writeVoiceNote(text)
            return
        }
        if (VoiceParser.switchToNote(text)) { enterNote(); return }
        val expr = VoiceParser.toExpr(text)
        val v = if (expr != null) evalExpr(expr) else null
        if (expr != null && v != null) {
            history.add("$expr = ${fmt(v)}")
            screen = Screen.Calculator
            Toast.makeText(context, "$expr = ${fmt(v)}", Toast.LENGTH_LONG).show()
            voice.say(
                VoiceParser.spoken(expr, true) + " সমান " + VoiceParser.spoken(fmt(v), true),
                VoiceParser.spoken(expr, false) + " equals " + VoiceParser.spoken(fmt(v), false)
            )
            return
        }
        val target = VoiceParser.toScreen(text)
        if (target != null) screen = target
        else Toast.makeText(context, "বুঝতে পারিনি: $text", Toast.LENGTH_SHORT).show()
    }

    fun startVoice() {
        listening = true
        voiceMode = "ask"
        voice.start(
            settings.voiceLang, settings.voiceSeconds,
            "স্যার, ক্যালকুলেটর মোড নাকি নোট খাতা?", "Sir, calculator mode or notebook?",
            { handleVoice(it) }, { listening = false }
        )
    }

'''
    a = "    val voice = remember { VoiceController(context) }"
    b = "    val micPermission"
    if a not in s or b not in s:
        print("anchor not found")
    else:
        i = s.index(a)
        j = s.index(b)
        open(p, "w", encoding="utf-8").write(s[:i] + block + s[j:])
        print("patched")
