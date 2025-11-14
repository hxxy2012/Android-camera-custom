package com.proshot.camera.presentation.gallery

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.proshot.camera.data.gallery.Album
import com.proshot.camera.data.gallery.MediaItem
import com.proshot.camera.data.gallery.MediaType
import com.proshot.camera.presentation.theme.AccentBlue
import com.proshot.camera.presentation.theme.AccentGreen
import com.proshot.camera.presentation.theme.AccentOrange
import com.proshot.camera.presentation.theme.AccentRed
import com.proshot.camera.presentation.theme.AccentYellow
import com.proshot.camera.presentation.theme.OverlayBackground
import com.proshot.camera.presentation.theme.PureWhite

/**
 * Gallery grid panel
 */
@Composable
fun GalleryGridPanel(
    modifier: Modifier = Modifier,
    mediaItems: List<MediaItem>,
    onItemClick: (MediaItem) -> Unit,
    onItemLongClick: (MediaItem) -> Unit = {}
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(mediaItems) { item ->
            MediaThumbnail(
                item = item,
                onClick = { onItemClick(item) },
                onLongClick = { onItemLongClick(item) }
            )
        }
    }
}

/**
 * Media thumbnail
 */
@Composable
fun MediaThumbnail(
    item: MediaItem,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(4.dp))
            .background(OverlayBackground)
            .clickable(onClick = onClick)
    ) {
        // Thumbnail image
        Image(
            painter = rememberAsyncImagePainter(item.uri),
            contentDescription = item.displayName,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Video indicator
        if (item.type == MediaType.VIDEO) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .background(OverlayBackground.copy(alpha = 0.8f), CircleShape)
                    .padding(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Videocam,
                    contentDescription = "Video",
                    tint = PureWhite,
                    modifier = Modifier.size(16.dp)
                )
            }

            // Duration
            item.duration?.let { duration ->
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .background(OverlayBackground.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = formatDuration(duration),
                        style = MaterialTheme.typography.labelSmall,
                        color = PureWhite,
                        fontSize = 10.sp
                    )
                }
            }
        }

        // RAW indicator
        if (item.isRaw) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(4.dp)
                    .background(AccentOrange, RoundedCornerShape(4.dp))
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "RAW",
                    style = MaterialTheme.typography.labelSmall,
                    color = PureWhite,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Album card
 */
@Composable
fun AlbumCard(
    album: Album,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(OverlayBackground)
            .border(1.dp, AccentBlue.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
    ) {
        // Cover image
        album.coverItem?.let { coverItem ->
            Image(
                painter = rememberAsyncImagePainter(coverItem.uri),
                contentDescription = album.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        // Album info overlay
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(OverlayBackground.copy(alpha = 0.9f))
                .padding(8.dp)
        ) {
            Column {
                Text(
                    text = album.name,
                    style = MaterialTheme.typography.labelLarge,
                    color = PureWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    maxLines = 1
                )

                Text(
                    text = "${album.itemCount} items",
                    style = MaterialTheme.typography.labelSmall,
                    color = AccentBlue,
                    fontSize = 10.sp
                )
            }
        }
    }
}

/**
 * Media details panel
 */
@Composable
fun MediaDetailsPanel(
    modifier: Modifier = Modifier,
    item: MediaItem,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    Column(
        modifier = modifier
            .background(OverlayBackground, RoundedCornerShape(12.dp))
            .border(1.dp, AccentBlue.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        // Header
        Text(
            text = "DETAILS",
            style = MaterialTheme.typography.titleMedium,
            color = AccentBlue,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Image preview
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(4f / 3f)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Black)
        ) {
            Image(
                painter = rememberAsyncImagePainter(item.uri),
                contentDescription = item.displayName,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Metadata
        DetailRow("Name", item.displayName)
        DetailRow("Resolution", item.resolution)
        DetailRow("Size", item.formattedSize)
        DetailRow("Type", if (item.isRaw) "RAW (${item.mimeType})" else item.mimeType)

        if (item.type == MediaType.VIDEO) {
            item.duration?.let {
                DetailRow("Duration", formatDuration(it))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Edit button (for images only)
            if (item.type == MediaType.IMAGE) {
                MediaActionButton(
                    label = "Edit",
                    color = AccentGreen,
                    onClick = onEdit,
                    modifier = Modifier.weight(1f)
                )
            }

            // Share button
            MediaActionButton(
                icon = { Icon(Icons.Default.Share, contentDescription = "Share") },
                label = "Share",
                color = AccentBlue,
                onClick = onShare,
                modifier = Modifier.weight(1f)
            )

            // Delete button
            MediaActionButton(
                icon = { Icon(Icons.Default.Delete, contentDescription = "Delete") },
                label = "Delete",
                color = AccentRed,
                onClick = onDelete,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Detail row
 */
@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = PureWhite.copy(alpha = 0.7f),
            fontSize = 12.sp
        )

        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = PureWhite,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Media action button
 */
@Composable
fun MediaActionButton(
    icon: (@Composable () -> Unit)? = null,
    label: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.2f))
            .border(1.dp, color, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            icon?.let {
                Box(modifier = Modifier.size(18.dp)) {
                    it()
                }
            }

            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = PureWhite,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Gallery statistics panel
 */
@Composable
fun GalleryStatisticsPanel(
    modifier: Modifier = Modifier,
    totalImages: Int,
    totalVideos: Int,
    rawImages: Int,
    totalSize: String
) {
    Row(
        modifier = modifier
            .background(OverlayBackground, RoundedCornerShape(8.dp))
            .border(1.dp, AccentGreen.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        StatItem("Images", totalImages.toString(), AccentGreen)
        StatItem("Videos", totalVideos.toString(), AccentBlue)
        StatItem("RAW", rawImages.toString(), AccentOrange)
        StatItem("Size", totalSize, AccentYellow)
    }
}

/**
 * Stat item
 */
@Composable
private fun StatItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            color = color,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp
        )

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = PureWhite.copy(alpha = 0.7f),
            fontSize = 10.sp
        )
    }
}

/**
 * Format video duration (milliseconds to MM:SS)
 */
private fun formatDuration(milliseconds: Long): String {
    val totalSeconds = milliseconds / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
