package com.example.newsbite.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [BookmarkedArticle::class, CachedArticle::class, RemoteKeys::class],
    version = 3,
    exportSchema = false
)
abstract class NewsDatabase : RoomDatabase() {
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun cachedArticleDao(): CachedArticleDao
    abstract fun remoteKeysDao(): RemoteKeysDao
}
