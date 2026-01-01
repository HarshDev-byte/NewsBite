package com.example.newsbite.data.api

import com.example.newsbite.data.model.NewsResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface NewsApiService {
    
    @GET("top-headlines")
    suspend fun getTopHeadlines(
        @Query("apiKey") apiKey: String,
        @Query("language") language: String,
        @Query("category") category: String?,
        @Query("q") query: String?,
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 20
    ): Response<NewsResponse>
}
