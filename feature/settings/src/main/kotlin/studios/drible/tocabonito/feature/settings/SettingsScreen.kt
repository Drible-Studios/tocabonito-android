package studios.drible.tocabonito.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import studios.drible.tocabonito.core.domain.service.SyncStatus
import studios.drible.tocabonito.core.ui.theme.LocalThemePalette

@Composable
fun SettingsScreen(
    onNavigateToThemePicker: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val palette = LocalThemePalette.current
    val currentTheme by viewModel.themeProvider.selectedTheme.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsStateWithLifecycle()
    val dataPortabilityState by viewModel.dataPortabilityState.collectAsStateWithLifecycle()

    // Show export result dialog
    if (dataPortabilityState is DataPortabilityState.ExportReady) {
        val backup = (dataPortabilityState as DataPortabilityState.ExportReady).backup
        AlertDialog(
            onDismissRequest = { viewModel.clearPortabilityState() },
            title = { Text("Export Ready", color = palette.textPrimary) },
            text = {
                Text(
                    text = "Exported ${backup.favorites.size} favorites and ${backup.watchProgress.size} progress records.",
                    color = palette.textSecondary,
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.clearPortabilityState() }) {
                    Text("OK", color = palette.accent)
                }
            },
        )
    }

    // Show import result dialog
    if (dataPortabilityState is DataPortabilityState.ImportDone) {
        val result = (dataPortabilityState as DataPortabilityState.ImportDone).result
        AlertDialog(
            onDismissRequest = { viewModel.clearPortabilityState() },
            title = { Text("Import Complete", color = palette.textPrimary) },
            text = {
                Text(
                    text = "Imported ${result.favoritesImported} favorites and ${result.progressRecordsImported} progress records. Skipped ${result.skippedDuplicates} duplicates.",
                    color = palette.textSecondary,
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.clearPortabilityState() }) {
                    Text("OK", color = palette.accent)
                }
            },
        )
    }

    // Show error dialog
    if (dataPortabilityState is DataPortabilityState.Error) {
        val message = (dataPortabilityState as DataPortabilityState.Error).message
        AlertDialog(
            onDismissRequest = { viewModel.clearPortabilityState() },
            title = { Text("Error", color = palette.textPrimary) },
            text = { Text(text = message, color = palette.textSecondary) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearPortabilityState() }) {
                    Text("OK", color = palette.accent)
                }
            },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium,
            color = palette.textPrimary,
            modifier = Modifier.padding(bottom = 24.dp),
        )

        // — Appearance section —
        SettingsSectionHeader(title = "Appearance", palette = palette)
        SettingsRow(
            icon = Icons.Default.Palette,
            label = "Theme",
            subtitle = currentTheme.displayName,
            palette = palette,
            onClick = onNavigateToThemePicker,
        )

        Spacer(Modifier.height(16.dp))
        HorizontalDivider(color = palette.textTertiary.copy(alpha = 0.2f))
        Spacer(Modifier.height(16.dp))

        // — Account section —
        SettingsSectionHeader(title = "Account", palette = palette)
        SettingsRow(
            icon = Icons.Default.CloudOff,
            label = "Cloud Sync",
            subtitle = syncStatus.label(),
            palette = palette,
            onClick = null,
        )

        Spacer(Modifier.height(16.dp))
        HorizontalDivider(color = palette.textTertiary.copy(alpha = 0.2f))
        Spacer(Modifier.height(16.dp))

        // — Data section —
        SettingsSectionHeader(title = "Data", palette = palette)
        SettingsRow(
            icon = Icons.Default.Upload,
            label = "Export Data",
            subtitle = "Save a local backup of your favorites and progress",
            palette = palette,
            onClick = { viewModel.exportData() },
        )
        SettingsRow(
            icon = Icons.Default.Download,
            label = "Import Data",
            subtitle = "Restore from a local backup",
            palette = palette,
            onClick = { /* In a real implementation this opens a file picker */ },
        )

        Spacer(Modifier.height(16.dp))
        HorizontalDivider(color = palette.textTertiary.copy(alpha = 0.2f))
        Spacer(Modifier.height(16.dp))

        // — About section —
        SettingsSectionHeader(title = "About", palette = palette)
        SettingsRow(
            icon = Icons.Default.Info,
            label = "Version",
            subtitle = "1.0.0",
            palette = palette,
            onClick = null,
        )
        SettingsRow(
            icon = Icons.Default.Info,
            label = "Build",
            subtitle = "TocaBonito • studios.drible",
            palette = palette,
            onClick = null,
        )
    }
}

@Composable
private fun SettingsSectionHeader(
    title: String,
    palette: studios.drible.tocabonito.core.ui.theme.ThemePalette,
) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = palette.accent,
        modifier = Modifier.padding(bottom = 4.dp),
    )
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    label: String,
    subtitle: String,
    palette: studios.drible.tocabonito.core.ui.theme.ThemePalette,
    onClick: (() -> Unit)?,
) {
    val modifier = if (onClick != null) {
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp)
    } else {
        Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = palette.accent,
        )
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(text = label, color = palette.textPrimary)
            Text(
                text = subtitle,
                color = palette.textSecondary,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        if (onClick != null) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = palette.textTertiary,
            )
        }
    }
}

private fun SyncStatus.label(): String = when (this) {
    is SyncStatus.Disabled -> "Disabled"
    is SyncStatus.Idle -> "Up to date"
    is SyncStatus.Syncing -> "Syncing…"
    is SyncStatus.Error -> "Sync error"
    is SyncStatus.AccountUnavailable -> "No account"
}
