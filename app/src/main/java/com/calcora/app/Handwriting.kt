package com.calcora.app

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject

private val HwCyan = Color(0xFF00E5FF)
private val HwDim = Color(0xB3FFFFFF)

fun buildInkPath(pts: List<Offset>): Path {
    val path = Path()
    if (pts.isNotEmpty()) {
        path.moveTo(pts[0].x, pts[0].y)
        for (i in 1 until pts.size) path.lineTo(pts[i].x, pts[i].y)
    }
    return path
}

class InkStroke(val color: Long, val width: Float, val points: List<Offset>) {
    val path: Path by lazy { buildInkPath(points) }
}

class HwStore(context: Context) {
    private val prefs = context.getSharedPreferences("calcora_handwriting", Context.MODE_PRIVATE)
    val pages = mutableStateListOf<List<InkStroke>>()
    var index by mutableStateOf(0)
        private set
    var canUndo by mutableStateOf(false)
        private set
    var canRedo by mutableStateOf(false)
        private set
    private val undoStack = mutableListOf<List<InkStroke>>()
    private val redoStack = mutableListOf<List<InkStroke>>()
    private var pending: List<InkStroke>? = null

    init { load() }

    private fun flags() {
        canUndo = undoStack.isNotEmpty()
        canRedo = redoStack.isNotEmpty()
    }

    private fun resetStacks() {
        undoStack.clear()
        redoStack.clear()
        flags()
    }

    private fun load() {
        try {
            val arr = JSONArray(prefs.getString("pages", "[]") ?: "[]")
            for (i in 0 until arr.length()) {
                val sa = arr.getJSONArray(i)
                val page = ArrayList<InkStroke>()
                for (j in 0 until sa.length()) {
                    val o = sa.getJSONObject(j)
                    val pts = o.getString("p").split(";").filter { it.isNotEmpty() }.map {
                        val xy = it.split(",")
                        Offset(xy[0].toFloat(), xy[1].toFloat())
                    }
                    if (pts.isNotEmpty()) page.add(InkStroke(o.getLong("c"), o.getDouble("w").toFloat(), pts))
                }
                pages.add(page)
            }
        } catch (e: Exception) {
        }
        if (pages.isEmpty()) pages.add(emptyList())
        index = prefs.getInt("index", 0).coerceIn(0, pages.size - 1)
    }

    private fun save() {
        val arr = JSONArray()
        pages.forEach { page ->
            val sa = JSONArray()
            page.forEach { st ->
                val sb = StringBuilder()
                st.points.forEachIndexed { i, o ->
                    if (i > 0) sb.append(';')
                    sb.append(o.x.toInt()).append(',').append(o.y.toInt())
                }
                sa.put(JSONObject().put("c", st.color).put("w", st.width.toDouble()).put("p", sb.toString()))
            }
            arr.put(sa)
        }
        prefs.edit().putString("pages", arr.toString()).putInt("index", index).apply()
    }

    fun begin() {
        pending = pages[index]
    }

    fun end() {
        val p = pending
        pending = null
        if (p != null && p !== pages[index]) {
            undoStack.add(p)
            if (undoStack.size > 50) undoStack.removeAt(0)
            redoStack.clear()
            flags()
            save()
        }
    }

    fun addStroke(s: InkStroke) {
        pages[index] = pages[index] + s
    }

    fun erase(pos: Offset, r: Float) {
        val cur = pages[index]
        val keep = cur.filter { st -> st.points.none { (it - pos).getDistance() <= r } }
        if (keep.size != cur.size) pages[index] = keep
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            redoStack.add(pages[index])
            pages[index] = undoStack.removeAt(undoStack.size - 1)
            flags()
            save()
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            undoStack.add(pages[index])
            pages[index] = redoStack.removeAt(redoStack.size - 1)
            flags()
            save()
        }
    }

    fun clearPage() {
        if (pages[index].isNotEmpty()) {
            undoStack.add(pages[index])
            redoStack.clear()
            pages[index] = emptyList()
            flags()
            save()
        }
    }

    fun goto(i: Int) {
        if (i in pages.indices) {
            index = i
            resetStacks()
            save()
        }
    }

    fun addPage() {
        pages.add(emptyList())
        index = pages.size - 1
        resetStacks()
        save()
    }
}

private fun DrawScope.ink(points: List<Offset>, path: Path, color: Color, w: Float) {
    if (points.size == 1) {
        drawCircle(color, radius = w / 2f, center = points[0])
    } else if (points.size > 1) {
        drawPath(path, color, style = Stroke(width = w, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun HandwritingScreen(store: HwStore) {
    var tool by remember { mutableStateOf(0) }
    var color by remember { mutableStateOf(0xFFFFFFFFL) }
    var width by remember { mutableStateOf(4f) }
    val live = remember { mutableStateListOf<Offset>() }
    val colors = listOf(0xFFFFFFFFL, 0xFF00E5FFL, 0xFFFFD740L, 0xFFFF4081L, 0xFF69F0AEL)

    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Text("Handwriting", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Box(Modifier.weight(1f).fillMaxWidth().glass(RoundedCornerShape(20.dp))) {
            Canvas(
                Modifier.fillMaxSize().pointerInput(tool, color, width) {
                    val radius = 14.dp.toPx()
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        store.begin()
                        if (tool == 1) {
                            store.erase(down.position, radius)
                            drag(down.id) { ch ->
                                store.erase(ch.position, radius)
                                ch.consume()
                            }
                        } else {
                            live.clear()
                            live.add(down.position)
                            drag(down.id) { ch ->
                                live.add(ch.position)
                                ch.consume()
                            }
                            if (live.isNotEmpty()) store.addStroke(InkStroke(color, width, live.toList()))
                            live.clear()
                        }
                        store.end()
                    }
                }
            ) {
                store.pages[store.index].forEach { st ->
                    ink(st.points, st.path, Color(st.color), st.width.dp.toPx())
                }
                if (live.isNotEmpty()) {
                    val pts = live.toList()
                    ink(pts, buildInkPath(pts), Color(color), width.dp.toPx())
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            HwTool("✏️", tool == 0) { tool = 0 }
            HwTool("🧽", tool == 1) { tool = 1 }
            Spacer(Modifier.width(6.dp))
            colors.forEach { c ->
                HwDot(color == c && tool == 0, 18.dp, Color(c)) {
                    color = c
                    tool = 0
                }
            }
            Spacer(Modifier.width(6.dp))
            listOf(3f to 8.dp, 6f to 14.dp, 12f to 20.dp).forEach { (w, d) ->
                HwDot(width == w && tool == 0, d, Color(0xCCFFFFFF)) {
                    width = w
                    tool = 0
                }
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            HwAction("◀", store.index > 0) { store.goto(store.index - 1) }
            Text("${store.index + 1}/${store.pages.size}", color = HwDim, fontSize = 14.sp)
            HwAction("▶", store.index < store.pages.size - 1) { store.goto(store.index + 1) }
            HwAction("＋", true) { store.addPage() }
            Spacer(Modifier.weight(1f))
            HwAction("↩", store.canUndo) { store.undo() }
            HwAction("↪", store.canRedo) { store.redo() }
            HwAction("🗑", store.pages[store.index].isNotEmpty()) { store.clearPage() }
        }
    }
}

@Composable
fun HwTool(icon: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.size(38.dp).clip(RoundedCornerShape(12.dp))
            .background(if (selected) Color(0x4400E5FF) else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(icon, fontSize = 20.sp)
    }
}

@Composable
fun HwDot(selected: Boolean, size: Dp, color: Color, onClick: () -> Unit) {
    Box(Modifier.size(32.dp).clip(CircleShape).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Box(
            Modifier.size(size).clip(CircleShape).background(color)
                .border(if (selected) 2.dp else 1.dp, if (selected) HwCyan else Color(0x55FFFFFF), CircleShape)
        )
    }
}

@Composable
fun HwAction(label: String, enabled: Boolean, onClick: () -> Unit) {
    Text(
        label,
        fontSize = 20.sp,
        color = if (enabled) Color.White else Color(0x44FFFFFF),
        modifier = Modifier.clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    )
}
