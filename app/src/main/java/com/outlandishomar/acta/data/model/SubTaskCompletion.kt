package com.outlandishomar.acta.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "sub_task_completions",
    primaryKeys = ["subTaskId", "dateMillis"],
    foreignKeys = [
        ForeignKey(
            entity = SubTask::class,
            parentColumns = ["id"],
            childColumns = ["subTaskId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("subTaskId")]
)
data class SubTaskCompletion(
    val subTaskId: Int,
    val dateMillis: Long
)
