package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.Collaborator
import kotlinx.coroutines.flow.Flow

@Dao
interface CollaboratorDao {
    @Query("SELECT * FROM collaborators ORDER BY name ASC")
    fun getAllCollaborators(): Flow<List<Collaborator>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(collaborator: Collaborator): Long

    @Update
    suspend fun update(collaborator: Collaborator)

    @Delete
    suspend fun delete(collaborator: Collaborator)

    @Query("DELETE FROM collaborators WHERE id = :id")
    suspend fun deleteById(id: Long)
}
