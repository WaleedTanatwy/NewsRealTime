package com.waleed.newsrealtime.data

data class Source(
    val id: String,
    val name: String,
    val url: String,
    val lang: String,
    val category: String,
    val enabled: Boolean = true,
    val custom: Boolean = false,
    val lastOk: Long = 0L,
    val lastError: String? = null,
    val lastCount: Int = 0
)

data class Article(
    val id: String,
    val sourceId: String,
    val sourceName: String,
    val title: String,
    val summary: String,
    val link: String,
    val imageUrl: String?,
    val published: Long,
    val fetched: Long,
    val lang: String,
    val category: String,
    val clusterId: String,
    val saved: Boolean = false,
    val read: Boolean = false,
    val breaking: Boolean = false
)

/** One news event, possibly reported by several outlets (like Nabd's "N sources"). */
data class Story(val lead: Article, val members: List<Article>) {
    val sourceCount: Int get() = members.map { it.sourceName }.distinct().size
    val latest: Long get() = members.maxOfOrNull { it.published } ?: lead.published
    val isBreaking: Boolean get() = members.any { it.breaking }
}

enum class Feed { HOME, BREAKING, SAVED }

data class FeedQuery(
    val feed: Feed = Feed.HOME,
    val lang: String = "all",
    val category: String? = null,
    val search: String = "",
    val sortTop: Boolean = false
)

data class RefreshResult(val fresh: List<Article>, val ok: Int, val failed: Int, val total: Int)
