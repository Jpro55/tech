package com.qarin.wear.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory
import com.qarin.shared.crypto.CryptoManager

@Database(
    entities = [TaskEntity::class, SkillEntity::class, ReminderEntity::class, NoteEntity::class],
    version = 1,
    exportSchema = false
)
abstract class QarinWearDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun skillDao(): SkillDao
    abstract fun reminderDao(): ReminderDao
    abstract fun noteDao(): NoteDao

    companion object {
        @Volatile private var INSTANCE: QarinWearDatabase? = null

        fun getInstance(context: Context): QarinWearDatabase {
            return INSTANCE ?: synchronized(this) {
                val passphrase = CryptoManager.getDatabasePassphrase(context)
                val factory = SupportFactory(passphrase)
                Room.databaseBuilder(
                    context.applicationContext,
                    QarinWearDatabase::class.java,
                    "qarin_wear.db"
                )
                    .openHelperFactory(factory)
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
