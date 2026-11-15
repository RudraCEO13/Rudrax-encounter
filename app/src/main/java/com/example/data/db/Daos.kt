package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomGameDao {
    @Query("SELECT * FROM custom_games ORDER BY createdAtMs DESC")
    fun observeAll(): Flow<List<CustomGameEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(game: CustomGameEntity)

    @Query("DELETE FROM custom_games WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface GestureMappingDao {
    @Query("SELECT * FROM gesture_mappings")
    fun observeAll(): Flow<List<GestureMappingEntity>>

    @Query("SELECT COUNT(*) FROM gesture_mappings")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(rules: List<GestureMappingEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(rule: GestureMappingEntity)
}

@Dao
interface SavedDrawingDao {
    @Query("SELECT * FROM saved_drawings ORDER BY createdAtMs DESC")
    fun observeAll(): Flow<List<SavedDrawingEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(drawing: SavedDrawingEntity)

    @Query("DELETE FROM saved_drawings WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface PairedDeviceDao {
    @Query("SELECT * FROM paired_devices ORDER BY lastConnectedMs DESC")
    fun observeAll(): Flow<List<PairedDeviceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(device: PairedDeviceEntity)

    @Query("DELETE FROM paired_devices WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface CustomObject3DDao {
    @Query("SELECT * FROM custom_objects_3d ORDER BY createdAtMs DESC")
    fun observeAll(): Flow<List<CustomObject3DEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(obj: CustomObject3DEntity)

    @Query("DELETE FROM custom_objects_3d WHERE id = :id")
    suspend fun deleteById(id: String)
}
