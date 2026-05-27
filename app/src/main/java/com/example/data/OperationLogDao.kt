package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface OperationLogDao {
    @Query("SELECT * FROM operation_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<OperationLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: OperationLog)

    @Query("DELETE FROM operation_logs")
    suspend fun clearAllLogs()
}
