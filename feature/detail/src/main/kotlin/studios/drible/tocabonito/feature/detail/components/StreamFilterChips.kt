package studios.drible.tocabonito.feature.detail.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import studios.drible.tocabonito.core.ui.theme.LocalThemePalette
import studios.drible.tocabonito.feature.detail.model.StreamFilters

@Composable
fun StreamFilterChips(
    filters: StreamFilters,
    availableQualities: List<String>,
    availableSources: List<String>,
    availableLanguages: List<String>,
    onFiltersChanged: (StreamFilters) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalThemePalette.current

    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (availableQualities.size > 1) {
            item(key = "quality") {
                FilterDropdownChip(
                    label = "Quality",
                    selectedValue = filters.quality,
                    options = availableQualities,
                    onSelected = { onFiltersChanged(filters.copy(quality = it)) },
                )
            }
        }

        if (availableSources.size > 1) {
            item(key = "source") {
                FilterDropdownChip(
                    label = "Source",
                    selectedValue = filters.source,
                    options = availableSources,
                    onSelected = { onFiltersChanged(filters.copy(source = it)) },
                )
            }
        }

        if (availableLanguages.size > 1) {
            item(key = "language") {
                FilterDropdownChip(
                    label = "Language",
                    selectedValue = filters.language,
                    options = availableLanguages,
                    onSelected = { onFiltersChanged(filters.copy(language = it)) },
                )
            }
        }

        if (filters.isActive) {
            item(key = "clear") {
                FilterChip(
                    selected = false,
                    onClick = { onFiltersChanged(StreamFilters.EMPTY) },
                    label = { Text("Clear", fontSize = 13.sp) },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear filters",
                            modifier = Modifier.size(16.dp),
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = palette.cardBackground,
                        labelColor = Color.White,
                    ),
                )
            }
        }
    }
}

@Composable
private fun FilterDropdownChip(
    label: String,
    selectedValue: String?,
    options: List<String>,
    onSelected: (String?) -> Unit,
) {
    val palette = LocalThemePalette.current
    var expanded by remember { mutableStateOf(false) }

    Box {
        FilterChip(
            selected = selectedValue != null,
            onClick = { expanded = true },
            label = {
                Text(
                    text = selectedValue ?: label,
                    fontSize = 13.sp,
                )
            },
            colors = FilterChipDefaults.filterChipColors(
                containerColor = palette.cardBackground,
                selectedContainerColor = palette.accent.copy(alpha = 0.15f),
                labelColor = Color.White,
                selectedLabelColor = if (palette.isLight) palette.cardBackground else palette.accent,
            ),
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            if (selectedValue != null) {
                DropdownMenuItem(
                    text = { Text("All $label") },
                    onClick = {
                        onSelected(null)
                        expanded = false
                    },
                )
            }
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    },
                )
            }
        }
    }
}
