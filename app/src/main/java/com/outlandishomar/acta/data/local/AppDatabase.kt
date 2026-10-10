package com.outlandishomar.acta.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.outlandishomar.acta.data.model.Task
import com.outlandishomar.acta.data.model.SubTask
import com.outlandishomar.acta.data.model.TaskCompletion
import com.outlandishomar.acta.data.model.SubTaskCompletion
import com.outlandishomar.acta.data.model.TaskException
import com.outlandishomar.acta.data.model.TaskOrder

@Database(entities = [Task::class, SubTask::class, TaskCompletion::class, SubTaskCompletion::class, TaskException::class, TaskOrder::class], version = 12, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun taskDao(): TaskDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS `task_completions` (`taskId` INTEGER NOT NULL, `dateMillis` INTEGER NOT NULL, PRIMARY KEY(`taskId`, `dateMillis`), FOREIGN KEY(`taskId`) REFERENCES `tasks`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_task_completions_taskId` ON `task_completions` (`taskId`)")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE `tasks` ADD COLUMN `endDate` INTEGER DEFAULT NULL")
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE `tasks` ADD COLUMN `alertEnabled` INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE `tasks` ADD COLUMN `additionalAlerts` TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS `sub_task_completions` (`subTaskId` INTEGER NOT NULL, `dateMillis` INTEGER NOT NULL, PRIMARY KEY(`subTaskId`, `dateMillis`), FOREIGN KEY(`subTaskId`) REFERENCES `sub_tasks`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_sub_task_completions_subTaskId` ON `sub_task_completions` (`subTaskId`)")
            }
        }

        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS `task_exceptions` (`taskId` INTEGER NOT NULL, `dateMillis` INTEGER NOT NULL, PRIMARY KEY(`taskId`, `dateMillis`), FOREIGN KEY(`taskId`) REFERENCES `tasks`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_task_exceptions_taskId` ON `task_exceptions` (`taskId`)")
            }
        }

        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS `task_orders` (`taskId` INTEGER NOT NULL, `dateMillis` INTEGER NOT NULL, `orderIndex` INTEGER NOT NULL, PRIMARY KEY(`taskId`, `dateMillis`), FOREIGN KEY(`taskId`) REFERENCES `tasks`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_task_orders_taskId` ON `task_orders` (`taskId`)")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "task_database"
                )
                .addMigrations(MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12)
                .fallbackToDestructiveMigration()
                .build().also { INSTANCE = it }
            }
        }
    }
}
