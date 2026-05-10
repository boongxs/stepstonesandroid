package com.flutter.stepstonesflt.ui.screen

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flutter.stepstonesflt.data.local.entity.MediaItem
import com.flutter.stepstonesflt.data.local.entity.MediaType
import com.flutter.stepstonesflt.data.local.entity.Tag
import com.flutter.stepstonesflt.util.formatDurationMs
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val InfoPanelBg = Color(0xF0121212)
private val InfoLabelColor = Color(0xFF888888)
private val InfoValueColor = Color(0xFFDEDEDE)
private val InfoClickableColor = Color(0xFF86D6BF)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InfoPanel(
    modifier: Modifier = Modifier,
    item: MediaItem,
    tags: List<Tag>,
    onDismiss: () -> Unit,
    onAddTag: (String) -> Unit,
    onRemoveTag: (Tag) -> Unit,
    onUpdateDate: (Long) -> Unit,
) {
    val context = LocalContext.current
    var showTagDialog by remember { mutableStateOf(false) }

    Box(modifier = modifier.background(InfoPanelBg)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        ) {
            val dateStr = if (item.mediaDate != null)
                SimpleDateFormat("MMM d, yyyy  HH:mm", Locale.getDefault()).format(Date(item.mediaDate))
            else "Not set"
            InfoRow(
                label = "Date",
                value = dateStr,
                clickable = true,
                onClick = {
                    val cal = Calendar.getInstance().apply {
                        timeInMillis = item.mediaDate ?: System.currentTimeMillis()
                    }
                    DatePickerDialog(
                        context,
                        { _, year, month, day ->
                            TimePickerDialog(
                                context,
                                { _, hour, minute ->
                                    val newCal = Calendar.getInstance().apply {
                                        set(year, month, day, hour, minute, 0)
                                        set(Calendar.MILLISECOND, 0)
                                    }
                                    onUpdateDate(newCal.timeInMillis)
                                },
                                cal.get(Calendar.HOUR_OF_DAY),
                                cal.get(Calendar.MINUTE),
                                true,
                            ).show()
                        },
                        cal.get(Calendar.YEAR),
                        cal.get(Calendar.MONTH),
                        cal.get(Calendar.DAY_OF_MONTH),
                    ).show()
                },
            )

            Spacer(Modifier.height(12.dp))
            Text("Tags", fontSize = 12.sp, color = InfoLabelColor, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(6.dp))

            if (tags.isEmpty()) {
                TextButton(
                    onClick = { showTagDialog = true },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                ) {
                    Text("# Add tag", fontSize = 13.sp, color = InfoClickableColor)
                }
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    tags.forEach { tag ->
                        Text(
                            text = "# ${tag.name}",
                            fontSize = 13.sp,
                            color = InfoClickableColor,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                    IconButton(
                        onClick = { showTagDialog = true },
                        modifier = Modifier.size(28.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit tags",
                            tint = InfoLabelColor,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
            Spacer(Modifier.height(12.dp))

            InfoRow(label = "File", value = item.originalFileName)
            if (item.width != null && item.height != null) {
                InfoRow(label = "Dimensions", value = "${item.width} × ${item.height}")
            }
            val fileSize = remember(item.filePath) {
                val bytes = try { File(item.filePath).length() } catch (e: Exception) { 0L }
                when {
                    bytes >= 1_048_576 -> "%.1f MB".format(bytes / 1_048_576.0)
                    bytes >= 1024 -> "%.1f KB".format(bytes / 1024.0)
                    else -> "$bytes B"
                }
            }
            InfoRow(label = "Size", value = fileSize)
            if ((item.fileType == MediaType.VIDEO || item.fileType == MediaType.AUDIO) && item.durationMs != null) {
                InfoRow(label = "Duration", value = formatDurationMs(item.durationMs))
            }
        }

        IconButton(
            onClick = onDismiss,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close info panel",
                tint = Color.White,
            )
        }
    }

    if (showTagDialog) {
        TagDialog(
            tags = tags,
            onDismiss = { showTagDialog = false },
            onAddTag = onAddTag,
            onRemoveTag = onRemoveTag,
        )
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    clickable: Boolean = false,
    onClick: () -> Unit = {},
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = if (clickable) 6.dp else 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = InfoLabelColor,
            modifier = Modifier.width(80.dp),
        )
        Text(
            text = value,
            fontSize = 13.sp,
            color = if (clickable) InfoClickableColor else InfoValueColor,
            fontWeight = if (clickable) FontWeight.Medium else FontWeight.Normal,
            modifier = if (clickable) Modifier.clickable(onClick = onClick) else Modifier,
        )
    }
}

