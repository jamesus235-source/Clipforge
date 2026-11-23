package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.model.BrandPreset
import com.example.domain.model.ClipProject
import com.example.ui.components.StudioSectionCard
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.ObsidianCanvas
import com.example.ui.theme.RoseError
import com.example.ui.theme.SubtleBorder
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.MainViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    projects: List<ClipProject>,
    brandPresets: List<BrandPreset>,
    onOpenProject: (ClipProject) -> Unit,
    onRenameProject: (String, String) -> Unit,
    onDeleteProject: (ClipProject) -> Unit,
    onSaveClipToGallery: (String) -> Unit,
    onDeleteBrandPreset: (String) -> Unit,
    onCreateNewProject: () -> Unit,
    modifier: Modifier = Modifier
) {
    var projectToRename by remember { mutableStateOf<ClipProject?>(null) }
    var renameText by remember { mutableStateOf("") }
    var projectToDelete by remember { mutableStateOf<ClipProject?>(null) }

    val dateFormatter = remember { SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier.widthIn(max = 760.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Projects Dashboard", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
                    Text(
                        "${projects.size} saved projects • ${brandPresets.size} brand presets",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                Button(
                    onClick = onCreateNewProject,
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("dashboard_new_project_button")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "New project", modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("New")
                }
            }

            if (projects.isEmpty()) {
                StudioSectionCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.FolderOpen,
                            contentDescription = "No saved projects",
                            tint = ElectricViolet,
                            modifier = Modifier.size(44.dp)
                        )
                        Text("No Projects Yet", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        Text(
                            "Paste a YouTube link or import a video on the Create tab to generate and save your first clips.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                        Button(
                            onClick = onCreateNewProject,
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricViolet)
                        ) {
                            Text("Create First Project")
                        }
                    }
                }
            } else {
                Text("Recent Projects", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                projects.forEachIndexed { idx, project ->
                    val exportedClips = project.clips.filter {
                        val path = it.exportedFilePath
                        path != null && File(path).exists()
                    }
                    StudioSectionCard {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(ElectricViolet.copy(alpha = 0.16f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Filled.Movie, contentDescription = "Project", tint = ElectricViolet)
                                    }
                                    Column {
                                        Text(
                                            text = project.name,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = TextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${project.clips.size} clips • ${exportedClips.size} exported • ${dateFormatter.format(Date(project.updatedAtMs))}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary
                                        )
                                    }
                                }

                                Row {
                                    IconButton(
                                        onClick = {
                                            projectToRename = project
                                            renameText = project.name
                                        },
                                        modifier = Modifier.testTag("rename_project_button_$idx")
                                    ) {
                                        Icon(Icons.Filled.Edit, contentDescription = "Rename project", tint = TextSecondary)
                                    }
                                    IconButton(
                                        onClick = { projectToDelete = project },
                                        modifier = Modifier.testTag("delete_project_button_$idx")
                                    ) {
                                        Icon(Icons.Filled.Delete, contentDescription = "Delete project", tint = RoseError)
                                    }
                                }
                            }

                            // Quick exported clip downloads inside dashboard card
                            if (exportedClips.isNotEmpty()) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    exportedClips.forEach { exp ->
                                        val file = File(exp.exportedFilePath!!)
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = ObsidianCanvas,
                                            border = BorderStroke(1.dp, SubtleBorder)
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "${exp.title} (${MainViewModel.formatBytes(file.length())})",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = EmeraldSuccess,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                IconButton(onClick = { onSaveClipToGallery(file.absolutePath) }) {
                                                    Icon(Icons.Filled.Download, contentDescription = "Download clip to gallery", tint = ElectricBlue, modifier = Modifier.size(18.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            OutlinedButton(
                                onClick = { onOpenProject(project) },
                                border = BorderStroke(1.dp, ElectricViolet.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .testTag("open_project_button_$idx")
                            ) {
                                Text("Continue Editing in Studio", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            // Saved Brand Presets
            Text("Saved Brand Presets", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            if (brandPresets.isEmpty()) {
                StudioSectionCard {
                    Text(
                        text = "No saved brand presets yet. Customize captions and branding in the Studio Editor and tap \"Save Brand\" to reuse across projects.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                brandPresets.forEach { preset ->
                    StudioSectionCard {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.Bookmark, contentDescription = "Brand preset", tint = ElectricBlue)
                                Column {
                                    Text(preset.name, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                                    Text(
                                        text = "${preset.aspectRatio.label} • ${preset.subtitleStyle.preset.displayName} • ${preset.watermarkText.ifBlank { "No watermark" }}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                            }
                            IconButton(onClick = { onDeleteBrandPreset(preset.id) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete brand preset", tint = RoseError)
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }

    projectToRename?.let { proj ->
        AlertDialog(
            onDismissRequest = { projectToRename = null },
            title = { Text("Rename Project") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    label = { Text("Project Name") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    onRenameProject(proj.id, renameText)
                    projectToRename = null
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { projectToRename = null }) { Text("Cancel") }
            }
        )
    }

    projectToDelete?.let { proj ->
        AlertDialog(
            onDismissRequest = { projectToDelete = null },
            title = { Text("Delete Project?") },
            text = { Text("Delete \"${proj.name}\" and its exported clips from local storage?") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteProject(proj)
                        projectToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoseError)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { projectToDelete = null }) { Text("Cancel") }
            }
        )
    }
}
