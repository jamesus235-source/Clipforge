package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val id: String,
    val name: String,
    val updatedAtMs: Long,
    val projectJson: String
)

@Entity(tableName = "brand_presets")
data class BrandPresetEntity(
    @PrimaryKey val id: String,
    val name: String,
    val createdAtMs: Long,
    val presetJson: String
)

@Dao
interface ClipForgeDao {
    @Query("SELECT * FROM projects ORDER BY updatedAtMs DESC")
    fun observeAllProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :id LIMIT 1")
    suspend fun getProjectById(id: String): ProjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProject(entity: ProjectEntity)

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteProjectById(id: String)

    @Query("SELECT * FROM brand_presets ORDER BY createdAtMs DESC")
    fun observeAllBrandPresets(): Flow<List<BrandPresetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBrandPreset(entity: BrandPresetEntity)

    @Query("DELETE FROM brand_presets WHERE id = :id")
    suspend fun deleteBrandPresetById(id: String)
}

@Database(
    entities = [ProjectEntity::class, BrandPresetEntity::class],
    version = 1,
    exportSchema = false
)
abstract class ClipForgeDatabase : RoomDatabase() {
    abstract fun clipForgeDao(): ClipForgeDao

    companion object {
        @Volatile
        private var INSTANCE: ClipForgeDatabase? = null

        fun getInstance(context: Context): ClipForgeDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    ClipForgeDatabase::class.java,
                    "clipforge_studio.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
