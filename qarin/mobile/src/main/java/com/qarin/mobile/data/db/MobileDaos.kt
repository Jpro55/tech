package com.qarin.mobile.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MobileTaskDao {
    @Query("SELECT * FROM tasks ORDER BY createdAt DESC")
    fun getAllFlow(): Flow<List<MobileTaskEntity>>
    @Query("SELECT * FROM tasks ORDER BY createdAt DESC")
    suspend fun getAll(): List<MobileTaskEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: MobileTaskEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tasks: List<MobileTaskEntity>)
    @Update
    suspend fun update(task: MobileTaskEntity)
    @Delete
    suspend fun delete(task: MobileTaskEntity)
    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteById(id: String)
    @Query("UPDATE tasks SET syncStatus = 'SYNCED' WHERE id = :id")
    suspend fun markSynced(id: String)
}

@Dao
interface MobileSkillDao {
    @Query("SELECT * FROM skills ORDER BY name ASC")
    fun getAllFlow(): Flow<List<MobileSkillEntity>>
    @Query("SELECT * FROM skills ORDER BY name ASC")
    suspend fun getAll(): List<MobileSkillEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(skill: MobileSkillEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(skills: List<MobileSkillEntity>)
    @Update
    suspend fun update(skill: MobileSkillEntity)
    @Delete
    suspend fun delete(skill: MobileSkillEntity)
    @Query("UPDATE skills SET syncStatus = 'SYNCED' WHERE id = :id")
    suspend fun markSynced(id: String)
}

@Dao
interface MobileReminderDao {
    @Query("SELECT * FROM reminders ORDER BY triggerTime ASC")
    fun getAllFlow(): Flow<List<MobileReminderEntity>>
    @Query("SELECT * FROM reminders ORDER BY triggerTime ASC")
    suspend fun getAll(): List<MobileReminderEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reminder: MobileReminderEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(reminders: List<MobileReminderEntity>)
    @Update
    suspend fun update(reminder: MobileReminderEntity)
    @Delete
    suspend fun delete(reminder: MobileReminderEntity)
    @Query("UPDATE reminders SET syncStatus = 'SYNCED' WHERE id = :id")
    suspend fun markSynced(id: String)
}

@Dao
interface MobileNoteDao {
    @Query("SELECT * FROM notes ORDER BY createdAt DESC")
    fun getAllFlow(): Flow<List<MobileNoteEntity>>
    @Query("SELECT * FROM notes ORDER BY createdAt DESC")
    suspend fun getAll(): List<MobileNoteEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: MobileNoteEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(notes: List<MobileNoteEntity>)
    @Update
    suspend fun update(note: MobileNoteEntity)
    @Delete
    suspend fun delete(note: MobileNoteEntity)
    @Query("UPDATE notes SET syncStatus = 'SYNCED' WHERE id = :id")
    suspend fun markSynced(id: String)
}
