package com.example.newsbite.data.repository

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.example.newsbite.BuildConfig
import com.example.newsbite.data.api.NewsApiService
import com.example.newsbite.data.local.CachedArticle
import com.example.newsbite.data.local.CachedArticleDao
import com.example.newsbite.data.local.NewsDatabase
import com.example.newsbite.data.model.Article
import com.example.newsbite.data.model.Source
import com.example.newsbite.data.paging.NewsRemoteMediator
import com.example.newsbite.util.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NewsRepository @Inject constructor(
    private val apiService: NewsApiService,
    private val cachedArticleDao: CachedArticleDao,
    private val database: NewsDatabase
) {
    companion object {
        private const val CACHE_TIMEOUT_MS = 15 * 60 * 1000L // 15 minutes
        private const val PAGE_SIZE = 20
        private const val PREFETCH_DISTANCE = 5
    }
    
    /**
     * Returns a Flow of PagingData for paginated news articles.
     * Uses RemoteMediator for offline-first behavior with automatic network fetching.
     * 
     * @param category The news category to fetch (e.g., "general", "technology")
     * @param query Optional search query to filter articles
     * @return Flow<PagingData<Article>> for use with PagingDataAdapter
     */
    @OptIn(ExperimentalPagingApi::class)
    fun getPagedNews(category: String, query: String? = null): Flow<PagingData<Article>> {
        val effectiveCategory = category.ifBlank { "general" }
        
        return Pager(
            config = PagingConfig(
                pageSize = PAGE_SIZE,
                enablePlaceholders = false,
                prefetchDistance = PREFETCH_DISTANCE
            ),
            remoteMediator = NewsRemoteMediator(
                category = effectiveCategory,
                query = query,
                apiService = apiService,
                database = database
            ),
            pagingSourceFactory = {
                if (query.isNullOrBlank()) {
                    cachedArticleDao.getArticlesPagingSource(effectiveCategory)
                } else {
                    cachedArticleDao.searchArticlesPagingSource(query)
                }
            }
        ).flow.map { pagingData ->
            pagingData.map { cachedArticle -> cachedArticle.toArticle() }
        }
    }
    
    suspend fun getTopHeadlines(
        category: String?,
        query: String?,
        forceRefresh: Boolean = false
    ): Resource<List<Article>> {
        return withContext(Dispatchers.IO) {
            val effectiveCategory = category ?: "general"
            
            // For search queries, try network first, fall back to cache
            if (!query.isNullOrBlank()) {
                return@withContext fetchWithSearchFallback(query)
            }
            
            // Check if cache is valid
            if (!forceRefresh && isCacheValid(effectiveCategory)) {
                val cached = getCachedArticles(effectiveCategory)
                if (cached.isNotEmpty()) {
                    return@withContext Resource.Success(cached)
                }
            }
            
            // Try network
            try {
                val response = apiService.getTopHeadlines(
                    apiKey = BuildConfig.NEWS_API_KEY,
                    language = "en",
                    category = category,
                    query = null
                )
                
                if (response.isSuccessful) {
                    val articles = response.body()?.articles
                    if (!articles.isNullOrEmpty()) {
                        // Cache the results
                        cacheArticles(articles, effectiveCategory)
                        return@withContext Resource.Success(articles)
                    } else {
                        // No articles from API, try cache
                        val cached = getCachedArticles(effectiveCategory)
                        return@withContext if (cached.isNotEmpty()) {
                            Resource.Success(cached)
                        } else {
                            Resource.Error("No articles found")
                        }
                    }
                } else {
                    // API error, try cache
                    val cached = getCachedArticles(effectiveCategory)
                    return@withContext if (cached.isNotEmpty()) {
                        Resource.Success(cached)
                    } else {
                        Resource.Error("Failed to load news: ${response.message()}")
                    }
                }
            } catch (e: Exception) {
                // Network error, try cache
                val cached = getCachedArticles(effectiveCategory)
                return@withContext if (cached.isNotEmpty()) {
                    Resource.Success(cached)
                } else {
                    Resource.Error("No internet connection and no cached data available")
                }
            }
        }
    }
    
    private suspend fun fetchWithSearchFallback(query: String): Resource<List<Article>> {
        return try {
            val response = apiService.getTopHeadlines(
                apiKey = BuildConfig.NEWS_API_KEY,
                language = "en",
                category = null,
                query = query
            )
            
            if (response.isSuccessful) {
                val articles = response.body()?.articles
                if (!articles.isNullOrEmpty()) {
                    Resource.Success(articles)
                } else {
                    // Search in cache
                    val cached = searchCachedArticles(query)
                    if (cached.isNotEmpty()) {
                        Resource.Success(cached)
                    } else {
                        Resource.Error("No articles found for '$query'")
                    }
                }
            } else {
                val cached = searchCachedArticles(query)
                if (cached.isNotEmpty()) {
                    Resource.Success(cached)
                } else {
                    Resource.Error("Search failed: ${response.message()}")
                }
            }
        } catch (e: Exception) {
            val cached = searchCachedArticles(query)
            if (cached.isNotEmpty()) {
                Resource.Success(cached)
            } else {
                Resource.Error("No internet connection")
            }
        }
    }
    
    private suspend fun isCacheValid(category: String): Boolean {
        val lastCacheTime = cachedArticleDao.getLastCacheTime(category) ?: return false
        return System.currentTimeMillis() - lastCacheTime < CACHE_TIMEOUT_MS
    }
    
    private suspend fun cacheArticles(articles: List<Article>, category: String) {
        // Clear old articles for this category
        cachedArticleDao.deleteByCategory(category)
        
        // Insert new articles
        val cachedArticles = articles.mapNotNull { article ->
            article.url?.let { url ->
                CachedArticle(
                    url = url,
                    title = article.title,
                    description = article.description,
                    urlToImage = article.urlToImage,
                    sourceName = article.source?.name,
                    publishedAt = article.publishedAt,
                    category = category
                )
            }
        }
        cachedArticleDao.insertAll(cachedArticles)
        
        // Clean up old cache entries (older than 24 hours)
        val oneDayAgo = System.currentTimeMillis() - (24 * 60 * 60 * 1000L)
        cachedArticleDao.deleteOlderThan(oneDayAgo)
    }
    
    private suspend fun getCachedArticles(category: String): List<Article> {
        return cachedArticleDao.getArticlesByCategory(category).map { it.toArticle() }
    }
    
    private suspend fun searchCachedArticles(query: String): List<Article> {
        return cachedArticleDao.searchArticles(query).map { it.toArticle() }
    }
    
    suspend fun clearCache() {
        withContext(Dispatchers.IO) {
            cachedArticleDao.clearAll()
        }
    }
    
    private fun CachedArticle.toArticle(): Article {
        return Article(
            title = title,
            description = description,
            url = url,
            urlToImage = urlToImage,
            publishedAt = publishedAt,
            source = Source(id = null, name = sourceName)
        )
    }
}
