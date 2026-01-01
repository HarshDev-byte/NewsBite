package com.example.newsbite.data.repository

import androidx.lifecycle.LiveData
import com.example.newsbite.data.local.BookmarkDao
import com.example.newsbite.data.local.BookmarkedArticle
import com.example.newsbite.data.model.Article
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookmarkRepository @Inject constructor(
    private val bookmarkDao: BookmarkDao
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    fun toggleBookmark(article: Article) {
        scope.launch {
            val url = article.url ?: return@launch
            if (bookmarkDao.isBookmarkedSync(url)) {
                bookmarkDao.deleteByUrl(url)
            } else {
                val bookmarked = BookmarkedArticle(
                    url = url,
                    title = article.title,
                    description = article.description,
                    urlToImage = article.urlToImage,
                    sourceName = article.source?.name,
                    publishedAt = article.publishedAt
                )
                bookmarkDao.insert(bookmarked)
            }
        }
    }
    
    fun removeBookmark(url: String) {
        scope.launch {
            bookmarkDao.deleteByUrl(url)
        }
    }
    
    fun getAllBookmarks(): LiveData<List<BookmarkedArticle>> = bookmarkDao.getAllBookmarks()
    
    fun isBookmarked(url: String): LiveData<Boolean> = bookmarkDao.isBookmarked(url)
}
