package com.example.newsbite.data.local

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface CachedArticleDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(articles: List<CachedArticle>)
    
    @Query("SELECT * FROM cached_articles WHERE category = :category ORDER BY cachedAt DESC")
    suspend fun getArticlesByCategory(category: String): List<CachedArticle>
    
    @Query("SELECT * FROM cached_articles WHERE category = :category ORDER BY cachedAt DESC")
    fun getArticlesPagingSource(category: String): PagingSource<Int, CachedArticle>
    
    @Query("SELECT * FROM cached_articles WHERE title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%' ORDER BY cachedAt DESC")
    suspend fun searchArticles(query: String): List<CachedArticle>
    
    @Query("SELECT * FROM cached_articles WHERE (title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%') ORDER BY cachedAt DESC")
    fun searchArticlesPagingSource(query: String): PagingSource<Int, CachedArticle>
    
    @Query("DELETE FROM cached_articles WHERE category = :category")
    suspend fun deleteByCategory(category: String)
    
    @Query("DELETE FROM cached_articles WHERE cachedAt < :timestamp")
    suspend fun deleteOlderThan(timestamp: Long)
    
    @Query("SELECT MAX(cachedAt) FROM cached_articles WHERE category = :category")
    suspend fun getLastCacheTime(category: String): Long?
    
    @Query("DELETE FROM cached_articles")
    suspend fun clearAll()
}
