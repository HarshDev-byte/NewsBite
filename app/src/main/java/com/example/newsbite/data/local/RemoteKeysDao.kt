package com.example.newsbite.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface RemoteKeysDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(remoteKeys: List<RemoteKeys>)

    @Query("SELECT * FROM remote_keys WHERE articleUrl = :articleUrl")
    suspend fun getRemoteKeyByArticleUrl(articleUrl: String): RemoteKeys?

    @Query("DELETE FROM remote_keys WHERE category = :category")
    suspend fun clearByCategory(category: String)

    @Query("DELETE FROM remote_keys")
    suspend fun clearAll()
}
