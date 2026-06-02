package com.teacherscompanion.data.local.dao

import androidx.room.*
import com.teacherscompanion.data.local.entity.FaqItemCache

@Dao
interface FaqDao {
    @Query("SELECT * FROM faq_items_cache ORDER BY displayOrder")
    suspend fun getAll(): List<FaqItemCache>

    @Query("SELECT * FROM faq_items_cache WHERE question LIKE '%' || :query || '%' OR answer LIKE '%' || :query || '%'")
    suspend fun search(query: String): List<FaqItemCache>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<FaqItemCache>)

    @Query("DELETE FROM faq_items_cache")
    suspend fun deleteAll()
}
