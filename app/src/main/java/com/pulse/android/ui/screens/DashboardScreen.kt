package com.pulse.android.ui.screens

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.SubcomposeAsyncImage
import com.pulse.android.data.B2File
import com.pulse.android.data.Track
import com.pulse.android.ui.theme.LocalPulseColors
import com.pulse.android.viewmodel.PlayerViewModel
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
fun DashboardScreen(vm: PlayerViewModel, navController: NavController) {
    val colors = LocalPulseColors.current
    val isConnected by vm.isConnected.collectAsState()
    val scope = rememberCoroutineScope()

    var artists by remember { mutableStateOf<List<B2File>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var isShuffleLoading by remember { mutableStateOf(false) }

    LaunchedEffect(isConnected) {
        if (isConnected && artists.isEmpty()) {
            scope.launch {
                isLoading = true
                loadError = null
                vm.b2.listFiles("Music/").onSuccess { files ->
                    artists = files.filter { it.isFolder }
                }.onFailure {
                    loadError = it.message
                }
                isLoading = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
    ) {
        // Header bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Artists",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )
            if (artists.isNotEmpty()) {
                Text(
                    "${artists.size} Artists",
                    fontSize = 12.sp,
                    color = colors.textMuted
                )
            }
            Spacer(Modifier.weight(1f))
            Button(
                onClick = {
                    scope.launch {
                        isShuffleLoading = true
                        vm.b2.listAllFiles("Music/").onSuccess { allFiles ->
                            val queue = allFiles.map { f ->
                                val fileName = f.name.substringAfterLast("/")
                                val albumFolder = f.name.substringBeforeLast("/")
                                val parts = f.name.removePrefix("Music/").split("/")
                                Track(
                                    id = UUID.randomUUID().toString(),
                                    title = fileName.substringBeforeLast(".").replace(Regex("^\\d+[.\\-\\s]+"), "").trim(),
                                    artist = if (parts.size > 1) parts[0] else "Unknown",
                                    album = if (parts.size > 2) parts[1] else "",
                                    duration = 0L,
                                    format = fileName.substringAfterLast(".").uppercase(),
                                    streamUrl = vm.b2.getStreamUrl(f.name),
                                    filePath = f.name,
                                    albumArtUrl = vm.b2.getStreamUrl("$albumFolder/cover.jpg"),
                                )
                            }.shuffled()
                            if (queue.isNotEmpty()) {
                                vm.playTrack(queue[0], queue, 0)
                                navController.navigate("nowplaying")
                            }
                        }
                        isShuffleLoading = false
                    }
                },
                enabled = artists.isNotEmpty() && !isShuffleLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.greenDim,
                    contentColor = colors.green,
                ),
                shape = RoundedCornerShape(8.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            ) {
                if (isShuffleLoading) {
                    CircularProgressIndicator(color = colors.green, modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Shuffle, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Shuffle All", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        HorizontalDivider(color = colors.border, thickness = 0.5.dp)

        when {
            !isConnected -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Not connected to B2", color = colors.textMuted, fontSize = 14.sp)
            }
            isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = colors.green, modifier = Modifier.size(32.dp))
            }
            loadError != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "Error: $loadError",
                    color = Color(0xFFFF6B6B),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(24.dp)
                )
            }
            else -> LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 128.dp),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(artists) { artist ->
                    val name = artist.name.removePrefix("Music/").trimEnd('/')
                    val artUrl = vm.b2.getStreamUrl("${artist.name}artist.jpg")
                    ArtistCard(
                        name = name,
                        artUrl = artUrl,
                        onClick = {
                            val encoded = Uri.encode(artist.name)
                            navController.navigate("cloud_prefix?p=$encoded")
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ArtistCard(name: String, artUrl: String, onClick: () -> Unit) {
    val colors = LocalPulseColors.current
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(colors.surface, RoundedCornerShape(8.dp))
                .clip(RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            SubcomposeAsyncImage(
                model = artUrl,
                contentDescription = name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                error = {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = colors.textDim,
                        modifier = Modifier.size(36.dp)
                    )
                }
            )
        }
        Text(
            text = name,
            color = colors.textPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 2,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
