package com.outlandishomar.acta.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "task_orders",
    primaryKeys = ["taskId", "dateMillis"],
    foreignKeys = [
        ForeignKey(
            entity = Task::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("taskId")]
)
data class TaskOrder(
    val taskId: Int,
    val dateMillis: Long,
    val orderIndex: Int
)
