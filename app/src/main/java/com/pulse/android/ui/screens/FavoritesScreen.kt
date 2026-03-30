package com.pulse.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.SubcomposeAsyncImage
import com.pulse.android.data.Favorite
import com.pulse.android.data.Track
import com.pulse.android.ui.theme.LocalPulseColors
import com.pulse.android.viewmodel.PlayerViewModel
import java.util.UUID

@Composable
fun FavoritesScreen(vm: PlayerViewModel, navController: NavController) {
    val colors = LocalPulseColors.current
    val favorites by vm.favorites.collectAsState()

    Column(modifier = Modifier.fillMaxSize().background(colors.bg)) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "Favorites",
                color = colors.textPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
            IconButton(onClick = { vm.loadFavorites() }, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = colors.textMuted, modifier = Modifier.size(18.dp))
            }
        }

        HorizontalDivider(color = colors.border, thickness = 0.5.dp)

        if (favorites.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No favorites yet.\nTap ♥ on a track to add it.", color = colors.textMuted, fontSize = 14.sp)
            }
        } else {
            LazyColumn {
                itemsIndexed(favorites) { idx, fav ->
                    val isFav = vm.isFavorite(fav.filePath)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val queue = favorites.map { it.toTrack(vm) }
                                vm.playTrack(queue[idx], queue, idx)
                                navController.navigate("nowplaying")
                            }
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .background(colors.surface, RoundedCornerShape(6.dp))
                                .clip(RoundedCornerShape(6.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            val albumFolder = fav.filePath.substringBeforeLast("/")
                            SubcomposeAsyncImage(
                                model = vm.b2.getStreamUrl("$albumFolder/cover.jpg"),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                                error = {
                                    Icon(Icons.Default.AudioFile, contentDescription = null, tint = colors.textMuted, modifier = Modifier.size(20.dp))
                                }
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                fav.title.ifEmpty { fav.filePath.substringAfterLast("/").substringBeforeLast(".") },
                                color = colors.textPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                            )
                            if (fav.artist.isNotEmpty()) {
                                Text(fav.artist, color = colors.textMuted, fontSize = 11.sp, maxLines = 1)
                            }
                        }
                        val fmt = fav.format.uppercase()
                        if (fmt.isNotEmpty()) {
                            Text(
                                fmt,
                                color = if (fmt == "FLAC") colors.green else colors.textDim,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .background(
                                        if (fmt == "FLAC") colors.greenFaint else colors.surface2,
                                        RoundedCornerShape(4.dp)
                                    )
                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                        IconButton(
                            onClick = { vm.toggleFavorite(fav.toTrack(vm)) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Unfavorite",
                                tint = if (isFav) androidx.compose.ui.graphics.Color(0xFFEF4444) else colors.textDim,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    HorizontalDivider(color = colors.border, thickness = 0.5.dp)
                }
            }
        }
    }
}

private fun Favorite.toTrack(vm: PlayerViewModel) = Track(
    id = UUID.randomUUID().toString(),
    title = title,
    artist = artist,
    album = album,
    duration = 0L,
    format = format,
    streamUrl = vm.b2.getStreamUrl(filePath),
    filePath = filePath,
    albumArtUrl = vm.b2.getStreamUrl(filePath.substringBeforeLast("/") + "/cover.jpg"),
)
