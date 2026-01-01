package com.example.newsbite.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bookmarked_articles")
data class BookmarkedArticle(
    @PrimaryKey
    val url: String,
    val title: String?,
    val description: String?,
    val urlToImage: String?,
    val sourceName: String?,
    val publishedAt: String?,
    val bookmarkedAt: Long = System.currentTimeMillis()
)
