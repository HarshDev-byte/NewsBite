package com.example.newsbite.data.local

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface BookmarkDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(article: BookmarkedArticle)
    
    @Delete
    suspend fun delete(article: BookmarkedArticle)
    
    @Query("DELETE FROM bookmarked_articles WHERE url = :url")
    suspend fun deleteByUrl(url: String)
    
    @Query("SELECT * FROM bookmarked_articles ORDER BY bookmarkedAt DESC")
    fun getAllBookmarks(): LiveData<List<BookmarkedArticle>>
    
    @Query("SELECT EXISTS(SELECT 1 FROM bookmarked_articles WHERE url = :url)")
    fun isBookmarked(url: String): LiveData<Boolean>
    
    @Query("SELECT EXISTS(SELECT 1 FROM bookmarked_articles WHERE url = :url)")
    suspend fun isBookmarkedSync(url: String): Boolean
}
