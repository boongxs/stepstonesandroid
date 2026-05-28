package com.flutter.stepstonesflt.ui.screen

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LibraryAdd
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextOverflow
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import androidx.media3.common.MediaItem as Media3Item
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.request.ImageRequest
import coil.size.Size
import me.saket.telephoto.zoomable.coil.ZoomableAsyncImage
import me.saket.telephoto.zoomable.rememberZoomableImageState
import me.saket.telephoto.zoomable.rememberZoomableState
import me.saket.telephoto.zoomable.ZoomSpec
import com.flutter.stepstonesflt.data.local.entity.Album
import com.flutter.stepstonesflt.data.local.entity.MediaItem
import com.flutter.stepstonesflt.data.local.entity.MediaType
import com.flutter.stepstonesflt.data.local.entity.Tag
import com.flutter.stepstonesflt.util.formatDurationMs
import java.io.File
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.delay

private val EnlargeToolbarBackground = Color(0x8B000000)
private val EnlargeDeleteColor = Color(0xFFCF6679)

@Composable
fun EnlargeView(
    mediaItems: List<MediaItem>,
    initialItemId: Long,
    albums: List<Album>,
    onClose: (currentItemId: Long?) -> Unit,
    onAddToAlbum: (Long, Album) -> Unit,
    onDelete: (Long) -> Unit,
    onGetItemTags: (itemId: Long) -> Flow<List<Tag>>,
    onAddTag: (itemId: Long, tagName: String) -> Unit,
    onRemoveTag: (itemId: Long, tagId: Long) -> Unit,
    onUpdateDate: (itemId: Long, newDate: Long) -> Unit,
) {
    val initialPage = mediaItems.indexOfFirst { it.id == initialItemId }.coerceAtLeast(0)
    val pagerState = rememberPagerState(initialPage = initialPage) { mediaItems.size }
    val context = LocalContext.current
    val view = LocalView.current
    val window = remember(context) { (context as Activity).window }

    var toolbarVisible by remember { mutableStateOf(true) }
    var showAddToAlbumDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var currentScale by remember { mutableStateOf(1f) }
    var isMuted by remember { mutableStateOf(false) }
    var playerCurrentMs by remember { mutableStateOf(0L) }
    var playerDurationMs by remember { mutableStateOf(0L) }
    var playerSeekFn by remember { mutableStateOf<((Float) -> Unit)?>(null) }
    var showInfoPanel by remember { mutableStateOf(false) }

    val currentPageItemId = mediaItems.getOrNull(pagerState.currentPage)?.id
    val currentItemTags by produceState<List<Tag>>(emptyList(), currentPageItemId) {
        if (currentPageItemId != null) onGetItemTags(currentPageItemId).collect { value = it }
        else value = emptyList()
    }

    LaunchedEffect(pagerState.currentPage) {
        currentScale = 1f
        playerCurrentMs = 0L
        playerDurationMs = 0L
        playerSeekFn = null
    }

    SideEffect {
        WindowInsetsControllerCompat(window, view).let { ctrl ->
            if (toolbarVisible) {
                ctrl.show(WindowInsetsCompat.Type.systemBars())
            } else {
                ctrl.hide(WindowInsetsCompat.Type.systemBars())
                ctrl.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }
    }
    DisposableEffect(Unit) {
        onDispose { WindowInsetsControllerCompat(window, view).show(WindowInsetsCompat.Type.systemBars()) }
    }

    val handleContentTap = {
        if (!showInfoPanel) {
            toolbarVisible = !toolbarVisible
            if (toolbarVisible) showInfoPanel = false
        }
    }

    val currentItemId = { mediaItems.getOrNull(pagerState.currentPage)?.id }
    BackHandler {
        if (showInfoPanel) showInfoPanel = false
        else onClose(currentItemId())
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        HorizontalPager(
            state = pagerState,
            userScrollEnabled = currentScale <= 1f,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(if (showInfoPanel) 0.5f else 1f)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                ) { handleContentTap() },
        ) { page ->
            val isActivePage = page == pagerState.currentPage
            EnlargedMediaContent(
                item = mediaItems[page],
                shouldPlay = isActivePage,
                toolbarVisible = toolbarVisible && !showInfoPanel,
                isMuted = isMuted,
                onMuteToggle = { isMuted = !isMuted },
                onProgressUpdate = if (isActivePage) { ms, dur -> playerCurrentMs = ms; playerDurationMs = dur } else null,
                onSeekReady = if (isActivePage) { fn -> playerSeekFn = fn } else null,
                onScaleChanged = { currentScale = it },
                onTap = handleContentTap,
            )
        }

        if (toolbarVisible) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth()
                    .background(EnlargeToolbarBackground)
                    .statusBarsPadding()
                    .padding(4.dp)
                    .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {},
            ) {
                IconButton(onClick = { onClose(currentItemId()) }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White,
                    )
                }
            }
        }

        val currentItem = mediaItems.getOrNull(pagerState.currentPage)
        val currentItemIsPlayable = currentItem?.fileType == MediaType.VIDEO || currentItem?.fileType == MediaType.AUDIO
        if (toolbarVisible && !showInfoPanel) Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(EnlargeToolbarBackground)
                .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {},
        ) {
            if (currentItemIsPlayable) {
                SeekBar(
                    currentMs = playerCurrentMs,
                    durationMs = playerDurationMs,
                    onSeek = { fraction -> playerSeekFn?.invoke(fraction) },
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                EnlargeAction(
                    icon = { Icon(Icons.Default.Info, contentDescription = null, tint = Color.White) },
                    label = "Info",
                    onClick = { showInfoPanel = true },
                )
                EnlargeAction(
                    icon = { Icon(Icons.Default.Share, contentDescription = null, tint = Color.White) },
                    label = "Share",
                    onClick = { if (currentItem != null) shareEnlargedItem(context, currentItem) },
                )
                EnlargeAction(
                    icon = { Icon(Icons.Default.LibraryAdd, contentDescription = null, tint = Color.White) },
                    label = "Add to Album",
                    onClick = { showAddToAlbumDialog = true },
                )
                EnlargeAction(
                    icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color.White) },
                    label = "Delete",
                    onClick = { showDeleteConfirmDialog = true },
                )
            }
        }

        if (showInfoPanel && currentItem != null) {
            InfoPanel(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .fillMaxHeight(0.5f),
                item = currentItem,
                tags = currentItemTags,
                onDismiss = { showInfoPanel = false },
                onAddTag = { name -> onAddTag(currentItem.id, name) },
                onRemoveTag = { tag -> onRemoveTag(currentItem.id, tag.id) },
                onUpdateDate = { date -> onUpdateDate(currentItem.id, date) },
            )
        }
    }

    if (showAddToAlbumDialog) {
        val currentItem = mediaItems.getOrNull(pagerState.currentPage)
        AlertDialog(
            onDismissRequest = { showAddToAlbumDialog = false },
            title = { Text("Add to album") },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                    items(albums) { album ->
                        TextButton(
                            onClick = {
                                if (currentItem != null) onAddToAlbum(currentItem.id, album)
                                showAddToAlbumDialog = false
                            },
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
                TextButton(onClick = { showAddToAlbumDialog = false }) { Text("Cancel") }
            },
        )
    }

    if (showDeleteConfirmDialog) {
        val currentItem = mediaItems.getOrNull(pagerState.currentPage)
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = null,
            text = { Text("Remove this item from the album?", color = Color.White) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirmDialog = false
                    if (currentItem != null) onDelete(currentItem.id)
                }) { Text("Remove", color = EnlargeDeleteColor, fontSize = 18.sp) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel", color = Color.White, fontSize = 18.sp)
                }
            },
        )
    }
}

@OptIn(UnstableApi::class)
@Composable
internal fun EnlargedMediaContent(
    item: MediaItem,
    shouldPlay: Boolean,
    toolbarVisible: Boolean,
    isMuted: Boolean,
    onMuteToggle: () -> Unit,
    onProgressUpdate: ((currentMs: Long, durationMs: Long) -> Unit)? = null,
    onSeekReady: ((seekFn: (Float) -> Unit) -> Unit)? = null,
    onScaleChanged: (Float) -> Unit = {},
    onTap: () -> Unit = {},
) {
    val isPlayable = item.fileType == MediaType.VIDEO || item.fileType == MediaType.AUDIO
    val zoomableState = rememberZoomableImageState(rememberZoomableState(ZoomSpec(maxZoomFactor = 10f)))

    LaunchedEffect(item.id) {
        zoomableState.zoomableState.resetZoom(withAnimation = false)
    }

    val context = LocalContext.current
    val exoPlayer = remember(item.id) {
        if (isPlayable) {
            ExoPlayer.Builder(context).build().apply {
                repeatMode = Player.REPEAT_MODE_OFF
                setMediaItem(Media3Item.fromUri(item.filePath))
                prepare()
            }
        } else null
    }

    DisposableEffect(item.id) {
        onDispose { exoPlayer?.release() }
    }

    var isPlaying by remember(item.id) { mutableStateOf(false) }
    var hasEnded by remember(item.id) { mutableStateOf(false) }
    var currentMs by remember(item.id) { mutableStateOf(0L) }
    var durationMs by remember(item.id) { mutableStateOf(item.durationMs ?: 0L) }

    val latestOnProgressUpdate by rememberUpdatedState(onProgressUpdate)
    val latestOnSeekReady by rememberUpdatedState(onSeekReady)

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) { isPlaying = playing }
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) {
                    hasEnded = true
                    currentMs = durationMs
                    latestOnProgressUpdate?.invoke(durationMs, durationMs)
                }
                if (state == Player.STATE_READY) {
                    val dur = exoPlayer?.duration?.coerceAtLeast(0L) ?: durationMs
                    durationMs = dur
                    latestOnProgressUpdate?.invoke(currentMs, dur)
                    latestOnSeekReady?.invoke { fraction ->
                        val seekMs = (fraction * durationMs).toLong()
                        exoPlayer?.seekTo(seekMs)
                        currentMs = seekMs
                        latestOnProgressUpdate?.invoke(seekMs, durationMs)
                    }
                }
            }
        }
        exoPlayer?.addListener(listener)
        onDispose { exoPlayer?.removeListener(listener) }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) exoPlayer?.pause()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            currentMs = exoPlayer?.currentPosition ?: 0L
            latestOnProgressUpdate?.invoke(currentMs, durationMs)
            delay(100L)
        }
    }

    LaunchedEffect(shouldPlay) {
        if (shouldPlay) {
            if (!hasEnded) exoPlayer?.play()
            if (exoPlayer?.playbackState == Player.STATE_READY || exoPlayer?.playbackState == Player.STATE_ENDED) {
                latestOnSeekReady?.invoke { fraction ->
                    val seekMs = (fraction * durationMs).toLong()
                    exoPlayer?.seekTo(seekMs)
                    currentMs = seekMs
                    latestOnProgressUpdate?.invoke(seekMs, durationMs)
                }
                latestOnProgressUpdate?.invoke(currentMs, durationMs)
            }
        } else {
            exoPlayer?.pause()
            exoPlayer?.seekTo(0)
            currentMs = 0L
        }
    }

    LaunchedEffect(isMuted) {
        exoPlayer?.volume = if (isMuted) 0f else 1f
    }

    LaunchedEffect(zoomableState.zoomableState.zoomFraction) {
        onScaleChanged(1f + (zoomableState.zoomableState.zoomFraction ?: 0f) * 9f)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        when (item.fileType) {
            MediaType.AUDIO -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(96.dp),
                )
            }
            MediaType.VIDEO -> AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        useController = false
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    }
                },
                update = { view -> view.player = exoPlayer },
                modifier = Modifier.fillMaxSize(),
            )
            else -> {
                ZoomableAsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(item.filePath)
                        .size(Size.ORIGINAL)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    state = zoomableState,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                    onClick = { onTap() },
                )
            }
        }

        if (isPlayable && toolbarVisible) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = 118.dp),
            ) {
                PlaybackPill(
                    isPlaying = isPlaying,
                    currentMs = if (hasEnded) durationMs else currentMs,
                    durationMs = durationMs,
                    onPlayPauseClick = {
                        if (hasEnded) {
                            hasEnded = false
                            exoPlayer?.seekTo(0)
                            exoPlayer?.play()
                        } else if (isPlaying) {
                            exoPlayer?.pause()
                        } else {
                            exoPlayer?.play()
                        }
                    },
                    modifier = Modifier.align(Alignment.Center),
                )
                MuteCircle(
                    isMuted = isMuted,
                    onToggle = onMuteToggle,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 16.dp),
                )
            }
        }
    }
}

@Composable
private fun PlaybackPill(
    isPlaying: Boolean,
    currentMs: Long,
    durationMs: Long,
    onPlayPauseClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .background(EnlargeToolbarBackground, RoundedCornerShape(50))
            .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(50))
            .padding(start = 4.dp, end = 12.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onPlayPauseClick,
            modifier = Modifier.size(36.dp),
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = Color.White,
            )
        }
        Text(
            text = "${formatDurationMs(currentMs)} / ${formatDurationMs(durationMs)}",
            color = Color.White,
            fontSize = 13.sp,
        )
    }
}

@Composable
private fun MuteCircle(
    isMuted: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(44.dp)
            .background(EnlargeToolbarBackground, CircleShape)
            .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onToggle,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
            contentDescription = if (isMuted) "Unmute" else "Mute",
            tint = Color.White,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun SeekBar(
    currentMs: Long,
    durationMs: Long,
    onSeek: (Float) -> Unit,
) {
    val currentOnSeek by rememberUpdatedState(onSeek)
    val progress = if (durationMs > 0) (currentMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(20.dp)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    currentOnSeek((down.position.x / size.width.toFloat()).coerceIn(0f, 1f))
                    down.consume()
                    var keepGoing = true
                    while (keepGoing) {
                        val event = awaitPointerEvent()
                        keepGoing = event.changes.any { it.pressed }
                        event.changes.forEach { change ->
                            currentOnSeek((change.position.x / size.width.toFloat()).coerceIn(0f, 1f))
                            change.consume()
                        }
                    }
                }
            },
        contentAlignment = Alignment.TopCenter,
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(2.dp)) {
            Box(modifier = Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.3f)))
            Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(progress).background(Color.White))
        }
    }
}

@Composable
private fun EnlargeAction(
    icon: @Composable () -> Unit,
    label: String,
    onClick: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(onClick = onClick) { icon() }
        Text(
            text = label,
            fontSize = 11.sp,
            color = Color.White,
            modifier = Modifier.offset(y = (-2).dp),
        )
    }
}

private fun shareEnlargedItem(context: Context, item: MediaItem) {
    val uri = try {
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            File(item.filePath),
        )
    } catch (e: Exception) { return }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = context.contentResolver.getType(uri) ?: "*/*"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share"))
}

