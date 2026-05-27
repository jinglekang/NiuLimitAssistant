package com.example.data

import kotlinx.coroutines.flow.Flow

class OperationLogRepository(private val operationLogDao: OperationLogDao) {
    val allLogs: Flow<List<OperationLog>> = operationLogDao.getAllLogs()

    suspend fun insertLog(log: OperationLog) {
        operationLogDao.insertLog(log)
    }

    suspend fun clearLogs() {
        operationLogDao.clearAllLogs()
    }
}
