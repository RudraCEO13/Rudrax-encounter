package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        CustomGameEntity::class,
        GestureMappingEntity::class,
        SavedDrawingEntity::class,
        PairedDeviceEntity::class,
        CustomObject3DEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class MotionXDatabase : RoomDatabase() {
    abstract fun customGameDao(): CustomGameDao
    abstract fun gestureMappingDao(): GestureMappingDao
    abstract fun savedDrawingDao(): SavedDrawingDao
    abstract fun pairedDeviceDao(): PairedDeviceDao
    abstract fun customObject3DDao(): CustomObject3DDao

    companion object {
        @Volatile
        private var INSTANCE: MotionXDatabase? = null

        fun getInstance(context: Context): MotionXDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    MotionXDatabase::class.java,
                    "rudrax_motion_x.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
