p = "app/src/main/java/com/calcora/app/MainActivity.kt"
s = open(p, encoding="utf-8").read()
if "VoiceController" in s:
    print("already patched")
else:
    anchor = "    BackHandler(enabled = screen != Screen.Home) { screen = Screen.Home }"
    block = '''    val voice = remember { VoiceController(context) }
    var listening by remember { mutableStateOf(false) }
    DisposableEffect(Unit) { onDispose { voice.stop() } }

    fun handleVoice(text: String) {
        val expr = VoiceParser.toExpr(text)
        val v = if (expr != null) evalExpr(expr) else null
        if (expr != null && v != null) {
            history.add("$expr = ${fmt(v)}")
            screen = Screen.Calculator
            Toast.makeText(context, "$expr = ${fmt(v)}", Toast.LENGTH_LONG).show()
            return
        }
        val target = VoiceParser.toScreen(text)
        if (target != null) screen = target
        else Toast.makeText(context, "বুঝতে পারিনি: $text", Toast.LENGTH_SHORT).show()
    }

    fun startVoice() {
        listening = true
        Toast.makeText(context, "শুনছি… আবার চাপলে বন্ধ", Toast.LENGTH_SHORT).show()
        voice.start(settings.voiceLang, settings.voiceSeconds, { handleVoice(it) }, { listening = false })
    }

    val micPermission = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startVoice()
        else Toast.makeText(context, "মাইক permission দরকার", Toast.LENGTH_SHORT).show()
    }

    fun onMicClick() {
        if (listening) {
            voice.stop()
            return
        }
        val ok = androidx.core.content.ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.RECORD_AUDIO
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (ok) startVoice() else micPermission.launch(android.Manifest.permission.RECORD_AUDIO)
    }

'''
    old = 'Toast.makeText(context, "Voice command coming soon", Toast.LENGTH_SHORT).show()'
    if anchor not in s or old not in s:
        print("anchor not found")
    else:
        s = s.replace(anchor, block + anchor, 1).replace(old, "onMicClick()", 1)
        open(p, "w", encoding="utf-8").write(s)
        print("patched")
