package studios.drible.tocabonito.feature.detail

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import studios.drible.tocabonito.core.ui.theme.LocalThemePalette

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormatGuideScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalThemePalette.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Format Guide", color = palette.textPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = palette.textPrimary,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = palette.background,
                ),
            )
        },
        containerColor = palette.background,
        modifier = modifier,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            FormatSection(
                title = "Quality Tiers",
                items = listOf(
                    "4K / 2160p" to "Best quality. Requires fast connection (25+ Mbps).",
                    "1080p" to "Full HD. Good balance of quality and bandwidth.",
                    "720p" to "HD. Works on slower connections.",
                    "480p" to "SD. Minimum acceptable quality.",
                ),
            )

            FormatSection(
                title = "Codecs",
                items = listOf(
                    "x265 / HEVC" to "Modern codec. Smaller files, same quality. Needs hardware decode.",
                    "x264 / AVC" to "Universal compatibility. Larger files.",
                    "AV1" to "Newest codec. Best compression but limited device support.",
                ),
            )

            FormatSection(
                title = "Sources",
                items = listOf(
                    "BluRay / BDRip" to "Ripped from retail Blu-ray. Best quality source.",
                    "WEB-DL" to "Downloaded from streaming service. No re-encoding.",
                    "WEBRip" to "Screen-captured from streaming. Slightly lower quality than WEB-DL.",
                    "HDRip" to "HD source, usually re-encoded. Variable quality.",
                ),
            )

            FormatSection(
                title = "Audio",
                items = listOf(
                    "Atmos / TrueHD" to "Lossless surround. Best audio on compatible receivers.",
                    "DTS-HD MA" to "Lossless surround. Wide receiver support.",
                    "DD+ / EAC3" to "Lossy surround. Good quality, streaming standard.",
                    "AAC" to "Stereo or surround. Smallest size, universal playback.",
                ),
            )

            FormatSection(
                title = "HDR",
                items = listOf(
                    "Dolby Vision" to "Dynamic HDR. Best on DV-compatible displays.",
                    "HDR10+" to "Dynamic HDR. Samsung ecosystem preferred.",
                    "HDR10" to "Static HDR. Widely supported on 4K displays.",
                    "SDR" to "Standard dynamic range. Works on all displays.",
                ),
            )

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun FormatSection(
    title: String,
    items: List<Pair<String, String>>,
) {
    val palette = LocalThemePalette.current

    Spacer(Modifier.height(16.dp))

    Text(
        text = title,
        color = palette.textPrimary,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
    )

    Spacer(Modifier.height(8.dp))

    items.forEach { (name, description) ->
        Text(
            text = name,
            color = palette.accent,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 8.dp),
        )
        Text(
            text = description,
            color = palette.textSecondary,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}
