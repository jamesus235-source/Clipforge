package com.example.data.local

import com.example.domain.model.BrandPreset
import com.example.domain.model.ClipProject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

interface ProjectRepository {
    fun observeProjects(): Flow<List<ClipProject>>
    suspend fun getProject(id: String): ClipProject?
    suspend fun saveProject(project: ClipProject)
    suspend fun renameProject(id: String, newName: String)
    suspend fun deleteProject(id: String)

    fun observeBrandPresets(): Flow<List<BrandPreset>>
    suspend fun saveBrandPreset(preset: BrandPreset)
    suspend fun deleteBrandPreset(id: String)
}

class RoomProjectRepository(
    private val dao: ClipForgeDao
) : ProjectRepository {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    override fun observeProjects(): Flow<List<ClipProject>> =
        dao.observeAllProjects().map { list ->
            list.mapNotNull { entity ->
                runCatching { json.decodeFromString<ClipProject>(entity.projectJson) }.getOrNull()
            }
        }

    override suspend fun getProject(id: String): ClipProject? {
        val entity = dao.getProjectById(id) ?: return null
        return runCatching { json.decodeFromString<ClipProject>(entity.projectJson) }.getOrNull()
    }

    override suspend fun saveProject(project: ClipProject) {
        val updated = project.copy(updatedAtMs = System.currentTimeMillis())
        dao.upsertProject(
            ProjectEntity(
                id = updated.id,
                name = updated.name,
                updatedAtMs = updated.updatedAtMs,
                projectJson = json.encodeToString(updated)
            )
        )
    }

    override suspend fun renameProject(id: String, newName: String) {
        val existing = getProject(id) ?: return
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) return
        saveProject(existing.copy(name = trimmed))
    }

    override suspend fun deleteProject(id: String) {
        dao.deleteProjectById(id)
    }

    override fun observeBrandPresets(): Flow<List<BrandPreset>> =
        dao.observeAllBrandPresets().map { list ->
            list.mapNotNull { entity ->
                runCatching { json.decodeFromString<BrandPreset>(entity.presetJson) }.getOrNull()
            }
        }

    override suspend fun saveBrandPreset(preset: BrandPreset) {
        dao.upsertBrandPreset(
            BrandPresetEntity(
                id = preset.id,
                name = preset.name,
                createdAtMs = preset.createdAtMs,
                presetJson = json.encodeToString(preset)
            )
        )
    }

    override suspend fun deleteBrandPreset(id: String) {
        dao.deleteBrandPresetById(id)
    }
}
