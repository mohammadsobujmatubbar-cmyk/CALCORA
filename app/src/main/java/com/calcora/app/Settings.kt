package com.calcora.app

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("calcora_settings", Context.MODE_PRIVATE)
    var voiceLang by mutableStateOf(prefs.getString("voice_lang", "bn") ?: "bn")
        private set
    var voiceSeconds by mutableStateOf(prefs.getInt("voice_seconds", 30))
        private set

    fun updateVoiceLang(v: String) {
        voiceLang = v
        prefs.edit().putString("voice_lang", v).apply()
    }

    fun updateVoiceSeconds(v: Int) {
        voiceSeconds = v
        prefs.edit().putInt("voice_seconds", v).apply()
    }
}

fun buildBackup(context: Context): String {
    val a = context.getSharedPreferences("calcora_prefs", Context.MODE_PRIVATE)
    val b = context.getSharedPreferences("calcora_notebooks", Context.MODE_PRIVATE)
    val c = context.getSharedPreferences("calcora_handwriting", Context.MODE_PRIVATE)
    val s = context.getSharedPreferences("calcora_settings", Context.MODE_PRIVATE)
    return JSONObject()
        .put("app", "CALCORA")
        .put("format", 1)
        .put("history", a.getString("history", "") ?: "")
        .put("books", b.getString("books", "[]") ?: "[]")
        .put("pages", c.getString("pages", "[]") ?: "[]")
        .put("pageIndex", c.getInt("index", 0))
        .put("voiceLang", s.getString("voice_lang", "bn") ?: "bn")
        .put("voiceSeconds", s.getInt("voice_seconds", 30))
        .toString(2)
}

fun restoreBackup(context: Context, text: String): Boolean {
    try {
        val o = JSONObject(text)
        if (o.optString("app") != "CALCORA") return false
        val books = o.optString("books", "[]")
        val pages = o.optString("pages", "[]")
        JSONArray(books)
        JSONArray(pages)
        context.getSharedPreferences("calcora_prefs", Context.MODE_PRIVATE).edit()
            .putString("history", o.optString("history", "")).commit()
        context.getSharedPreferences("calcora_notebooks", Context.MODE_PRIVATE).edit()
            .putString("books", books).commit()
        context.getSharedPreferences("calcora_handwriting", Context.MODE_PRIVATE).edit()
            .putString("pages", pages).putInt("index", o.optInt("pageIndex", 0)).commit()
        context.getSharedPreferences("calcora_settings", Context.MODE_PRIVATE).edit()
            .putString("voice_lang", o.optString("voiceLang", "bn"))
            .putInt("voice_seconds", o.optInt("voiceSeconds", 30)).commit()
        return true
    } catch (e: Exception) {
        return false
    }
}

@Composable
fun SetChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        label,
        color = if (selected) Color(0xFF001133) else Color.White,
        fontSize = 14.sp,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        modifier = Modifier.clip(RoundedCornerShape(16.dp))
            .background(if (selected) Color(0xFF00E5FF) else Color(0x22FFFFFF))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    )
}

@Composable
fun SettingsScreen(settings: SettingsStore) {
    val context = LocalContext.current
    val ver = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    val saver = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri: Uri? ->
        if (uri != null) {
            var ok = true
            try {
                context.contentResolver.openOutputStream(uri, "wt")?.use {
                    it.write(buildBackup(context).toByteArray(Charsets.UTF_8))
                }
            } catch (e: Exception) {
                ok = false
            }
            Toast.makeText(context, if (ok) "Backup saved ✅" else "Backup failed", Toast.LENGTH_LONG).show()
        }
    }

    val loader = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            var text: String? = null
            try {
                text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            } catch (e: Exception) {
            }
            if (text != null && restoreBackup(context, text)) {
                Toast.makeText(context, "Restored ✅", Toast.LENGTH_LONG).show()
                (context as? Activity)?.recreate()
            } else {
                Toast.makeText(context, "Not a valid CALCORA backup", Toast.LENGTH_LONG).show()
            }
        }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("⚙️ Settings", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)

        InfoCard("ℹ️", "About", Modifier.fillMaxWidth()) {
            Text("CALCORA $ver", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text("Calculate • Write • Remember", color = Color(0xB3FFFFFF), fontSize = 13.sp)
        }

        InfoCard("💾", "Backup & Restore", Modifier.fillMaxWidth()) {
            Text(
                "Save your notebooks, handwriting and history as a file in your phone's memory, or bring them back from a backup file.",
                color = Color(0xB3FFFFFF), fontSize = 13.sp
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                NbButton("💾 Save Backup") {
                    val name = "CALCORA-backup-" + SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(Date()) + ".json"
                    saver.launch(name)
                }
                NbButton("📂 Restore") { loader.launch(arrayOf("*/*")) }
            }
        }

        InfoCard("🎙️", "Voice Settings", Modifier.fillMaxWidth()) {
            Text("Voice language", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SetChip("বাংলা", settings.voiceLang == "bn") { settings.updateVoiceLang("bn") }
                SetChip("English", settings.voiceLang == "en") { settings.updateVoiceLang("en") }
            }
            Spacer(Modifier.height(12.dp))
            Text("Keep voice on for", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(15 to "15s", 30 to "30s", 60 to "1m", 120 to "2m", 300 to "5m").forEach { (sec, label) ->
                    SetChip(label, settings.voiceSeconds == sec) { settings.updateVoiceSeconds(sec) }
                }
            }
        }
    }
}
