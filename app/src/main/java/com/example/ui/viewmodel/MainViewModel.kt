package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.annotation.OptIn
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import com.example.data.ai.AiClipRepository
import com.example.data.ai.ClipGenerationRequest
import com.example.data.ai.DemoAiClipRepository
import com.example.data.local.ClipForgeDatabase
import com.example.data.local.ProjectRepository
import com.example.data.local.RoomProjectRepository
import com.example.data.network.YouTubeMetadataService
import com.example.data.network.YouTubeParseResult
import com.example.data.network.YouTubeUrlParser
import com.example.domain.model.BrandPreset
import com.example.domain.model.CaptionPreset
import com.example.domain.model.ClipProject
import com.example.domain.model.ExportConfig
import com.example.domain.model.GeneratedClip
import com.example.domain.model.SilenceSpan
import com.example.domain.model.SourceType
import com.example.domain.model.SubtitleSegment
import com.example.domain.model.SubtitleStyle
import com.example.domain.model.VideoFramingConfig
import com.example.domain.model.YouTubeMetadata
import com.example.media.SampleVideoGenerator
import com.example.media.VideoAnalyzer
import com.example.media.VideoExportEngine
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

sealed interface UiAsyncState {
    data object Idle : UiAsyncState
    data class Loading(val message: String) : UiAsyncState
    data class Error(val message: String) : UiAsyncState
}

data class CreateUiState(
    val youtubeUrlInput: String = "",
    val youtubeValidationError: String? = null,
    val isFetchingYouTube: Boolean = false,
    val youtubeMetadata: YouTubeMetadata? = null,
    val selectedVideoPath: String? = null,
    val selectedVideoName: String = "",
    val selectedVideoDurationMs: Long = 0L,
    val selectedVideoWidth: Int = 1280,
    val selectedVideoHeight: Int = 720,
    val sourceType: SourceType = SourceType.SAMPLE_STUDIO,
    val detectedSilences: List<SilenceSpan> = emptyList(),
    val clipCount: Int = 4,
    val preferredDurationSec: Int = 30,
    val useManualRange: Boolean = false,
    val manualStartFraction: Float = 0.0f,
    val manualEndFraction: Float = 0.45f,
    val generationState: UiAsyncState = UiAsyncState.Idle
)

data class ExportUiState(
    val config: ExportConfig = ExportConfig(),
    val isExporting: Boolean = false,
    val activeClipTitle: String = "",
    val currentClipIndex: Int = 0,
    val totalClipsToExport: Int = 0,
    val progress: Float = 0f,
    val zipFilePath: String? = null,
    val zipFileSizeBytes: Long = 0L,
    val statusMessage: String? = null,
    val errorMessage: String? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val database = ClipForgeDatabase.getInstance(application)
    private val projectRepository: ProjectRepository = RoomProjectRepository(database.clipForgeDao())
    private val youTubeService = YouTubeMetadataService()
    private val aiRepository: AiClipRepository = DemoAiClipRepository()

    val aiStatusNotice: String = aiRepository.statusNotice
    val isLiveAiEnabled: Boolean = aiRepository.isLiveAiEnabled

    val projects: StateFlow<List<ClipProject>> = projectRepository.observeProjects()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val brandPresets: StateFlow<List<BrandPreset>> = projectRepository.observeBrandPresets()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _createState = MutableStateFlow(CreateUiState())
    val createState: StateFlow<CreateUiState> = _createState.asStateFlow()

    private val _activeProject = MutableStateFlow<ClipProject?>(null)
    val activeProject: StateFlow<ClipProject?> = _activeProject.asStateFlow()

    private val _selectedClipId = MutableStateFlow<String?>(null)
    val selectedClipId: StateFlow<String?> = _selectedClipId.asStateFlow()

    val selectedClip: StateFlow<GeneratedClip?> = combine(_activeProject, _selectedClipId) { proj, cid ->
        proj?.clips?.firstOrNull { it.id == cid } ?: proj?.clips?.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _exportState = MutableStateFlow(ExportUiState())
    val exportState: StateFlow<ExportUiState> = _exportState.asStateFlow()

    private val _aiBusyMessage = MutableStateFlow<String?>(null)
    val aiBusyMessage: StateFlow<String?> = _aiBusyMessage.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private var exportJob: Job? = null

    init {
        // Prepare sample studio video quietly in background so sample preview is instant
        viewModelScope.launch {
            runCatching {
                val sampleFile = SampleVideoGenerator.ensureSampleVideo(getApplication())
                val info = VideoAnalyzer.inspectVideoFile(sampleFile, "ClipForge Studio Sample.mp4")
                val silences = VideoAnalyzer.detectSilences(sampleFile.absolutePath)
                if (_createState.value.selectedVideoPath == null) {
                    _createState.update {
                        it.copy(
                            selectedVideoPath = sampleFile.absolutePath,
                            selectedVideoName = info.displayName,
                            selectedVideoDurationMs = info.durationMs,
                            selectedVideoWidth = info.width,
                            selectedVideoHeight = info.height,
                            sourceType = SourceType.SAMPLE_STUDIO,
                            detectedSilences = silences
                        )
                    }
                }
            }
        }
    }

    fun consumeSnackbar() {
        _snackbarMessage.value = null
    }

    fun onYouTubeUrlChanged(input: String) {
        val error = if (input.isBlank()) {
            null
        } else {
            when (val res = YouTubeUrlParser.parse(input)) {
                is YouTubeParseResult.Valid -> null
                is YouTubeParseResult.Invalid -> res.errorMessage
            }
        }
        _createState.update {
            it.copy(youtubeUrlInput = input, youtubeValidationError = error)
        }
    }

    fun fetchYouTubeMetadata() {
        val url = _createState.value.youtubeUrlInput.trim()
        when (val parsed = YouTubeUrlParser.parse(url)) {
            is YouTubeParseResult.Invalid -> {
                _createState.update { it.copy(youtubeValidationError = parsed.errorMessage) }
                return
            }
            is YouTubeParseResult.Valid -> {
                viewModelScope.launch {
                    _createState.update {
                        it.copy(isFetchingYouTube = true, youtubeValidationError = null)
                    }
                    val result = youTubeService.fetchMetadata(parsed.canonicalUrl)
                    result.fold(
                        onSuccess = { meta ->
                            _createState.update {
                                it.copy(
                                    isFetchingYouTube = false,
                                    youtubeMetadata = meta,
                                    youtubeValidationError = null
                                )
                            }
                        },
                        onFailure = { err ->
                            _createState.update {
                                it.copy(
                                    isFetchingYouTube = false,
                                    youtubeValidationError = err.localizedMessage ?: "Metadata lookup failed."
                                )
                            }
                        }
                    )
                }
            }
        }
    }

    fun importLocalVideoUri(uri: Uri) {
        viewModelScope.launch {
            _createState.update {
                it.copy(generationState = UiAsyncState.Loading("Importing and scanning video audio..."))
            }
            val result = VideoAnalyzer.importPickedVideo(getApplication(), uri)
            result.fold(
                onSuccess = { info ->
                    val silences = VideoAnalyzer.detectSilences(info.localFile.absolutePath)
                    _createState.update {
                        it.copy(
                            selectedVideoPath = info.localFile.absolutePath,
                            selectedVideoName = info.displayName,
                            selectedVideoDurationMs = info.durationMs,
                            selectedVideoWidth = info.width,
                            selectedVideoHeight = info.height,
                            sourceType = SourceType.LOCAL_FILE,
                            detectedSilences = silences,
                            generationState = UiAsyncState.Idle
                        )
                    }
                    _snackbarMessage.value = "Loaded ${info.displayName} (${silences.size} pauses detected)"
                },
                onFailure = { err ->
                    _createState.update {
                        it.copy(
                            generationState = UiAsyncState.Error(
                                err.localizedMessage ?: "Could not import video."
                            )
                        )
                    }
                }
            )
        }
    }

    fun loadSampleStudioVideo() {
        viewModelScope.launch {
            _createState.update {
                it.copy(generationState = UiAsyncState.Loading("Preparing built-in studio sample video..."))
            }
            runCatching {
                val sampleFile = SampleVideoGenerator.ensureSampleVideo(getApplication())
                val info = VideoAnalyzer.inspectVideoFile(sampleFile, "ClipForge Studio Sample.mp4")
                val silences = VideoAnalyzer.detectSilences(sampleFile.absolutePath)
                _createState.update {
                    it.copy(
                        selectedVideoPath = sampleFile.absolutePath,
                        selectedVideoName = info.displayName,
                        selectedVideoDurationMs = info.durationMs,
                        selectedVideoWidth = info.width,
                        selectedVideoHeight = info.height,
                        sourceType = SourceType.SAMPLE_STUDIO,
                        detectedSilences = silences,
                        generationState = UiAsyncState.Idle
                    )
                }
                _snackbarMessage.value = "Studio sample video ready (${silences.size} pauses detected)"
            }.onFailure { err ->
                _createState.update {
                    it.copy(
                        generationState = UiAsyncState.Error(err.localizedMessage ?: "Failed to prepare sample video.")
                    )
                }
            }
        }
    }

    fun updateClipCount(count: Int) {
        _createState.update { it.copy(clipCount = count.coerceIn(1, 10)) }
    }

    fun updatePreferredDuration(seconds: Int) {
        _createState.update { it.copy(preferredDurationSec = seconds.coerceIn(15, 60)) }
    }

    fun toggleManualRange(enabled: Boolean) {
        _createState.update { it.copy(useManualRange = enabled) }
    }

    fun updateManualRange(startFrac: Float, endFrac: Float) {
        val s = startFrac.coerceIn(0f, 0.90f)
        val e = endFrac.coerceIn(s + 0.08f, 1.0f)
        _createState.update { it.copy(manualStartFraction = s, manualEndFraction = e) }
    }

    fun generateClips(onNavigateToEditor: () -> Unit) {
        val state = _createState.value
        val videoPath = state.selectedVideoPath
        if (videoPath == null || !File(videoPath).exists()) {
            _createState.update {
                it.copy(
                    generationState = UiAsyncState.Error(
                        "Select a local video file or load the Studio Sample Video before generating clips."
                    )
                )
            }
            return
        }

        viewModelScope.launch {
            _createState.update {
                it.copy(generationState = UiAsyncState.Loading("Analyzing audio cadence & generating clips..."))
            }
            try {
                val duration = state.selectedVideoDurationMs.coerceAtLeast(10_000L)
                val manualStart = if (state.useManualRange) (state.manualStartFraction * duration).toLong() else null
                val manualEnd = if (state.useManualRange) (state.manualEndFraction * duration).toLong() else null

                val sourceTitle = state.youtubeMetadata?.title
                    ?: state.selectedVideoName.substringBeforeLast(".")
                    .ifBlank { "Untitled Studio Project" }

                val generatedClips = aiRepository.generateClips(
                    ClipGenerationRequest(
                        sourceTitle = sourceTitle,
                        totalDurationMs = duration,
                        clipCount = state.clipCount,
                        preferredDurationSec = state.preferredDurationSec,
                        manualStartMs = manualStart,
                        manualEndMs = manualEnd,
                        silences = state.detectedSilences,
                        youtubeMetadata = state.youtubeMetadata
                    )
                )

                val project = ClipProject(
                    name = sourceTitle,
                    sourceType = state.sourceType,
                    sourceVideoPath = videoPath,
                    sourceDisplayName = state.selectedVideoName,
                    sourceDurationMs = duration,
                    sourceWidth = state.selectedVideoWidth,
                    sourceHeight = state.selectedVideoHeight,
                    youtubeMetadata = state.youtubeMetadata,
                    clips = generatedClips,
                    detectedSilences = state.detectedSilences,
                    isSampleAiOutput = !aiRepository.isLiveAiEnabled
                )

                projectRepository.saveProject(project)
                _activeProject.value = project
                _selectedClipId.value = generatedClips.firstOrNull()?.id
                _exportState.value = ExportUiState()
                _createState.update { it.copy(generationState = UiAsyncState.Idle) }
                onNavigateToEditor()
            } catch (e: Exception) {
                _createState.update {
                    it.copy(
                        generationState = UiAsyncState.Error(
                            e.localizedMessage ?: "Clip generation failed."
                        )
                    )
                }
            }
        }
    }

    fun selectClip(clipId: String) {
        _selectedClipId.value = clipId
    }

    fun addManualClip() {
        val project = _activeProject.value ?: return
        val total = project.sourceDurationMs.coerceAtLeast(10_000L)
        val newStart = (total * 0.1f).toLong()
        val newEnd = (newStart + minOf(20_000L, total / 2)).coerceAtMost(total)
        val newClip = GeneratedClip(
            title = "Manual Clip ${project.clips.size + 1}",
            description = "Custom user-defined clip range.",
            startMs = newStart,
            endMs = newEnd,
            scoreEstimate = 80,
            scoreExplanation = "Custom user range.",
            selectionReason = "Added manually in Studio Editor.",
            suggestedPreset = CaptionPreset.CLEAN_MINIMAL,
            transcriptSegments = listOf(
                SubtitleSegment(
                    startMs = 0L,
                    endMs = newEnd - newStart,
                    text = "Tap subtitle text to customize your caption",
                    highlightedWords = listOf("customize")
                )
            ),
            hooks = listOf("Custom opening hook"),
            titleSuggestions = listOf("Manual Clip ${project.clips.size + 1}"),
            socialCaption = "Edited with ClipForge AI.",
            hashtags = listOf("#ClipForgeAI", "#Shorts"),
            isAiDemoPlaceholder = false
        )
        val updated = project.copy(clips = project.clips + newClip)
        persistActiveProject(updated)
        _selectedClipId.value = newClip.id
    }

    fun updateSelectedClip(transform: (GeneratedClip) -> GeneratedClip) {
        val project = _activeProject.value ?: return
        val targetId = selectedClip.value?.id ?: return
        val updatedClips = project.clips.map { if (it.id == targetId) transform(it) else it }
        persistActiveProject(project.copy(clips = updatedClips))
    }

    fun applySubtitlePreset(preset: CaptionPreset) {
        updateSelectedClip { clip ->
            clip.copy(
                suggestedPreset = preset,
                subtitleStyle = SubtitleStyle.fromPreset(preset)
            )
        }
    }

    fun updateSubtitleStyle(transform: (SubtitleStyle) -> SubtitleStyle) {
        updateSelectedClip { clip ->
            clip.copy(subtitleStyle = transform(clip.subtitleStyle))
        }
    }

    fun updateFraming(transform: (VideoFramingConfig) -> VideoFramingConfig) {
        updateSelectedClip { clip ->
            clip.copy(framing = transform(clip.framing))
        }
    }

    fun updateSubtitleSegmentText(segmentId: String, newText: String) {
        updateSelectedClip { clip ->
            clip.copy(
                transcriptSegments = clip.transcriptSegments.map { seg ->
                    if (seg.id == segmentId) seg.copy(text = newText) else seg
                }
            )
        }
    }

    fun updateSubtitleSegmentTiming(segmentId: String, newStartMs: Long, newEndMs: Long) {
        updateSelectedClip { clip ->
            val maxDur = clip.durationMs
            val s = newStartMs.coerceIn(0L, (maxDur - 500L).coerceAtLeast(0L))
            val e = newEndMs.coerceIn(s + 400L, maxDur)
            clip.copy(
                transcriptSegments = clip.transcriptSegments.map { seg ->
                    if (seg.id == segmentId) seg.copy(startMs = s, endMs = e) else seg
                }
            )
        }
    }

    fun addSubtitleSegment(text: String) {
        if (text.isBlank()) return
        updateSelectedClip { clip ->
            val lastEnd = clip.transcriptSegments.maxOfOrNull { it.endMs } ?: 0L
            val segStart = lastEnd.coerceAtMost((clip.durationMs - 1000L).coerceAtLeast(0L))
            val segEnd = (segStart + 2500L).coerceAtMost(clip.durationMs)
            val newSeg = SubtitleSegment(
                startMs = segStart,
                endMs = segEnd,
                text = text.trim()
            )
            clip.copy(transcriptSegments = clip.transcriptSegments + newSeg)
        }
    }

    fun pickBackgroundMusicUri(uri: Uri) {
        var name = "Custom Track.mp3"
        getApplication<Application>().contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && cursor.moveToFirst()) {
                name = cursor.getString(idx) ?: name
            }
        }
        updateFraming { it.copy(backgroundMusicUri = uri.toString(), backgroundMusicName = name) }
    }

    fun regenerateSelectedClip() {
        val project = _activeProject.value ?: return
        val current = selectedClip.value ?: return
        viewModelScope.launch {
            _aiBusyMessage.value = "Regenerating clip moment..."
            val regenerated = aiRepository.regenerateClip(
                existingClip = current,
                totalDurationMs = project.sourceDurationMs,
                existingClips = project.clips
            )
            val updated = project.copy(
                clips = project.clips.map { if (it.id == current.id) regenerated else it }
            )
            persistActiveProject(updated)
            _aiBusyMessage.value = null
        }
    }

    fun refreshAiHooks() {
        val current = selectedClip.value ?: return
        viewModelScope.launch {
            _aiBusyMessage.value = "Generating opening hooks..."
            val hooks = aiRepository.generateHooks(current)
            updateSelectedClip { it.copy(hooks = hooks) }
            _aiBusyMessage.value = null
        }
    }

    fun refreshAiTitles() {
        val current = selectedClip.value ?: return
        viewModelScope.launch {
            _aiBusyMessage.value = "Generating title ideas..."
            val titles = aiRepository.generateTitles(current)
            updateSelectedClip { it.copy(titleSuggestions = titles) }
            _aiBusyMessage.value = null
        }
    }

    fun refreshAiSocialCaption() {
        val current = selectedClip.value ?: return
        viewModelScope.launch {
            _aiBusyMessage.value = "Generating social caption & hashtags..."
            val (caption, tags) = aiRepository.generateSocialCaption(current)
            updateSelectedClip { it.copy(socialCaption = caption, hashtags = tags) }
            _aiBusyMessage.value = null
        }
    }

    fun saveCurrentStyleAsBrandPreset(name: String) {
        val clip = selectedClip.value ?: return
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            val preset = BrandPreset(
                name = trimmed,
                subtitleStyle = clip.subtitleStyle,
                watermarkText = clip.framing.watermarkText,
                watermarkImageUri = clip.framing.watermarkImageUri,
                aspectRatio = clip.framing.aspectRatio,
                backgroundMode = clip.framing.backgroundMode
            )
            projectRepository.saveBrandPreset(preset)
            _snackbarMessage.value = "Saved brand preset \"$trimmed\""
        }
    }

    fun applyBrandPreset(preset: BrandPreset) {
        updateSelectedClip { clip ->
            clip.copy(
                subtitleStyle = preset.subtitleStyle,
                framing = clip.framing.copy(
                    watermarkText = preset.watermarkText,
                    watermarkImageUri = preset.watermarkImageUri,
                    aspectRatio = preset.aspectRatio,
                    backgroundMode = preset.backgroundMode
                )
            )
        }
        _snackbarMessage.value = "Applied brand preset \"${preset.name}\""
    }

    fun deleteBrandPreset(id: String) {
        viewModelScope.launch {
            projectRepository.deleteBrandPreset(id)
        }
    }

    fun updateExportConfig(transform: (ExportConfig) -> ExportConfig) {
        _exportState.update { it.copy(config = transform(it.config)) }
    }

    @OptIn(UnstableApi::class)
    fun exportSingleClip(clipId: String) {
        val project = _activeProject.value ?: return
        val sourcePath = project.sourceVideoPath ?: return
        val clip = project.clips.firstOrNull { it.id == clipId } ?: return

        exportJob?.cancel()
        exportJob = viewModelScope.launch {
            _exportState.update {
                it.copy(
                    isExporting = true,
                    activeClipTitle = clip.title,
                    currentClipIndex = 1,
                    totalClipsToExport = 1,
                    progress = 0f,
                    errorMessage = null,
                    statusMessage = "Rendering MP4 with Media3 Transformer..."
                )
            }
            val res = VideoExportEngine.exportClip(
                context = getApplication(),
                sourcePath = sourcePath,
                clip = clip,
                detectedSilences = project.detectedSilences,
                config = _exportState.value.config,
                onProgress = { p ->
                    _exportState.update { it.copy(progress = p) }
                }
            )
            res.fold(
                onSuccess = { exported ->
                    val updatedProject = project.copy(
                        clips = project.clips.map { c ->
                            if (c.id == exported.clipId) {
                                c.copy(
                                    exportedFilePath = exported.outputFile.absolutePath,
                                    exportedFileSizeBytes = exported.fileSizeBytes,
                                    exportedResolution = exported.resolutionLabel,
                                    exportedFps = exported.fps,
                                    exportedWithSubtitles = exported.hasBurnedSubtitles
                                )
                            } else c
                        }
                    )
                    persistActiveProject(updatedProject)
                    _exportState.update {
                        it.copy(
                            isExporting = false,
                            progress = 1f,
                            statusMessage = "Rendered ${exported.outputFile.name} (${formatBytes(exported.fileSizeBytes)})"
                        )
                    }
                },
                onFailure = { err ->
                    _exportState.update {
                        it.copy(
                            isExporting = false,
                            errorMessage = err.localizedMessage ?: "Export failed.",
                            statusMessage = null
                        )
                    }
                }
            )
        }
    }

    @OptIn(UnstableApi::class)
    fun exportAllClipsAndZip() {
        val project = _activeProject.value ?: return
        val sourcePath = project.sourceVideoPath ?: return
        if (project.clips.isEmpty()) return

        exportJob?.cancel()
        exportJob = viewModelScope.launch {
            val total = project.clips.size
            var currentProj = project
            val exportedFiles = mutableListOf<File>()

            _exportState.update {
                it.copy(
                    isExporting = true,
                    totalClipsToExport = total,
                    currentClipIndex = 0,
                    progress = 0f,
                    zipFilePath = null,
                    zipFileSizeBytes = 0L,
                    errorMessage = null,
                    statusMessage = "Starting batch MP4 export..."
                )
            }

            for ((idx, clip) in project.clips.withIndex()) {
                _exportState.update {
                    it.copy(
                        currentClipIndex = idx + 1,
                        activeClipTitle = clip.title,
                        statusMessage = "Rendering clip ${idx + 1} of $total: ${clip.title}"
                    )
                }
                val res = VideoExportEngine.exportClip(
                    context = getApplication(),
                    sourcePath = sourcePath,
                    clip = clip,
                    detectedSilences = project.detectedSilences,
                    config = _exportState.value.config,
                    onProgress = { clipProg ->
                        val overall = (idx + clipProg) / total.toFloat()
                        _exportState.update { it.copy(progress = overall) }
                    }
                )
                val exported = res.getOrElse { err ->
                    _exportState.update {
                        it.copy(
                            isExporting = false,
                            errorMessage = "Clip ${idx + 1} failed: ${err.localizedMessage ?: "error"}",
                            statusMessage = null
                        )
                    }
                    return@launch
                }
                exportedFiles.add(exported.outputFile)
                currentProj = currentProj.copy(
                    clips = currentProj.clips.map { c ->
                        if (c.id == exported.clipId) {
                            c.copy(
                                exportedFilePath = exported.outputFile.absolutePath,
                                exportedFileSizeBytes = exported.fileSizeBytes,
                                exportedResolution = exported.resolutionLabel,
                                exportedFps = exported.fps,
                                exportedWithSubtitles = exported.hasBurnedSubtitles
                            )
                        } else c
                    }
                )
                persistActiveProject(currentProj)
            }

            _exportState.update { it.copy(statusMessage = "Creating ZIP archive...") }
            val zipRes = VideoExportEngine.createZipBundle(
                context = getApplication(),
                files = exportedFiles,
                zipName = currentProj.name
            )
            zipRes.fold(
                onSuccess = { zipFile ->
                    _exportState.update {
                        it.copy(
                            isExporting = false,
                            progress = 1f,
                            zipFilePath = zipFile.absolutePath,
                            zipFileSizeBytes = zipFile.length(),
                            statusMessage = "Exported $total MP4 clips + ZIP (${formatBytes(zipFile.length())})"
                        )
                    }
                },
                onFailure = { err ->
                    _exportState.update {
                        it.copy(
                            isExporting = false,
                            errorMessage = "ZIP failed: ${err.localizedMessage ?: "error"}"
                        )
                    }
                }
            )
        }
    }

    fun cancelExport() {
        exportJob?.cancel()
        _exportState.update {
            it.copy(isExporting = false, statusMessage = "Export cancelled.")
        }
    }

    fun saveExportedClipToGallery(filePath: String) {
        viewModelScope.launch {
            val res = VideoExportEngine.saveVideoToGallery(getApplication(), File(filePath))
            _snackbarMessage.value = res.fold(
                onSuccess = { "Saved MP4 to Movies/ClipForgeAI" },
                onFailure = { "Save failed: ${it.localizedMessage}" }
            )
        }
    }

    fun openProject(project: ClipProject, onNavigateToEditor: () -> Unit) {
        _activeProject.value = project
        _selectedClipId.value = project.clips.firstOrNull()?.id
        _exportState.value = ExportUiState()
        onNavigateToEditor()
    }

    fun renameProject(projectId: String, newName: String) {
        viewModelScope.launch {
            projectRepository.renameProject(projectId, newName)
            if (_activeProject.value?.id == projectId) {
                _activeProject.update { it?.copy(name = newName.trim()) }
            }
        }
    }

    fun deleteProject(project: ClipProject) {
        viewModelScope.launch {
            project.clips.forEach { clip ->
                clip.exportedFilePath?.let { path ->
                    runCatching { File(path).delete() }
                }
            }
            projectRepository.deleteProject(project.id)
            if (_activeProject.value?.id == project.id) {
                _activeProject.value = null
                _selectedClipId.value = null
            }
            _snackbarMessage.value = "Deleted \"${project.name}\""
        }
    }

    private fun persistActiveProject(updated: ClipProject) {
        _activeProject.value = updated
        viewModelScope.launch {
            projectRepository.saveProject(updated)
        }
    }

    companion object {
        fun formatBytes(bytes: Long): String {
            return when {
                bytes <= 0L -> "0 KB"
                bytes < 1024L * 1024L -> "${(bytes / 1024.0).toInt().coerceAtLeast(1)} KB"
                else -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
            }
        }

        fun formatTimestamp(ms: Long): String {
            val totalSec = (ms / 1000L).coerceAtLeast(0L)
            val m = totalSec / 60L
            val s = totalSec % 60L
            return "%02d:%02d".format(m, s)
        }
    }
}
