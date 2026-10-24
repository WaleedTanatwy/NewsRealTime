package com.waleed.newsrealtime.data

import android.text.Html
import java.security.MessageDigest

object TextUtil {

    private val STOP_RAW = listOf(
        "the", "and", "for", "with", "from", "that", "this", "after", "over", "into", "says", "said", "amid",
        "will", "have", "has", "its", "are", "was", "were", "new", "more", "news", "live", "updates", "update",
        "breaking", "what", "how", "why", "who", "than", "about", "just", "been", "they", "their", "his", "her",
        "not", "you", "your", "our", "but", "all", "can", "out", "may", "could", "would", "should", "first",
        "year", "years", "day", "week", "today", "here", "video", "watch",
        "في", "من", "على", "إلى", "الى", "عن", "مع", "بعد", "قبل", "خلال", "هذا", "هذه", "التي", "الذي",
        "الذين", "حول", "بين", "أن", "إن", "ان", "كان", "كانت", "قد", "لم", "لن", "ما", "لا", "عاجل", "يوم",
        "اليوم", "ذلك", "تلك", "أو", "او", "ثم", "حتى", "عند", "منذ", "وفق", "وسط", "أمام", "امام", "بشأن",
        "شأن", "فيديو", "صور", "بالصور", "بالفيديو"
    )

    private val STOP: Set<String> by lazy { STOP_RAW.map { normalize(it).trim() }.toSet() }

    private val BREAKING_EN = listOf("breaking", "urgent", "just in", "developing")

    /** Lower-case, remove Arabic diacritics and unify letter forms so the same word matches across outlets. */
    fun normalize(s: String): String {
        val sb = StringBuilder(s.length)
        for (ch in s.lowercase()) {
            val c = ch.code
            when {
                c in 0x064B..0x0652 || c == 0x0640 -> {}
                c == 0x0623 || c == 0x0625 || c == 0x0622 || c == 0x0671 -> sb.append(Char(0x0627))
                c == 0x0629 -> sb.append(Char(0x0647))
                c == 0x0649 -> sb.append(Char(0x064A))
                ch.isLetterOrDigit() -> sb.append(ch)
                else -> sb.append(' ')
            }
        }
        return sb.toString()
    }

    fun tokens(s: String): Set<String> {
        val out = HashSet<String>()
        for (t in normalize(s).split(' ')) {
            if (t.length < 3) continue
            var w = t
            if (w.length > 4 && (w.startsWith("وال") || w.startsWith("بال") || w.startsWith("كال") || w.startsWith("فال"))) {
                w = w.substring(3)
            } else if (w.length > 3 && w.startsWith("ال")) {
                w = w.substring(2)
            }
            if (w.length < 3 || w in STOP) continue
            out.add(w)
        }
        return out
    }

    /** Returns (similarity 0..1, number of shared words). */
    fun jaccard(a: Set<String>, b: Set<String>): Pair<Double, Int> {
        if (a.isEmpty() || b.isEmpty()) return Pair(0.0, 0)
        val small = if (a.size <= b.size) a else b
        val big = if (a.size <= b.size) b else a
        var inter = 0
        for (t in small) if (t in big) inter++
        val union = a.size + b.size - inter
        return Pair(inter.toDouble() / union, inter)
    }

    fun isArabic(s: String): Boolean {
        var ar = 0
        var lat = 0
        for (ch in s) {
            val c = ch.code
            if (c in 0x0600..0x06FF) ar++ else if (ch in 'a'..'z' || ch in 'A'..'Z') lat++
        }
        return ar > lat
    }

    fun stripHtml(s: String): String {
        if (s.isBlank()) return ""
        return collapse(Html.fromHtml(s, Html.FROM_HTML_MODE_LEGACY).toString())
    }

    fun collapse(s: String): String {
        val sb = StringBuilder(s.length)
        var space = false
        for (ch in s) {
            if (ch.isWhitespace() || ch.code == 0xFFFC || ch.code == 0xA0) {
                if (!space && sb.isNotEmpty()) {
                    sb.append(' ')
                    space = true
                }
            } else {
                sb.append(ch)
                space = false
            }
        }
        return sb.toString().trim()
    }

    fun isBreaking(title: String): Boolean {
        if (title.contains("عاجل")) return true
        val l = title.lowercase()
        return BREAKING_EN.any { l.contains(it) }
    }

    fun hashId(s: String): String {
        val d = MessageDigest.getInstance("SHA-1").digest(s.toByteArray())
        val sb = StringBuilder()
        for (i in 0 until 10) sb.append((d[i].toInt() and 0xff).toString(16).padStart(2, '0'))
        return sb.toString()
    }

    fun timeAgo(t: Long, now: Long = System.currentTimeMillis()): String {
        val m = (now - t) / 60000L
        return when {
            m < 1 -> "now"
            m < 60 -> "${m}m"
            m < 60 * 24 -> "${m / 60}h"
            else -> "${m / 1440}d"
        }
    }

    fun cleanTitle(title: String, publisher: String?): String {
        var t = stripHtml(title)
        if (publisher != null && publisher.isNotBlank()) {
            val suffix = " - " + publisher.trim()
            if (t.endsWith(suffix)) t = t.substring(0, t.length - suffix.length).trim()
        }
        return t
    }
}
