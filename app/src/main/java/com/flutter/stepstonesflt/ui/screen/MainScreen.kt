package com.flutter.stepstonesflt.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.flutter.stepstonesflt.data.local.entity.Album
import com.flutter.stepstonesflt.ui.components.AlbumSelector
import com.flutter.stepstonesflt.ui.components.StepstonesSearchBar
import com.flutter.stepstonesflt.ui.viewmodel.MainViewModel

private val viewNames = listOf("Media Grid", "Review")
private val SubtitleColor = Color(0xFF86D6BF)
private val LibraryHeaderBackground = Color(0xFF222222)
private val LibraryHeaderContent = Color(0xFFDEE4E0)
private val LibraryHeaderSecondary = Color(0xFFAAAAAA)
private val FolderIconColor = Color(0xFFFFFFFF)

@Composable
fun MainScreen(viewModel: MainViewModel = hiltViewModel()) {
    val albums by viewModel.albums.collectAsState()
    val selectedAlbum by viewModel.selectedAlbum.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val mediaItemCount by viewModel.mediaItemCount.collectAsState()
    val pendingCount by viewModel.pendingCount.collectAsState()
    val ingestInProgress by viewModel.ingestInProgress.collectAsState()

    val pagerState = rememberPagerState(pageCount = { viewNames.size })
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.snackbarMessages.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    LaunchedEffect(pendingCount, albums.size) {
        if (pendingCount > 0 && albums.size == 1) {
            viewModel.ingestToAlbum(albums[0])
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            AppTitle(modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))

            StepstonesSearchBar(
                query = searchQuery,
                onQueryChange = viewModel::setSearchQuery,
                onSearch = viewModel::triggerSearch,
            )

            LibraryHeader(
                albumName = selectedAlbum?.name ?: "—",
                itemCount = mediaItemCount,
                albums = albums,
                selectedAlbum = selectedAlbum,
                onAlbumSelected = viewModel::selectAlbum,
                onCreateAlbum = viewModel::createAlbum,
                onRenameAlbum = viewModel::renameAlbum,
                onDeleteAlbum = viewModel::deleteAlbum,
            )

            ViewLabel(
                currentPage = pagerState.currentPage,
                pageCount = viewNames.size,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
            )

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) { page ->
                when (page) {
                    0 -> GridPagePlaceholder()
                    1 -> ReviewPagePlaceholder()
                }
            }
        }
    }

    if (pendingCount > 0 && albums.size > 1) {
        IngestAlbumPickerDialog(
            albums = albums,
            pendingCount = pendingCount,
            onAlbumPicked = viewModel::ingestToAlbum,
            onDismiss = viewModel::dismissIngest,
        )
    }

    if (ingestInProgress) {
        IngestProgressDialog()
    }
}

@Composable
private fun AppTitle(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Column {
            Text(
                text = "Stepstones",
                fontSize = 48.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = "Media Manager",
                fontSize = 24.sp,
                fontWeight = FontWeight.Medium,
                color = SubtitleColor,
                letterSpacing = 0.5.sp,
            )
        }
    }
}

@Composable
private fun LibraryHeader(
    albumName: String,
    itemCount: Int,
    albums: List<Album>,
    selectedAlbum: Album?,
    onAlbumSelected: (Album) -> Unit,
    onCreateAlbum: (String) -> Unit,
    onRenameAlbum: (Album, String) -> Unit,
    onDeleteAlbum: (Album) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 5.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(LibraryHeaderBackground)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AlbumSelector(
            albums = albums,
            selectedAlbum = selectedAlbum,
            onAlbumSelected = onAlbumSelected,
            onCreateAlbum = onCreateAlbum,
            onRenameAlbum = onRenameAlbum,
            onDeleteAlbum = onDeleteAlbum,
            trigger = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = "Select album",
                        tint = FolderIconColor,
                        modifier = Modifier.size(32.dp),
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = albumName,
                            color = LibraryHeaderContent,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = "Currently showing $itemCount media items",
                            color = LibraryHeaderSecondary,
                            fontSize = 12.sp,
                        )
                    }
                }
            },
        )
    }
}

@Composable
private fun ViewLabel(
    currentPage: Int,
    pageCount: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Default.KeyboardArrowLeft,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.alpha(if (currentPage > 0) 1f else 0.2f),
        )
        Text(
            text = viewNames[currentPage],
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        Icon(
            imageVector = Icons.Default.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.alpha(if (currentPage < pageCount - 1) 1f else 0.2f),
        )
    }
}

@Composable
private fun IngestAlbumPickerDialog(
    albums: List<Album>,
    pendingCount: Int,
    onAlbumPicked: (Album) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (pendingCount == 1) "Add 1 item to album" else "Add $pendingCount items to album")
        },
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

@Composable
private fun IngestProgressDialog() {
    AlertDialog(
        onDismissRequest = {},
        title = { Text("Adding media…") },
        text = {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        },
        confirmButton = {},
    )
}

@Composable
private fun GridPagePlaceholder() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = "Media Grid",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ReviewPagePlaceholder() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = "Review",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
