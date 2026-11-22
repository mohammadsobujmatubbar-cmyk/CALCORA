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
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.TextFieldValue
import org.json.JSONArray
import org.json.JSONObject

private val NbCyan = Color(0xFF00E5FF)
private val NbDim = Color(0xB3FFFFFF)

data class Note(val id: Long, val title: String, val body: String)
data class Notebook(val id: Long, val name: String, val notes: List<Note>)

class NotebookStore(context: Context) {
    private val prefs = context.getSharedPreferences("calcora_notebooks", Context.MODE_PRIVATE)
    val books = mutableStateListOf<Notebook>()

    init { load() }

    private fun load() {
        try {
            val arr = JSONArray(prefs.getString("books", "[]") ?: "[]")
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val na = o.getJSONArray("notes")
                val notes = (0 until na.length()).map { j ->
                    val n = na.getJSONObject(j)
                    Note(n.getLong("id"), n.getString("title"), n.getString("body"))
                }
                books.add(Notebook(o.getLong("id"), o.getString("name"), notes))
            }
        } catch (e: Exception) {
        }
    }

    private fun save() {
        val arr = JSONArray()
        books.forEach { b ->
            val na = JSONArray()
            b.notes.forEach { n ->
                na.put(JSONObject().put("id", n.id).put("title", n.title).put("body", n.body))
            }
            arr.put(JSONObject().put("id", b.id).put("name", b.name).put("notes", na))
        }
        prefs.edit().putString("books", arr.toString()).apply()
    }

    private fun update(bookId: Long, f: (Notebook) -> Notebook) {
        val i = books.indexOfFirst { it.id == bookId }
        if (i >= 0) {
            books[i] = f(books[i])
            save()
        }
    }

    fun addBook(name: String) {
        books.add(Notebook(System.currentTimeMillis(), name, emptyList()))
        save()
    }

    fun deleteBook(id: Long) {
        books.removeAll { it.id == id }
        save()
    }

    fun addNote(bookId: Long): Long {
        val id = System.currentTimeMillis()
        update(bookId) { it.copy(notes = listOf(Note(id, "", "")) + it.notes) }
        return id
    }

    fun saveNote(bookId: Long, id: Long, title: String, body: String) {
        update(bookId) { b -> b.copy(notes = b.notes.map { if (it.id == id) it.copy(title = title, body = body) else it }) }
    }

    fun deleteNote(bookId: Long, id: Long) {
        update(bookId) { b -> b.copy(notes = b.notes.filter { it.id != id }) }
    }
}

@Composable
fun NotebooksScreen(store: NotebookStore) {
    var bookId by remember { mutableStateOf<Long?>(null) }
    var noteId by remember { mutableStateOf<Long?>(null) }

    fun closeNote() {
        val b = bookId
        val n = noteId
        if (b != null && n != null) {
            val note = store.books.firstOrNull { it.id == b }?.notes?.firstOrNull { it.id == n }
            if (note != null && note.title.isBlank() && note.body.isBlank()) store.deleteNote(b, n)
        }
        noteId = null
    }

    BackHandler(enabled = bookId != null) {
        if (noteId != null) closeNote() else bookId = null
    }

    val book = store.books.firstOrNull { it.id == bookId }
    val note = book?.notes?.firstOrNull { it.id == noteId }
    when {
        book == null -> BookList(store) { bookId = it }
        note != null -> NoteEditor(store, book.id, note) { closeNote() }
        else -> NoteList(store, book, { bookId = null }) { noteId = it }
    }
}

@Composable
fun NbButton(label: String, onClick: () -> Unit) {
    Text(
        label, color = Color(0xFF001133), fontSize = 14.sp, fontWeight = FontWeight.Bold,
        modifier = Modifier.clip(RoundedCornerShape(14.dp)).background(NbCyan)
            .clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 8.dp)
    )
}

@Composable
fun NbBack(onClick: () -> Unit) {
    Text(
        "←", color = NbCyan, fontSize = 26.sp,
        modifier = Modifier.clip(CircleShape).clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 2.dp)
    )
}

@Composable
fun BookList(store: NotebookStore, onOpen: (Long) -> Unit) {
    var name by remember { mutableStateOf("") }
    var confirmId by remember { mutableStateOf<Long?>(null) }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Notebooks", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Row(
            Modifier.fillMaxWidth().glass(RoundedCornerShape(16.dp))
                .padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.weight(1f)) {
                if (name.isEmpty()) Text("New notebook name…", color = Color(0x80FFFFFF), fontSize = 16.sp)
                BasicTextField(
                    value = name, onValueChange = { name = it }, singleLine = true,
                    textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
                    cursorBrush = SolidColor(NbCyan), modifier = Modifier.fillMaxWidth()
                )
            }
            NbButton("＋ Add") {
                if (name.isNotBlank()) {
                    store.addBook(name.trim())
                    name = ""
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        if (store.books.isEmpty()) {
            Text("No notebooks yet. Create your first one above.", color = NbDim, fontSize = 14.sp)
        } else {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                store.books.forEach { b ->
                    Row(
                        Modifier.fillMaxWidth().glass(RoundedCornerShape(16.dp))
                            .clickable { onOpen(b.id) }.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("📓", fontSize = 26.sp)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(b.name, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                            Text("${b.notes.size} notes", color = NbDim, fontSize = 12.sp)
                        }
                        val ask = confirmId == b.id
                        Text(
                            if (ask) "Delete?" else "🗑",
                            color = if (ask) Color(0xFFFF5252) else Color.White,
                            fontSize = 16.sp,
                            modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable {
                                if (ask) {
                                    store.deleteBook(b.id)
                                    confirmId = null
                                } else confirmId = b.id
                            }.padding(8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NoteList(store: NotebookStore, book: Notebook, onBack: () -> Unit, onOpen: (Long) -> Unit) {
    var confirmId by remember { mutableStateOf<Long?>(null) }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            NbBack(onBack)
            Text(book.name, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 1, modifier = Modifier.weight(1f))
            NbButton("＋ Note") { onOpen(store.addNote(book.id)) }
        }
        Spacer(Modifier.height(12.dp))
        if (book.notes.isEmpty()) {
            Text("No notes yet. Tap ＋ Note to write one.", color = NbDim, fontSize = 14.sp)
        } else {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                book.notes.forEach { n ->
                    Row(
                        Modifier.fillMaxWidth().glass(RoundedCornerShape(16.dp))
                            .clickable { onOpen(n.id) }.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                if (n.title.isBlank()) "Untitled" else n.title,
                                color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1
                            )
                            if (n.body.isNotBlank()) Text(n.body, color = NbDim, fontSize = 13.sp, maxLines = 2)
                        }
                        val ask = confirmId == n.id
                        Text(
                            if (ask) "Delete?" else "🗑",
                            color = if (ask) Color(0xFFFF5252) else Color.White,
                            fontSize = 16.sp,
                            modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable {
                                if (ask) {
                                    store.deleteNote(book.id, n.id)
                                    confirmId = null
                                } else confirmId = n.id
                            }.padding(8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NoteEditor(store: NotebookStore, bookId: Long, note: Note, onBack: () -> Unit) {
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
            Text("Auto-saved ✅", color = NbDim, fontSize = 12.sp)
        }
        Spacer(Modifier.height(8.dp))
        Box(Modifier.fillMaxWidth()) {
            if (title.text.isEmpty()) Text("Title", color = Color(0x80FFFFFF), fontSize = 22.sp, fontWeight = FontWeight.Bold)
            BasicTextField(
                value = title,
                onValueChange = {
                    title = it
                    store.saveNote(bookId, note.id, it.text, body.text)
                },
                singleLine = true,
                textStyle = TextStyle(color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold),
                cursorBrush = SolidColor(NbCyan),
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
                    store.saveNote(bookId, note.id, title.text, it.text)
                },
                textStyle = TextStyle(color = Color.White, fontSize = 17.sp),
                cursorBrush = SolidColor(NbCyan),
                modifier = Modifier.fillMaxWidth().heightIn(min = 400.dp)
            )
        }
    }
}
