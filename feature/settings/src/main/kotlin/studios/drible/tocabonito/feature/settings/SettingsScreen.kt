package studios.drible.tocabonito.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import studios.drible.tocabonito.core.data.preferences.TorrentioPreferences
import studios.drible.tocabonito.core.domain.service.SyncStatus
import studios.drible.tocabonito.core.ui.theme.LocalThemePalette
import studios.drible.tocabonito.core.ui.theme.ThemePalette

@Composable
fun SettingsScreen(
    onNavigateToThemePicker: () -> Unit,
    onSignInClick: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val palette = LocalThemePalette.current
    val currentTheme by viewModel.themeProvider.selectedTheme.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsStateWithLifecycle()
    val dataPortabilityState by viewModel.dataPortabilityState.collectAsStateWithLifecycle()
    val apiValidationState by viewModel.apiValidationState.collectAsStateWithLifecycle()
    val storedApiKey by viewModel.storedApiKey.collectAsStateWithLifecycle()
    val torrentioProviders by viewModel.torrentioProviders.collectAsStateWithLifecycle()
    val torrentioLanguage by viewModel.torrentioLanguage.collectAsStateWithLifecycle()
    val isSignedIn by viewModel.isSignedIn.collectAsStateWithLifecycle()
    val displayName by viewModel.displayName.collectAsStateWithLifecycle()

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

        // — Real-Debrid section —
        SettingsSectionHeader(title = "Real-Debrid", palette = palette)
        RealDebridSection(
            storedApiKey = storedApiKey,
            validationState = apiValidationState,
            palette = palette,
            onValidate = { viewModel.validateApiKey(it) },
            onSave = { viewModel.saveApiKey(it) },
            onClear = { viewModel.clearApiKey() },
        )

        Spacer(Modifier.height(16.dp))
        HorizontalDivider(color = palette.textTertiary.copy(alpha = 0.2f))
        Spacer(Modifier.height(16.dp))

        // — Torrentio Advanced section —
        SettingsSectionHeader(title = "Torrentio Advanced", palette = palette)
        TorrentioSection(
            providers = torrentioProviders,
            language = torrentioLanguage,
            palette = palette,
            onProvidersChange = { viewModel.updateProviders(it) },
            onLanguageChange = { viewModel.updateLanguage(it) },
        )

        Spacer(Modifier.height(16.dp))
        HorizontalDivider(color = palette.textTertiary.copy(alpha = 0.2f))
        Spacer(Modifier.height(16.dp))

        // — Account section —
        SettingsSectionHeader(title = "Account", palette = palette)
        if (isSignedIn) {
            SettingsRow(
                icon = Icons.Default.AccountCircle,
                label = displayName ?: "Signed In",
                subtitle = "Google Account connected",
                palette = palette,
                onClick = null,
            )
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { viewModel.signOut() },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Sign Out", color = palette.textPrimary)
            }
        } else {
            SettingsRow(
                icon = Icons.Default.CloudOff,
                label = "Cloud Sync",
                subtitle = "Sign in to sync across devices",
                palette = palette,
                onClick = null,
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onSignInClick,
                colors = ButtonDefaults.buttonColors(containerColor = palette.accent),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Sign in with Google")
            }
        }
        Spacer(Modifier.height(4.dp))
        SettingsRow(
            icon = Icons.Default.CloudOff,
            label = "Sync Status",
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
private fun RealDebridSection(
    storedApiKey: String?,
    validationState: ApiValidationState,
    palette: ThemePalette,
    onValidate: (String) -> Unit,
    onSave: (String) -> Unit,
    onClear: () -> Unit,
) {
    var apiKeyInput by remember(storedApiKey) { mutableStateOf(storedApiKey ?: "") }

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Key, contentDescription = null, tint = palette.accent)
            Spacer(Modifier.width(12.dp))
            Text("API Key", color = palette.textPrimary)
        }
        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = apiKeyInput,
            onValueChange = { apiKeyInput = it },
            label = { Text("Real-Debrid API Key") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { onValidate(apiKeyInput) },
                enabled = apiKeyInput.isNotBlank() && validationState !is ApiValidationState.Loading,
                colors = ButtonDefaults.buttonColors(containerColor = palette.accent),
            ) {
                Text("Validate")
            }
            Button(
                onClick = { onSave(apiKeyInput) },
                enabled = apiKeyInput.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = palette.accent),
            ) {
                Text("Save")
            }
            OutlinedButton(onClick = {
                onClear()
                apiKeyInput = ""
            }) {
                Icon(Icons.Default.Clear, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Clear")
            }
        }

        Spacer(Modifier.height(8.dp))

        when (validationState) {
            is ApiValidationState.Loading -> {
                CircularProgressIndicator(color = palette.accent)
            }
            is ApiValidationState.Success -> {
                Card(
                    colors = CardDefaults.cardColors(containerColor = palette.cardBackground),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = palette.accent)
                            Spacer(Modifier.width(8.dp))
                            Text("Valid", color = palette.accent, style = MaterialTheme.typography.labelMedium)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text("User: ${validationState.user.username}", color = palette.textPrimary)
                        Text("Email: ${validationState.user.email}", color = palette.textSecondary, style = MaterialTheme.typography.bodySmall)
                        Text("Type: ${validationState.user.type}", color = palette.textSecondary, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            is ApiValidationState.Error -> {
                Text(
                    text = "❌ ${validationState.message}",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            is ApiValidationState.Idle -> { /* no-op */ }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun TorrentioSection(
    providers: List<String>,
    language: String,
    palette: ThemePalette,
    onProvidersChange: (List<String>) -> Unit,
    onLanguageChange: (String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.VideoLibrary, contentDescription = null, tint = palette.accent)
            Spacer(Modifier.width(12.dp))
            Text("Providers", color = palette.textPrimary)
        }
        Spacer(Modifier.height(8.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            TorrentioPreferences.DEFAULT_PROVIDERS.forEach { provider ->
                val selected = provider in providers
                FilterChip(
                    selected = selected,
                    onClick = {
                        val updated = if (selected) providers - provider else providers + provider
                        onProvidersChange(updated)
                    },
                    label = { Text(provider) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = palette.accent.copy(alpha = 0.2f),
                        selectedLabelColor = palette.accent,
                    ),
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Text("Language", color = palette.textPrimary)
        Spacer(Modifier.height(4.dp))

        var expanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
        ) {
            OutlinedTextField(
                value = language.replaceFirstChar { it.uppercase() },
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                TorrentioPreferences.AVAILABLE_LANGUAGES.forEach { lang ->
                    DropdownMenuItem(
                        text = { Text(lang.replaceFirstChar { it.uppercase() }) },
                        onClick = {
                            onLanguageChange(lang)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionHeader(
    title: String,
    palette: ThemePalette,
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
    palette: ThemePalette,
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
