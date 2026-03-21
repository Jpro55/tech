package com.qarin.mobile.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import net.sqlcipher.database.SupportFactory
import com.qarin.shared.crypto.CryptoManager

@Database(
    entities = [MobileTaskEntity::class, MobileSkillEntity::class,
        MobileReminderEntity::class, MobileNoteEntity::class],
    version = 1,
    exportSchema = false
)
abstract class QarinMobileDatabase : RoomDatabase() {
    abstract fun taskDao(): MobileTaskDao
    abstract fun skillDao(): MobileSkillDao
    abstract fun reminderDao(): MobileReminderDao
    abstract fun noteDao(): MobileNoteDao

    companion object {
        @Volatile private var INSTANCE: QarinMobileDatabase? = null

        fun getInstance(context: Context): QarinMobileDatabase {
            return INSTANCE ?: synchronized(this) {
                val passphrase = CryptoManager.getDatabasePassphrase(context)
                val factory = SupportFactory(passphrase)
                Room.databaseBuilder(
                    context.applicationContext,
                    QarinMobileDatabase::class.java,
                    "qarin_mobile.db"
                )
                    .openHelperFactory(factory)
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
