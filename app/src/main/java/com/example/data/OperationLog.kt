package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Entity(tableName = "operation_logs")
data class OperationLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val deviceName: String,
    val macAddress: String,
    val timestamp: Long = System.currentTimeMillis(),
    val commandHex: String,
    val isSuccess: Boolean,
    val statusMessage: String
) {
    val formattedTime: String
        get() {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            return sdf.format(Date(timestamp))
        }
}
