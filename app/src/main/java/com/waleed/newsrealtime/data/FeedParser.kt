package com.waleed.newsrealtime.data

import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream
import java.text.SimpleDateFormat
import java.time.OffsetDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.TimeZone

class RawItem {
    var title = ""
    var link = ""
    var summary = ""
    var date = ""
    var image: String? = null
    var publisher: String? = null
}

/** Reads RSS 2.0, RDF and Atom feeds. */
object FeedParser {

    private val IMG = Regex("""<img[^>]+src=["']([^"']+)["']""", RegexOption.IGNORE_CASE)

    private val DATE_FORMATS = listOf(
        "EEE, dd MMM yyyy HH:mm:ss Z",
        "EEE, d MMM yyyy HH:mm:ss Z",
        "EEE, dd MMM yyyy HH:mm:ss zzz",
        "EEE, d MMM yyyy HH:mm:ss zzz",
        "dd MMM yyyy HH:mm:ss Z",
        "EEE, dd MMM yyyy HH:mm Z",
        "EEE, dd MMM yyyy HH:mm:ss",
        "yyyy-MM-dd HH:mm:ss"
    )

    fun parse(input: InputStream): List<RawItem> {
        val items = ArrayList<RawItem>()
        val factory = XmlPullParserFactory.newInstance()
        factory.isNamespaceAware = true
        val p = factory.newPullParser()
        try {
            p.setFeature("http://xmlpull.org/v1/doc/features.html#relaxed", true)
        } catch (e: Exception) {
        }
        p.setInput(input, null)
        var cur: RawItem? = null
        try {
            var event = p.eventType
            while (event != XmlPullParser.END_DOCUMENT) {
                if (event == XmlPullParser.START_TAG) {
                    val name = (p.name ?: "").lowercase(Locale.ROOT)
                    val ns = p.namespace ?: ""
                    val c = cur
                    if (name == "item" || name == "entry") {
                        cur = RawItem()
                    } else if (c != null) {
                        when (name) {
                            "title" -> {
                                val t = readText(p)
                                if (c.title.isEmpty()) c.title = t
                            }
                            "link" -> {
                                val href = p.getAttributeValue(null, "href")
                                if (href != null) {
                                    val rel = p.getAttributeValue(null, "rel")
                                    if ((rel == null || rel == "alternate") && c.link.isEmpty()) c.link = href.trim()
                                } else {
                                    val t = readText(p)
                                    if (c.link.isEmpty() && t.isNotBlank()) c.link = t.trim()
                                }
                            }
                            "description", "summary", "encoded" -> {
                                val t = readText(p)
                                if (c.summary.isEmpty()) c.summary = t
                                if (c.image == null) c.image = imgFromHtml(t)
                            }
                            "content" -> {
                                if (ns.contains("mrss")) {
                                    val u = mediaUrl(p)
                                    if (u != null && c.image == null) c.image = u
                                } else {
                                    val t = readText(p)
                                    if (c.summary.isEmpty()) c.summary = t
                                    if (c.image == null) c.image = imgFromHtml(t)
                                }
                            }
                            "thumbnail" -> {
                                val u = p.getAttributeValue(null, "url")
                                if (u != null && c.image == null) c.image = u
                            }
                            "enclosure" -> {
                                val u = p.getAttributeValue(null, "url")
                                val type = p.getAttributeValue(null, "type") ?: ""
                                if (u != null && c.image == null && (type.startsWith("image") || type.isEmpty())) c.image = u
                            }
                            "pubdate", "published", "updated", "date", "issued" -> {
                                val t = readText(p)
                                if (c.date.isEmpty()) c.date = t.trim()
                            }
                            "source" -> {
                                if (!ns.contains("atom", ignoreCase = true)) {
                                    val t = readText(p)
                                    if (t.isNotBlank()) c.publisher = t.trim()
                                }
                            }
                        }
                    }
                } else if (event == XmlPullParser.END_TAG) {
                    val name = (p.name ?: "").lowercase(Locale.ROOT)
                    val done = cur
                    if ((name == "item" || name == "entry") && done != null) {
                        items.add(done)
                        cur = null
                    }
                }
                event = p.next()
            }
        } catch (e: Exception) {
            if (items.isEmpty()) throw e
        }
        return items
    }

    private fun mediaUrl(p: XmlPullParser): String? {
        val u = p.getAttributeValue(null, "url") ?: return null
        val type = p.getAttributeValue(null, "type") ?: ""
        val medium = p.getAttributeValue(null, "medium") ?: ""
        return if (type.startsWith("image") || medium == "image" || (type.isEmpty() && medium.isEmpty())) u else null
    }

    private fun imgFromHtml(html: String): String? {
        val m = IMG.find(html) ?: return null
        var u = m.groupValues[1]
        if (u.startsWith("//")) u = "https:" + u
        return if (u.startsWith("http")) u else null
    }

    private fun readText(p: XmlPullParser): String {
        val sb = StringBuilder()
        var depth = 1
        while (depth > 0) {
            when (p.next()) {
                XmlPullParser.TEXT -> sb.append(p.text)
                XmlPullParser.START_TAG -> depth++
                XmlPullParser.END_TAG -> depth--
                XmlPullParser.END_DOCUMENT -> return sb.toString()
            }
        }
        return sb.toString()
    }

    fun parseDate(s: String): Long {
        val t = s.trim()
        if (t.isEmpty()) return 0L
        try {
            return OffsetDateTime.parse(t).toInstant().toEpochMilli()
        } catch (e: Exception) {
        }
        try {
            return ZonedDateTime.parse(t, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant().toEpochMilli()
        } catch (e: Exception) {
        }
        for (f in DATE_FORMATS) {
            try {
                val fmt = SimpleDateFormat(f, Locale.US)
                if (!f.contains("Z") && !f.contains("z")) fmt.timeZone = TimeZone.getTimeZone("UTC")
                val d = fmt.parse(t)
                if (d != null) return d.time
            } catch (e: Exception) {
            }
        }
        return 0L
    }
}
