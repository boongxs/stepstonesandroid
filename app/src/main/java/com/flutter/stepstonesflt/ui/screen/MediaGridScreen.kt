package com.flutter.stepstonesflt.ui.screen

import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.LibraryAdd
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.flutter.stepstonesflt.data.local.entity.MediaItem
import com.flutter.stepstonesflt.data.local.entity.MediaType
import com.flutter.stepstonesflt.ui.viewmodel.MainViewModel
import java.io.File

private val SelectionToolbarBackground = Color(0xFF1E1E1E)
private val BadgeBackground = Color(0x88000000)
private val AudioCellBackground = Color(0xFF3D3D3D)
private val DeleteColor = Color(0xFFCF6679)

@Composable
fun MediaGridPage(viewModel: MainViewModel, gridState: LazyGridState) {
    val mediaItems by viewModel.mediaItems.collectAsState()
    val selectedItemIds by viewModel.selectedItemIds.collectAsState()
    val isSelectionMode by viewModel.isSelectionMode.collectAsState()
    val albums by viewModel.albums.collectAsState()
    val context = LocalContext.current

    var showAddToAlbumDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    BackHandler(enabled = isSelectionMode) {
        viewModel.clearSelection()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (mediaItems.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Share media to add it here",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                state = gridState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(1.dp),
                verticalArrangement = Arrangement.spacedBy(1.dp),
                horizontalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                items(mediaItems, key = { it.id }) { item ->
                    MediaGridCell(
                        item = item,
                        isSelected = item.id in selectedItemIds,
                        isSelectionMode = isSelectionMode,
                        onLongPress = { viewModel.toggleSelection(item.id) },
                        onClick = {
                            if (isSelectionMode) viewModel.toggleSelection(item.id)
                            else viewModel.openEnlargeView(item.id)
                        },
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = isSelectionMode,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            SelectionToolbar(
                onShare = {
                    shareMediaItems(context, viewModel.getSelectedItems())
                    viewModel.clearSelection()
                },
                onDelete = { showDeleteConfirmDialog = true },
                onExport = { /* Step 8 */ },
                onAddToAlbum = { showAddToAlbumDialog = true },
            )
        }
    }

    if (showAddToAlbumDialog) {
        AddToAlbumDialog(
            albums = albums,
            onAlbumPicked = { album ->
                viewModel.addSelectedToAlbum(album)
                showAddToAlbumDialog = false
            },
            onDismiss = { showAddToAlbumDialog = false },
        )
    }

    if (showDeleteConfirmDialog) {
        val count = selectedItemIds.size
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = null,
            text = {
                Text(
                    "Remove $count ${if (count == 1) "item" else "items"} from the album?",
                    color = Color.White,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirmDialog = false
                    viewModel.deleteSelected()
                }) { Text("Remove", color = DeleteColor, fontSize = 18.sp, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) { Text("Cancel", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold) }
            },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MediaGridCell(
    item: MediaItem,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onLongPress: () -> Unit,
    onClick: () -> Unit,
) {

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongPress,
            ),
    ) {
        // Thumbnail
        if (item.fileType == MediaType.AUDIO || item.thumbnailPath == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(AudioCellBackground),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(40.dp),
                )
            }
        } else {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(item.thumbnailPath)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }

        // Badge (bottom-end)
        val badge = when (item.fileType) {
            MediaType.GIF -> "GIF"
            MediaType.VIDEO, MediaType.AUDIO -> item.durationMs?.let { formatDuration(it) }
            MediaType.IMAGE -> null
        }
        if (badge != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp) // outer spacing from cell corner
                    .background(BadgeBackground, RoundedCornerShape(3.dp))
                    .padding(horizontal = 4.dp),
            ) {
                Text(
                    text = badge,
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }

        // Selection indicator (top-start)
        if (isSelectionMode) {
            Box(modifier = Modifier.padding(6.dp)) {
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .border(2.dp, Color.White, CircleShape)
                            .background(Color.Black.copy(alpha = 0.3f), CircleShape),
                    )
                }
            }
        }

        // Dim overlay on unselected items during selection mode
        if (isSelectionMode && !isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f)),
            )
        }
    }
}

@Composable
private fun SelectionToolbar(
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onExport: () -> Unit,
    onAddToAlbum: () -> Unit,
) {
    HorizontalDivider(color = Color(0xFF3A3A3A))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SelectionToolbarBackground)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        ToolbarAction(icon = { Icon(Icons.Default.Share, contentDescription = null) }, label = "Share", onClick = onShare)
        ToolbarAction(icon = { Icon(Icons.Default.Delete, contentDescription = null) }, label = "Delete", onClick = onDelete)
        ToolbarAction(icon = { Icon(Icons.Default.FileUpload, contentDescription = null) }, label = "Export", onClick = onExport)
        ToolbarAction(icon = { Icon(Icons.Default.LibraryAdd, contentDescription = null) }, label = "Add to Album", onClick = onAddToAlbum)
    }
}

@Composable
private fun ToolbarAction(
    icon: @Composable () -> Unit,
    label: String,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        IconButton(onClick = onClick) { icon() }
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.offset(y = (-4).dp),
        )
    }
}

@Composable
private fun AddToAlbumDialog(
    albums: List<com.flutter.stepstonesflt.data.local.entity.Album>,
    onAlbumPicked: (com.flutter.stepstonesflt.data.local.entity.Album) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add to album") },
        text = {
            LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                items(albums) { album ->
                    TextButton(
                        onClick = { onAlbumPicked(album) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = album.name,
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

private fun formatDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds)
    else "%d:%02d".format(minutes, seconds)
}

private fun shareMediaItems(context: Context, items: List<MediaItem>) {
    val uris = items.mapNotNull { item ->
        try {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                File(item.filePath),
            )
        } catch (e: Exception) { null }
    }
    if (uris.isEmpty()) return
    val intent = if (uris.size == 1) {
        Intent(Intent.ACTION_SEND).apply {
            type = context.contentResolver.getType(uris[0]) ?: "*/*"
            putExtra(Intent.EXTRA_STREAM, uris[0])
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    } else {
        Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "*/*"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
    context.startActivity(Intent.createChooser(intent, "Share"))
}
