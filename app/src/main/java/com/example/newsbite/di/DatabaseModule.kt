package com.example.newsbite.di

import android.content.Context
import androidx.room.Room
import com.example.newsbite.data.local.BookmarkDao
import com.example.newsbite.data.local.CachedArticleDao
import com.example.newsbite.data.local.NewsDatabase
import com.example.newsbite.data.local.RemoteKeysDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideNewsDatabase(@ApplicationContext context: Context): NewsDatabase {
        return Room.databaseBuilder(
            context,
            NewsDatabase::class.java,
            "news_database"
        )
            .fallbackToDestructiveMigration()
            .build()
    }
    
    @Provides
    @Singleton
    fun provideBookmarkDao(database: NewsDatabase): BookmarkDao {
        return database.bookmarkDao()
    }
    
    @Provides
    @Singleton
    fun provideCachedArticleDao(database: NewsDatabase): CachedArticleDao {
        return database.cachedArticleDao()
    }
    
    @Provides
    @Singleton
    fun provideRemoteKeysDao(database: NewsDatabase): RemoteKeysDao {
        return database.remoteKeysDao()
    }
}
