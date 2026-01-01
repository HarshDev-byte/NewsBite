package com.example.newsbite.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_articles")
data class CachedArticle(
    @PrimaryKey
    val url: String,
    val title: String?,
    val description: String?,
    val urlToImage: String?,
    val sourceName: String?,
    val publishedAt: String?,
    val category: String,
    val cachedAt: Long = System.currentTimeMillis()
)
