package com.calcora.app

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = false
        setContent { CalcoraApp() }
    }
}

enum class Screen(val title: String, val icon: String) {
    Home("Home", "🏠"),
    Calculator("Calculator", "🧮"),
    History("History", "🕘"),
    Notebooks("Notebooks", "📒"),
    Handwriting("Handwriting", "✍️"),
    Vault("Secure Vault", "🔒"),
    Theme("Theme", "🎨"),
    More("More", "⋯")
}

private val Cyan = Color(0xFF00E5FF)
private val GlassFill = Color(0x1AFFFFFF)
private val GlassBorder = Color(0x33FFFFFF)

fun Modifier.glass(
    shape: Shape = RoundedCornerShape(22.dp),
    brush: Brush = Brush.linearGradient(listOf(GlassFill, GlassFill))
): Modifier = this
    .clip(shape)
    .background(brush, shape)
    .border(1.dp, GlassBorder, shape)

// ---------------------------------------------------------------- App root

@Composable
fun CalcoraApp() {
    var screen by remember { mutableStateOf(Screen.Home) }
    val context = LocalContext.current
    val history = remember { mutableStateListOf<String>().apply { addAll(loadHistory(context)) } }
    LaunchedEffect(history.size) { saveHistory(context, history) }

    BackHandler(enabled = screen != Screen.Home) { screen = Screen.Home }

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF050B2A), Color(0xFF0B1550), Color(0xFF1B0B4F))
                )
            )
    ) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Box(Modifier.weight(1f).fillMaxWidth()) {
                when (screen) {
                    Screen.Home -> HomeScreen(history) { screen = it }
                    Screen.Calculator -> CalculatorScreen(history)
                    Screen.History -> HistoryScreen(history)
                    else -> PlaceholderScreen(screen)
                }
            }
            BottomBar(
                current = screen,
                onSelect = { screen = it },
                onMic = {
                    Toast.makeText(context, "Voice command coming soon", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}

// ---------------------------------------------------------------- Home

@Composable
fun HomeScreen(history: List<String>, go: (Screen) -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF00E5FF), Color(0xFF7C4DFF), Color(0xFFFF4081))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text("C", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("CALCORA", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
                Text("Calculate • Write • Remember", color = Color(0xCCFFFFFF), fontSize = 11.sp, maxLines = 1)
            }
            Column(
                Modifier
                    .glass(RoundedCornerShape(20.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text("Hello, Friend! 👋", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text("Smart Ideas,\nBigger Plans", color = Color(0xB3FFFFFF), fontSize = 10.sp)
            }
        }

        // Hero
        Row(
            Modifier
                .fillMaxWidth()
                .glass(
                    RoundedCornerShape(24.dp),
                    Brush.linearGradient(listOf(Color(0x66304FFE), Color(0x3300E5FF)))
                )
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("Your Smart", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("Calculator &", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("Digital Ledger", color = Cyan, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text("Calculate, Write, Save...\nAll in One Place!", color = Color(0xCCFFFFFF), fontSize = 12.sp)
                Spacer(Modifier.height(10.dp))
                Box(
                    Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Cyan)
                        .clickable { go(Screen.Calculator) }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text("Explore Features →", color = Color(0xFF001133), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
            Text("🧮📓", fontSize = 40.sp)
        }

        // Feature grid
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FeatureCard("🧮", "Calculator", "Smart & Fast", Color(0x552979FF), Color(0x2200B0FF), Modifier.weight(1f)) { go(Screen.Calculator) }
            FeatureCard("📒", "Notebooks", "Take Notes", Color(0x55AA00FF), Color(0x226200EA), Modifier.weight(1f)) { go(Screen.Notebooks) }
            FeatureCard("✍️", "Handwriting", "Write Freely", Color(0x5500C853), Color(0x2200BFA5), Modifier.weight(1f)) { go(Screen.Handwriting) }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FeatureCard("🕘", "History", "View Past", Color(0x55FF6D00), Color(0x22FFAB00), Modifier.weight(1f)) { go(Screen.History) }
            FeatureCard("🔒", "Secure Vault", "Keep Private", Color(0x55D50000), Color(0x22C51162), Modifier.weight(1f)) { go(Screen.Vault) }
            FeatureCard("🎨", "Theme", "Dark / Light", Color(0x553D5AFE), Color(0x22651FFF), Modifier.weight(1f)) { go(Screen.Theme) }
        }

        // Recent activity + notebooks
        Row(
            Modifier.fillMaxWidth().height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            InfoCard("🕒", "Recent Activity", Modifier.weight(1f).fillMaxHeight()) {
                val recent = history.takeLast(3).reversed()
                if (recent.isEmpty()) {
                    Text("No calculations yet", color = Color(0xB3FFFFFF), fontSize = 12.sp)
                } else {
                    recent.forEach {
                        Text("🧮 $it", color = Color.White, fontSize = 13.sp, maxLines = 1, modifier = Modifier.padding(vertical = 3.dp))
                    }
                }
            }
            InfoCard("🔖", "My Notebooks", Modifier.weight(1f).fillMaxHeight()) {
                NotebookRow("📓", "Personal Notes", "3 notes")
                NotebookRow("📗", "Study Guide", "5 notes")
                NotebookRow("📙", "Work Planner", "2 notes")
            }
        }

        // Auto save + storage
        Row(
            Modifier.fillMaxWidth().height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .glass(RoundedCornerShape(18.dp), Brush.linearGradient(listOf(Color(0x5500C853), Color(0x2200E676))))
                    .padding(12.dp)
            ) {
                Text("🛡️ Auto Save Enabled ✅", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text("Your data is always safe", color = Color(0xCCFFFFFF), fontSize = 11.sp)
            }
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .glass(RoundedCornerShape(18.dp), Brush.linearGradient(listOf(Color(0x55651FFF), Color(0x223D5AFE))))
                    .padding(12.dp)
            ) {
                Text("💾 Storage Used", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text("6.2 MB / 50 MB", color = Color(0xCCFFFFFF), fontSize = 11.sp)
                Spacer(Modifier.height(6.dp))
                Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(Color(0x33FFFFFF))) {
                    Box(
                        Modifier
                            .fillMaxWidth(0.12f)
                            .fillMaxHeight()
                            .background(Brush.horizontalGradient(listOf(Cyan, Color(0xFF7C4DFF))))
                    )
                }
            }
        }
    }
}

@Composable
fun FeatureCard(
    icon: String,
    title: String,
    sub: String,
    c1: Color,
    c2: Color,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier
            .glass(brush = Brush.linearGradient(listOf(c1, c2)))
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(icon, fontSize = 30.sp)
        Spacer(Modifier.height(6.dp))
        Text(title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, textAlign = TextAlign.Center)
        Text(sub, color = Color(0xB3FFFFFF), fontSize = 11.sp, maxLines = 1, textAlign = TextAlign.Center)
    }
}

@Composable
fun InfoCard(icon: String, title: String, modifier: Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.glass().padding(12.dp)) {
        Text("$icon  $title", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        content()
    }
}

@Composable
fun NotebookRow(icon: String, title: String, sub: String) {
    Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(icon, fontSize = 20.sp)
        Spacer(Modifier.width(8.dp))
        Column {
            Text(title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            Text(sub, color = Color(0xB3FFFFFF), fontSize = 11.sp)
        }
    }
}

// ---------------------------------------------------------------- Bottom bar

@Composable
fun BottomBar(current: Screen, onSelect: (Screen) -> Unit, onMic: () -> Unit) {
    val barShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .height(72.dp)
            .background(Color(0xE60A1040), barShape)
            .border(1.dp, GlassBorder, barShape),
        verticalAlignment = Alignment.CenterVertically
    ) {
        NavItem(Screen.Home, current == Screen.Home, Modifier.weight(1f)) { onSelect(Screen.Home) }
        NavItem(Screen.Calculator, current == Screen.Calculator, Modifier.weight(1f)) { onSelect(Screen.Calculator) }
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .offset(y = (-12).dp)
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(Color(0xFF3D5AFE), Color(0xFFD500F9))))
                    .border(2.dp, Cyan.copy(alpha = 0.6f), CircleShape)
                    .clickable(onClick = onMic),
                contentAlignment = Alignment.Center
            ) {
                Text("🎤", fontSize = 28.sp)
            }
        }
        NavItem(Screen.Notebooks, current == Screen.Notebooks, Modifier.weight(1f)) { onSelect(Screen.Notebooks) }
        NavItem(Screen.More, current == Screen.More, Modifier.weight(1f)) { onSelect(Screen.More) }
    }
}

@Composable
fun NavItem(screen: Screen, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier.fillMaxHeight().clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(screen.icon, fontSize = 22.sp)
        Text(
            screen.title,
            fontSize = 11.sp,
            maxLines = 1,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) Cyan else Color(0xB3FFFFFF)
        )
    }
}

// ---------------------------------------------------------------- Calculator

@Composable
fun CalculatorScreen(history: MutableList<String>) {
    var expr by remember { mutableStateOf("") }
    var fresh by remember { mutableStateOf(false) }

    val preview = if (expr.any { it in "+−×÷%" }) evalExpr(expr)?.let { fmt(it) } ?: "" else ""

    fun press(k: String) {
        when (k) {
            "AC" -> {
                expr = ""
                fresh = false
            }
            "⌫" -> {
                expr = expr.dropLast(1)
                fresh = false
            }
            "=" -> {
                val v = evalExpr(expr)
                if (v != null) {
                    history.add("$expr = ${fmt(v)}")
                    expr = fmt(v)
                    fresh = true
                }
            }
            "+", "−", "×", "÷" -> {
                fresh = false
                if (expr.isNotEmpty()) {
                    expr = if (expr.last() in "+−×÷") expr.dropLast(1) + k else expr + k
                } else if (k == "−") {
                    expr = "−"
                }
            }
            "%" -> {
                if (expr.isNotEmpty() && expr.last().isDigit()) expr += "%"
            }
            "." -> {
                if (fresh) {
                    expr = "0."
                    fresh = false
                } else {
                    val lastNum = expr.takeLastWhile { it.isDigit() || it == '.' }
                    if (!lastNum.contains('.')) expr += if (lastNum.isEmpty()) "0." else "."
                }
            }
            else -> {
                if (fresh) {
                    expr = k
                    fresh = false
                } else {
                    expr += k
                }
            }
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Calculator", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Column(
            Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.End
        ) {
            Text(preview, color = Cyan, fontSize = 22.sp, maxLines = 1)
            Text(
                if (expr.isEmpty()) "0" else expr,
                color = Color.White,
                fontSize = 48.sp,
                fontWeight = FontWeight.Light,
                maxLines = 2,
                textAlign = TextAlign.End
            )
        }
        Spacer(Modifier.height(12.dp))
        Column(
            Modifier.fillMaxWidth().weight(1.8f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val rows = listOf(
                listOf("AC", "⌫", "%", "÷"),
                listOf("7", "8", "9", "×"),
                listOf("4", "5", "6", "−"),
                listOf("1", "2", "3", "+"),
                listOf("00", "0", ".", "=")
            )
            rows.forEach { r ->
                Row(
                    Modifier.fillMaxWidth().weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    r.forEach { k ->
                        CalcKey(
                            k,
                            Modifier.weight(1f).fillMaxHeight(),
                            accent = k in listOf("÷", "×", "−", "+", "=")
                        ) { press(k) }
                    }
                }
            }
        }
    }
}

@Composable
fun CalcKey(label: String, modifier: Modifier, accent: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(22.dp)
    Box(
        modifier
            .clip(shape)
            .background(if (accent) Cyan.copy(alpha = 0.9f) else Color(0x22FFFFFF), shape)
            .border(1.dp, GlassBorder, shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = if (accent) Color(0xFF001133) else Color.White,
            fontSize = 26.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

// ---------------------------------------------------------------- History / placeholders

private const val PREFS = "calcora_prefs"
private const val KEY_HISTORY = "history"

fun loadHistory(context: Context): List<String> {
    val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_HISTORY, "") ?: ""
    return if (raw.isEmpty()) emptyList() else raw.split("\n")
}

fun saveHistory(context: Context, history: List<String>) {
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .edit()
        .putString(KEY_HISTORY, history.takeLast(200).joinToString("\n"))
        .apply()
}

@Composable
fun HistoryScreen(history: List<String>) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("History", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        if (history.isEmpty()) {
            Text("No calculations yet", color = Color(0xB3FFFFFF), fontSize = 14.sp)
        } else {
            history.reversed().forEach {
                Text(
                    it,
                    color = Color.White,
                    fontSize = 18.sp,
                    modifier = Modifier.fillMaxWidth().glass(RoundedCornerShape(16.dp)).padding(14.dp)
                )
            }
        }
    }
}

@Composable
fun PlaceholderScreen(screen: Screen) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(screen.icon, fontSize = 56.sp)
            Spacer(Modifier.height(8.dp))
            Text(screen.title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text("Coming soon", color = Color(0xB3FFFFFF), fontSize = 14.sp)
        }
    }
}

// ---------------------------------------------------------------- Calculator engine

fun fmt(v: Double): String =
    java.math.BigDecimal(v)
        .setScale(8, java.math.RoundingMode.HALF_UP)
        .stripTrailingZeros()
        .toPlainString()

fun evalExpr(s: String): Double? {
    return try {
        val p = ExprParser(s.replace("×", "*").replace("÷", "/").replace("−", "-"))
        val v = p.parseExpr()
        if (p.pos != p.src.length || v.isNaN() || v.isInfinite()) null else v
    } catch (e: Exception) {
        null
    }
}

class ExprParser(val src: String) {
    var pos = 0

    fun parseExpr(): Double {
        var v = parseTerm()
        while (pos < src.length && (src[pos] == '+' || src[pos] == '-')) {
            val op = src[pos++]
            val r = parseTerm()
            v = if (op == '+') v + r else v - r
        }
        return v
    }

    private fun parseTerm(): Double {
        var v = parseFactor()
        while (pos < src.length && (src[pos] == '*' || src[pos] == '/')) {
            val op = src[pos++]
            val r = parseFactor()
            v = if (op == '*') v * r else v / r
        }
        return v
    }

    private fun parseFactor(): Double {
        var neg = false
        if (pos < src.length && src[pos] == '-') {
            neg = true
            pos++
        }
        val start = pos
        while (pos < src.length && (src[pos].isDigit() || src[pos] == '.')) pos++
        var v = src.substring(start, pos).toDouble()
        while (pos < src.length && src[pos] == '%') {
            v /= 100.0
            pos++
        }
        return if (neg) -v else v
    }
}
