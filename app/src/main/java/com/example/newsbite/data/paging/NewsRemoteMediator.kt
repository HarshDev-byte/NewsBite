package com.example.newsbite.data.paging

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import com.example.newsbite.BuildConfig
import com.example.newsbite.data.api.NewsApiService
import com.example.newsbite.data.local.CachedArticle
import com.example.newsbite.data.local.NewsDatabase
import com.example.newsbite.data.local.RemoteKeys
import retrofit2.HttpException
import java.io.IOException

@OptIn(ExperimentalPagingApi::class)
class NewsRemoteMediator(
    private val category: String,
    private val query: String?,
    private val apiService: NewsApiService,
    private val database: NewsDatabase
) : RemoteMediator<Int, CachedArticle>() {

    companion object {
        private const val STARTING_PAGE_INDEX = 1
        private const val PAGE_SIZE = 20
    }

    private val cachedArticleDao = database.cachedArticleDao()
    private val remoteKeysDao = database.remoteKeysDao()

    override suspend fun initialize(): InitializeAction {
        // Check if we have cached data and if it's still fresh
        val lastCacheTime = cachedArticleDao.getLastCacheTime(category)
        val cacheTimeout = 15 * 60 * 1000L // 15 minutes
        
        return if (lastCacheTime != null && 
            System.currentTimeMillis() - lastCacheTime < cacheTimeout) {
            // Cache is still valid, skip initial refresh
            InitializeAction.SKIP_INITIAL_REFRESH
        } else {
            // Cache is stale or empty, launch initial refresh
            InitializeAction.LAUNCH_INITIAL_REFRESH
        }
    }


    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, CachedArticle>
    ): MediatorResult {
        val page = when (loadType) {
            LoadType.REFRESH -> {
                val remoteKeys = getRemoteKeyClosestToCurrentPosition(state)
                remoteKeys?.nextKey?.minus(1) ?: STARTING_PAGE_INDEX
            }
            LoadType.PREPEND -> {
                val remoteKeys = getRemoteKeyForFirstItem(state)
                val prevKey = remoteKeys?.prevKey
                    ?: return MediatorResult.Success(endOfPaginationReached = remoteKeys != null)
                prevKey
            }
            LoadType.APPEND -> {
                val remoteKeys = getRemoteKeyForLastItem(state)
                val nextKey = remoteKeys?.nextKey
                    ?: return MediatorResult.Success(endOfPaginationReached = remoteKeys != null)
                nextKey
            }
        }

        return try {
            val response = if (query.isNullOrBlank()) {
                apiService.getTopHeadlines(
                    apiKey = BuildConfig.NEWS_API_KEY,
                    language = "en",
                    category = category,
                    query = null,
                    page = page,
                    pageSize = PAGE_SIZE
                )
            } else {
                apiService.getTopHeadlines(
                    apiKey = BuildConfig.NEWS_API_KEY,
                    language = "en",
                    category = null,
                    query = query,
                    page = page,
                    pageSize = PAGE_SIZE
                )
            }

            if (!response.isSuccessful) {
                return MediatorResult.Error(HttpException(response))
            }

            val articles = response.body()?.articles ?: emptyList()
            val endOfPaginationReached = articles.isEmpty()

            database.withTransaction {
                // Clear all tables on refresh
                if (loadType == LoadType.REFRESH) {
                    remoteKeysDao.clearByCategory(category)
                    cachedArticleDao.deleteByCategory(category)
                }

                val prevKey = if (page == STARTING_PAGE_INDEX) null else page - 1
                val nextKey = if (endOfPaginationReached) null else page + 1

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

                val keys = cachedArticles.map { article ->
                    RemoteKeys(
                        articleUrl = article.url,
                        prevKey = prevKey,
                        nextKey = nextKey,
                        category = category
                    )
                }

                remoteKeysDao.insertAll(keys)
                cachedArticleDao.insertAll(cachedArticles)
            }

            MediatorResult.Success(endOfPaginationReached = endOfPaginationReached)
        } catch (e: IOException) {
            MediatorResult.Error(e)
        } catch (e: HttpException) {
            MediatorResult.Error(e)
        }
    }


    private suspend fun getRemoteKeyForLastItem(state: PagingState<Int, CachedArticle>): RemoteKeys? {
        return state.pages.lastOrNull { it.data.isNotEmpty() }?.data?.lastOrNull()
            ?.let { article ->
                remoteKeysDao.getRemoteKeyByArticleUrl(article.url)
            }
    }

    private suspend fun getRemoteKeyForFirstItem(state: PagingState<Int, CachedArticle>): RemoteKeys? {
        return state.pages.firstOrNull { it.data.isNotEmpty() }?.data?.firstOrNull()
            ?.let { article ->
                remoteKeysDao.getRemoteKeyByArticleUrl(article.url)
            }
    }

    private suspend fun getRemoteKeyClosestToCurrentPosition(
        state: PagingState<Int, CachedArticle>
    ): RemoteKeys? {
        return state.anchorPosition?.let { position ->
            state.closestItemToPosition(position)?.url?.let { url ->
                remoteKeysDao.getRemoteKeyByArticleUrl(url)
            }
        }
    }
}
