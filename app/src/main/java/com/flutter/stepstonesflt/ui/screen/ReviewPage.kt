package com.flutter.stepstonesflt.ui.screen

import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.util.UnstableApi
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.flutter.stepstonesflt.data.local.entity.MediaItem
import com.flutter.stepstonesflt.data.local.entity.PendingReview
import com.flutter.stepstonesflt.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val ReviewSubtitleColor = Color(0xFF86D6BF)
private val ReviewKeepColor = Color(0xFF404040)
private val ReviewDiscardColor = Color(0xFFB71C1C)
private val ReviewPagerToolbarBg = Color(0x8B000000)

@Composable
fun ReviewPage(
    viewModel: MainViewModel,
    onOpenPager: (initialId: Long, items: List<MediaItem>) -> Unit,
) {
    val review by viewModel.currentReview.collectAsState()
    val count by viewModel.pendingReviewCount.collectAsState()

    if (review == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "No duplicates found",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    val currentReview = review!!

    var itemA by remember { mutableStateOf<MediaItem?>(null) }
    var itemB by remember { mutableStateOf<MediaItem?>(null) }
    LaunchedEffect(currentReview.itemAId, currentReview.itemBId) {
        itemA = viewModel.getItemById(currentReview.itemAId)
        itemB = viewModel.getItemById(currentReview.itemBId)
    }

    ReviewContent(
        review = currentReview,
        count = count,
        itemA = itemA,
        itemB = itemB,
        onKeep = { viewModel.keepBoth(currentReview) },
        onDiscard = { viewModel.deleteReviewItem(currentReview, currentReview.itemBId) },
        onItemClick = { id ->
            val a = itemA; val b = itemB
            if (a != null && b != null) onOpenPager(id, listOf(a, b))
        },
    )
}

@Composable
private fun ReviewContent(
    review: PendingReview,
    count: Int,
    itemA: MediaItem?,
    itemB: MediaItem?,
    onKeep: () -> Unit,
    onDiscard: () -> Unit,
    onItemClick: (Long) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "$count potential duplicate${if (count != 1) "s" else ""}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "${"%.0f".format(review.similarityPercent)}% similar",
                style = MaterialTheme.typography.labelMedium,
                color = ReviewSubtitleColor,
            )
        }

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            itemA?.let { ReviewItemCard(it, Modifier.weight(1f), onClick = { onItemClick(it.id) }) }
            itemB?.let { ReviewItemCard(it, Modifier.weight(1f), onClick = { onItemClick(it.id) }) }
        }

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(42.dp, Alignment.CenterHorizontally),
        ) {
            Button(
                onClick = onKeep,
                shape = RoundedCornerShape(5.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ReviewKeepColor),
                contentPadding = PaddingValues(horizontal = 31.dp, vertical = 11.dp),
            ) { Text("Keep", color = Color.White) }
            Button(
                onClick = onDiscard,
                shape = RoundedCornerShape(5.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ReviewDiscardColor),
                contentPadding = PaddingValues(horizontal = 31.dp, vertical = 11.dp),
            ) { Text("Discard", color = Color.White) }
        }
    }
}

@Composable
private fun ReviewItemCard(item: MediaItem, modifier: Modifier, onClick: () -> Unit) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(item.thumbnailPath)
                .crossfade(true)
                .build(),
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onClick),
            contentScale = ContentScale.Crop,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            item.originalFileName,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        item.mediaDate?.let {
            Text(
                SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(it)),
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
internal fun ReviewPagerView(
    items: List<MediaItem>,
    initialItemId: Long,
    onClose: () -> Unit,
) {
    val initialPage = items.indexOfFirst { it.id == initialItemId }.coerceAtLeast(0)
    val pagerState = rememberPagerState(initialPage = initialPage) { items.size }
    var currentScale by remember { mutableStateOf(1f) }

    LaunchedEffect(pagerState.currentPage) { currentScale = 1f }
    BackHandler(onBack = onClose)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        HorizontalPager(
            state = pagerState,
            userScrollEnabled = currentScale <= 1f,
            modifier = Modifier.fillMaxSize(),
        ) { page ->
            EnlargedMediaContent(
                item = items[page],
                shouldPlay = page == pagerState.currentPage,
                toolbarVisible = false,
                isMuted = false,
                onMuteToggle = {},
                onScaleChanged = { currentScale = it },
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .background(ReviewPagerToolbarBg)
                .statusBarsPadding()
                .padding(4.dp)
                .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {},
        ) {
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.White,
                )
            }
        }
    }
}
