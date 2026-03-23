package com.pulse.android.ui.screens

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.ChevronRight
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.SubcomposeAsyncImage
import com.pulse.android.ui.theme.LocalPulseColors
import com.pulse.android.viewmodel.PlayerViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

private data class AlbumEntry(
    val name: String,
    val artist: String,
    val prefix: String,
    val coverUrl: String,
)

private val artworkFolderNames = setOf("artwork", "scans", "covers", "images", "art", "booklet", "extras")

@Composable
fun AlbumsScreen(vm: PlayerViewModel, navController: NavController) {
    val colors = LocalPulseColors.current
    val isConnected by vm.isConnected.collectAsState()

    var albums by remember { mutableStateOf<List<AlbumEntry>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var loadError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(isConnected) {
        if (isConnected && albums.isEmpty()) {
            isLoading = true
            loadError = null
            try {
                vm.b2.listFiles("Music/").onSuccess { artistFolders ->
                    val all = mutableListOf<AlbumEntry>()
                    coroutineScope {
                        artistFolders.filter { it.isFolder }.map { artist ->
                            async {
                                vm.b2.listFiles(artist.name).onSuccess { children ->
                                    val artistName = artist.name.removePrefix("Music/").trimEnd('/')
                                    children.filter { it.isFolder }.forEach { album ->
                                        val albumName = album.name.removePrefix(artist.name).trimEnd('/')
                                        if (artworkFolderNames.none { albumName.lowercase().contains(it) }) {
                                            synchronized(all) {
                                                all.add(
                                                    AlbumEntry(
                                                        name = albumName,
                                                        artist = artistName,
                                                        prefix = album.name,
                                                        coverUrl = vm.b2.getStreamUrl("${album.name}cover.jpg"),
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }.awaitAll()
                    }
                    albums = all.sortedWith(compareBy({ it.name.lowercase() }, { it.artist.lowercase() }))
                }.onFailure {
                    loadError = it.message
                }
            } catch (e: Exception) {
                loadError = e.message
            }
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Albums", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
            if (albums.isNotEmpty()) {
                Text("${albums.size} Albums", fontSize = 12.sp, color = colors.textMuted)
            }
        }

        HorizontalDivider(color = colors.border, thickness = 0.5.dp)

        when {
            !isConnected -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Not connected to B2", color = colors.textMuted, fontSize = 14.sp)
            }
            isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    CircularProgressIndicator(color = colors.green, modifier = Modifier.size(32.dp))
                    Text("Loading albums…", color = colors.textMuted, fontSize = 12.sp)
                }
            }
            loadError != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Error: $loadError", color = Color(0xFFFF6B6B), fontSize = 13.sp, modifier = Modifier.padding(24.dp))
            }
            else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(albums) { album ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val encoded = Uri.encode(album.prefix)
                                navController.navigate("cloud_prefix?p=$encoded")
                            }
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(colors.surface, RoundedCornerShape(6.dp))
                                .clip(RoundedCornerShape(6.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            SubcomposeAsyncImage(
                                model = album.coverUrl,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                                error = {
                                    Icon(Icons.Default.Album, contentDescription = null, tint = colors.textDim, modifier = Modifier.size(22.dp))
                                }
                            )
                        }
                        Column(modifier = Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.Center) {
                            Text(album.name, color = colors.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                            Text(album.artist, color = colors.textMuted, fontSize = 11.sp, maxLines = 1)
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = colors.textDim, modifier = Modifier.size(16.dp))
                    }
                    HorizontalDivider(color = colors.border, thickness = 0.5.dp)
                }
            }
        }
    }
}
