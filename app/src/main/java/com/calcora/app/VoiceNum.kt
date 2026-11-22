package com.calcora.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object VoiceBus {
    var expr by mutableStateOf("")
    var tick by mutableStateOf(0)
    var seen = 0
}

object VoiceNum {
    private const val BN = "০১২৩৪৫৬৭৮৯"

    private val bnWords = listOf(
        "শূন্য", "এক", "দুই", "তিন", "চার", "পাঁচ", "ছয়", "সাত", "আট", "নয়",
        "দশ", "এগারো", "বারো", "তেরো", "চৌদ্দ", "পনেরো", "ষোলো", "সতেরো", "আঠারো", "উনিশ",
        "বিশ", "একুশ", "বাইশ", "তেইশ", "চব্বিশ", "পঁচিশ", "ছাব্বিশ", "সাতাশ", "আঠাশ", "ঊনত্রিশ",
        "ত্রিশ", "একত্রিশ", "বত্রিশ", "তেত্রিশ", "চৌত্রিশ", "পঁয়ত্রিশ", "ছত্রিশ", "সাঁইত্রিশ", "আটত্রিশ", "ঊনচল্লিশ",
        "চল্লিশ", "একচল্লিশ", "বিয়াল্লিশ", "তেতাল্লিশ", "চুয়াল্লিশ", "পঁয়তাল্লিশ", "ছেচল্লিশ", "সাতচল্লিশ", "আটচল্লিশ", "ঊনপঞ্চাশ",
        "পঞ্চাশ", "একান্ন", "বাহান্ন", "তিপ্পান্ন", "চুয়ান্ন", "পঞ্চান্ন", "ছাপ্পান্ন", "সাতান্ন", "আটান্ন", "ঊনষাট",
        "ষাট", "একষট্টি", "বাষট্টি", "তেষট্টি", "চৌষট্টি", "পঁয়ষট্টি", "ছেষট্টি", "সাতষট্টি", "আটষট্টি", "ঊনসত্তর",
        "সত্তর", "একাত্তর", "বাহাত্তর", "তিয়াত্তর", "চুয়াত্তর", "পঁচাত্তর", "ছিয়াত্তর", "সাতাত্তর", "আটাত্তর", "ঊনআশি",
        "আশি", "একাশি", "বিরাশি", "তিরাশি", "চুরাশি", "পঁচাশি", "ছিয়াশি", "সাতাশি", "অষ্টাশি", "ঊননব্বই",
        "নব্বই", "একানব্বই", "বিরানব্বই", "তিরানব্বই", "চুরানব্বই", "পঁচানব্বই", "ছিয়ানব্বই", "সাতানব্বই", "আটানব্বই", "নিরানব্বই"
    )

    private val nums: Map<String, Int> =
        bnWords.withIndex().associate { it.value to it.index } + mapOf(
            "পাচ" to 5, "জিরো" to 0, "zero" to 0,
            "ওয়ান" to 1, "one" to 1, "টু" to 2, "two" to 2, "থ্রি" to 3, "three" to 3,
            "ফোর" to 4, "four" to 4, "ফাইভ" to 5, "five" to 5, "সিক্স" to 6, "six" to 6,
            "সেভেন" to 7, "seven" to 7, "এইট" to 8, "eight" to 8, "নাইন" to 9, "nine" to 9,
            "টেন" to 10, "ten" to 10
        )

    private val scales: Map<String, Long> = mapOf(
        "শ" to 100L, "শো" to 100L, "শত" to 100L, "hundred" to 100L,
        "হাজার" to 1000L, "thousand" to 1000L,
        "লাখ" to 100000L, "লক্ষ" to 100000L
    )

    private val ops = mapOf(
        "+" to "+", "যোগ" to "+", "প্লাস" to "+", "plus" to "+", "add" to "+",
        "-" to "−", "−" to "−", "বিয়োগ" to "−", "মাইনাস" to "−", "minus" to "−", "subtract" to "−",
        "*" to "×", "×" to "×", "x" to "×", "গুণ" to "×", "গুন" to "×", "টাইমস" to "×",
        "ইনটু" to "×", "ইন্টু" to "×", "মাল্টিপ্লাই" to "×", "times" to "×", "multiply" to "×", "into" to "×",
        "/" to "÷", "÷" to "÷", "ভাগ" to "÷", "ডিভাইডেড" to "÷", "ডিভাইড" to "÷", "divided" to "÷", "divide" to "÷",
        "%" to "%", "শতাংশ" to "%", "পার্সেন্ট" to "%", "percent" to "%",
        "দশমিক" to ".", "পয়েন্ট" to ".", "point" to ".", "dot" to "."
    )

    fun toExpr(raw: String): String? {
        var t = raw.lowercase()
        for ((i, c) in BN.withIndex()) t = t.replace(c, '0' + i)
        t = t.replace(Regex("(?<=\\d),(?=\\d{3})"), "")
        t = t.replace(Regex("(?<=\\d)\\s*x\\s*(?=\\d)"), " × ")
        t = t.replace(Regex("([+\\-−*/×÷%])"), " \$1 ")

        val out = StringBuilder()
        var total = 0L
        var cur = 0L
        var inNum = false

        fun flush() {
            if (inNum) {
                out.append(total + cur)
                total = 0L
                cur = 0L
                inNum = false
            }
        }

        for (tok in t.split(Regex("\\s+"))) {
            if (tok.isEmpty()) continue
            val op = ops[tok]
            if (op != null) {
                flush()
                out.append(op)
                continue
            }
            if (Regex("[0-9.]+").matches(tok)) {
                flush()
                out.append(tok)
                continue
            }
            val n = nums[tok]
            if (n != null) {
                cur += n
                inNum = true
                continue
            }
            val sc = scales[tok]
            if (sc != null) {
                if (sc >= 1000L) {
                    total += (if (cur == 0L) 1L else cur) * sc
                    cur = 0L
                } else {
                    cur = (if (cur == 0L) 1L else cur) * sc
                }
                inNum = true
                continue
            }
            if (tok.endsWith("শো")) {
                val b = nums[tok.dropLast(2)]
                if (b != null) {
                    cur += b * 100L
                    inNum = true
                }
            }
        }
        flush()

        val e = out.toString()
        return if (e.any { it in "+−×÷" } && e.any { it.isDigit() }) e else null
    }
}
