package com.calcora.app

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.security.SecureRandom

private val VCyan = Color(0xFF00E5FF)
private val VDim = Color(0xB3FFFFFF)
private val VRed = Color(0xFFFF5252)

class VaultStore(context: Context) {
    private val prefs = context.getSharedPreferences("calcora_vault", Context.MODE_PRIVATE)
    val notes = mutableStateListOf<Note>()

    init { load() }

    private fun load() {
        try {
            val arr = JSONArray(prefs.getString("notes", "[]") ?: "[]")
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                notes.add(Note(o.getLong("id"), o.getString("title"), o.getString("body")))
            }
        } catch (e: Exception) {
        }
    }

    private fun save() {
        val arr = JSONArray()
        notes.forEach { arr.put(JSONObject().put("id", it.id).put("title", it.title).put("body", it.body)) }
        prefs.edit().putString("notes", arr.toString()).apply()
    }

    fun hasPin(): Boolean = !prefs.getString("pin_hash", "").isNullOrEmpty()

    private fun hash(salt: String, pin: String): String {
        val d = MessageDigest.getInstance("SHA-256").digest((salt + pin).toByteArray())
        return d.joinToString("") { "%02x".format(it) }
    }

    fun setPin(pin: String) {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        val salt = bytes.joinToString("") { "%02x".format(it) }
        prefs.edit().putString("salt", salt).putString("pin_hash", hash(salt, pin)).apply()
    }

    fun check(pin: String): Boolean {
        val salt = prefs.getString("salt", "") ?: ""
        return hash(salt, pin) == (prefs.getString("pin_hash", "") ?: "")
    }

    fun addNote(): Long {
        val id = System.currentTimeMillis()
        notes.add(0, Note(id, "", ""))
        save()
        return id
    }

    fun saveNote(id: Long, title: String, body: String) {
        val i = notes.indexOfFirst { it.id == id }
        if (i >= 0) {
            notes[i] = notes[i].copy(title = title, body = body)
            save()
        }
    }

    fun deleteNote(id: Long) {
        notes.removeAll { it.id == id }
        save()
    }
}

@Composable
fun VaultScreen() {
    val context = LocalContext.current
    val store = remember { VaultStore(context) }
    var hasPin by remember { mutableStateOf(store.hasPin()) }
    var unlocked by remember { mutableStateOf(false) }
    var openId by remember { mutableStateOf<Long?>(null) }
    var changing by remember { mutableStateOf(false) }

    fun closeEditor() {
        val n = store.notes.firstOrNull { it.id == openId }
        if (n != null && n.title.isEmpty() && n.body.isEmpty()) store.deleteNote(n.id)
        openId = null
    }

    BackHandler(enabled = unlocked && (openId != null || changing)) {
        if (changing) changing = false else closeEditor()
    }

    when {
        !hasPin -> PinSetup("নতুন PIN সেট করুন") { pin ->
            store.setPin(pin)
            hasPin = true
            unlocked = true
        }
        !unlocked -> PinEntry(store) { unlocked = true }
        changing -> PinSetup("নতুন PIN দিন") { pin ->
            store.setPin(pin)
            changing = false
        }
        openId != null -> {
            val note = store.notes.firstOrNull { it.id == openId }
            if (note == null) openId = null
            else VaultEditor(store, note) { closeEditor() }
        }
        else -> VaultList(
            store,
            onOpen = { openId = it },
            onChangePin = { changing = true },
            onLock = { unlocked = false }
        )
    }
}

@Composable
fun PinPad(title: String, sub: String, filled: Int, error: String, onKey: (String) -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("🔒", fontSize = 44.sp)
        Spacer(Modifier.height(10.dp))
        Text(title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(sub, color = VDim, fontSize = 14.sp)
        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            repeat(4) { i ->
                Box(
                    Modifier.size(18.dp).clip(CircleShape)
                        .background(if (i < filled) VCyan else Color(0x33FFFFFF))
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(if (error.isEmpty()) " " else error, color = VRed, fontSize = 13.sp)
        Spacer(Modifier.height(12.dp))
        listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("", "0", "⌫")
        ).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                row.forEach { k ->
                    if (k.isEmpty()) {
                        Spacer(Modifier.size(64.dp))
                    } else {
                        Box(
                            Modifier.size(64.dp).clip(CircleShape)
                                .background(Color(0x1AFFFFFF))
                                .clickable { onKey(k) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(k, color = Color.White, fontSize = 24.sp)
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
fun PinSetup(title: String, onDone: (String) -> Unit) {
    var stage by remember { mutableStateOf(0) }
    var first by remember { mutableStateOf("") }
    var input by remember { mutableStateOf("") }
    var err by remember { mutableStateOf("") }

    fun key(k: String) {
        if (k == "⌫") {
            input = input.dropLast(1)
            return
        }
        if (input.length >= 4) return
        input += k
        if (input.length == 4) {
            if (stage == 0) {
                first = input
                input = ""
                stage = 1
                err = ""
            } else if (input == first) {
                onDone(input)
            } else {
                err = "PIN মেলেনি, আবার করুন"
                stage = 0
                first = ""
                input = ""
            }
        }
    }

    PinPad(
        title = title,
        sub = if (stage == 0) "৪ সংখ্যার PIN দিন" else "আবার একই PIN দিন",
        filled = input.length,
        error = err,
        onKey = { key(it) }
    )
}

@Composable
fun PinEntry(store: VaultStore, onUnlocked: () -> Unit) {
    var input by remember { mutableStateOf("") }
    var err by remember { mutableStateOf("") }
    var fails by remember { mutableStateOf(0) }
    var lockUntil by remember { mutableStateOf(0L) }

    fun key(k: String) {
        if (System.currentTimeMillis() < lockUntil) {
            err = "৩০ সেকেন্ড পরে চেষ্টা করুন"
            return
        }
        if (k == "⌫") {
            input = input.dropLast(1)
            return
        }
        if (input.length >= 4) return
        input += k
        if (input.length == 4) {
            if (store.check(input)) {
                onUnlocked()
            } else {
                fails += 1
                input = ""
                if (fails >= 5) {
                    lockUntil = System.currentTimeMillis() + 30000L
                    fails = 0
                    err = "৫ বার ভুল — ৩০ সেকেন্ড অপেক্ষা করুন"
                } else {
                    err = "ভুল PIN (আর ${5 - fails} বার)"
                }
            }
        }
    }

    PinPad(
        title = "Secure Vault",
        sub = "PIN দিয়ে আনলক করুন",
        filled = input.length,
        error = err,
        onKey = { key(it) }
    )
}

@Composable
fun VaultList(store: VaultStore, onOpen: (Long) -> Unit, onChangePin: () -> Unit, onLock: () -> Unit) {
    var del by remember { mutableStateOf<Long?>(null) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "🔓 Secure Vault", color = Color.White, fontSize = 22.sp,
                fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f)
            )
            NbButton("+ নোট") { onOpen(store.addNote()) }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            Text("🔒 লক করুন", color = VCyan, fontSize = 14.sp, modifier = Modifier.clickable { onLock() })
            Text("🔑 PIN বদলান", color = VCyan, fontSize = 14.sp, modifier = Modifier.clickable { onChangePin() })
        }
        Spacer(Modifier.height(12.dp))
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
            if (store.notes.isEmpty()) {
                Text("এখানে কোনো গোপন নোট নেই। উপরে + নোট চাপুন।", color = VDim, fontSize = 15.sp)
            }
            store.notes.forEach { n ->
                Row(
                    Modifier.fillMaxWidth().padding(bottom = 10.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0x1AFFFFFF))
                        .clickable { onOpen(n.id) }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (n.title.isEmpty()) "(শিরোনামহীন)" else n.title,
                            color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold,
                            maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                        if (n.body.isNotEmpty()) {
                            Text(
                                n.body, color = VDim, fontSize = 14.sp,
                                maxLines = 1, overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Text("🗑", fontSize = 20.sp, modifier = Modifier.clickable { del = n.id }.padding(6.dp))
                }
            }
        }
    }

    if (del != null) {
        AlertDialog(
            onDismissRequest = { del = null },
            title = { Text("নোট মুছবেন?") },
            text = { Text("এই নোট চিরতরে মুছে যাবে।") },
            confirmButton = {
                Text("মুছুন", modifier = Modifier.clickable {
                    store.deleteNote(del!!)
                    del = null
                }.padding(12.dp))
            },
            dismissButton = {
                Text("না", modifier = Modifier.clickable { del = null }.padding(12.dp))
            }
        )
    }
}

@Composable
fun VaultEditor(store: VaultStore, note: Note, onBack: () -> Unit) {
    var title by remember(note.id) { mutableStateOf(TextFieldValue(note.title)) }
    var body by remember(note.id) { mutableStateOf(TextFieldValue(note.body)) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(note.id) {
        try {
            focus.requestFocus()
        } catch (e: Exception) {
        }
    }

    Column(Modifier.fillMaxSize().imePadding().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            NbBack(onBack)
            Text("🔒 Auto-saved ✅", color = VDim, fontSize = 12.sp)
        }
        Spacer(Modifier.height(8.dp))
        Box(Modifier.fillMaxWidth()) {
            if (title.text.isEmpty()) Text("Title", color = Color(0x80FFFFFF), fontSize = 22.sp, fontWeight = FontWeight.Bold)
            BasicTextField(
                value = title,
                onValueChange = {
                    title = it
                    store.saveNote(note.id, it.text, body.text)
                },
                singleLine = true,
                textStyle = TextStyle(color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold),
                cursorBrush = SolidColor(VCyan),
                modifier = Modifier.fillMaxWidth().focusRequester(focus)
            )
        }
        Spacer(Modifier.height(10.dp))
        Box(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
            if (body.text.isEmpty()) Text("Write here…", color = Color(0x80FFFFFF), fontSize = 17.sp)
            BasicTextField(
                value = body,
                onValueChange = {
                    body = it
                    store.saveNote(note.id, title.text, it.text)
                },
                textStyle = TextStyle(color = Color.White, fontSize = 17.sp),
                cursorBrush = SolidColor(VCyan),
                modifier = Modifier.fillMaxWidth().heightIn(min = 400.dp)
            )
        }
    }
}
