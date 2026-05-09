package com.flutter.stepstonesflt.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import com.flutter.stepstonesflt.data.local.entity.Tag
import kotlinx.coroutines.delay

private val TagInColor = Color(0xFF86D6BF)
private val TagOutColor = Color(0xFFADADAD)
private val TagOutTextColor = Color(0xFF292929)
private val TagDialogBg = Color(0xFF1E1E1E)

private data class TagEntry(
    val tag: Tag?,
    val name: String,   // lowercase-normalised
    val isIn: Boolean,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagDialog(
    tags: List<Tag>,
    onDismiss: () -> Unit,
    onAddTag: (String) -> Unit,
    onRemoveTag: (Tag) -> Unit,
) {
    var shakeCount by remember { mutableStateOf(0) }
    var isShaking by remember { mutableStateOf(false) }
    val shakeOffset = remember { Animatable(0f) }

    LaunchedEffect(shakeCount) {
        if (shakeCount > 0) {
            isShaking = true
            for (target in listOf(10f, -10f, 7f, -7f, 3f, 0f)) {
                shakeOffset.animateTo(target, animationSpec = tween(40, easing = LinearEasing))
            }
            isShaking = false
        }
    }

    var entries by remember {
        mutableStateOf(tags.map { TagEntry(tag = it, name = it.name.lowercase(), isIn = true) })
    }
    var tagInput by remember { mutableStateOf("") }
    var flashingName by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(flashingName) {
        if (flashingName != null) {
            delay(400L)
            flashingName = null
        }
    }

    fun addTag(input: String) {
        val normalized = input.trim().lowercase()
        if (normalized.isEmpty()) return
        val existing = entries.find { it.name == normalized }
        when {
            existing == null -> entries = entries + TagEntry(null, normalized, true)
            !existing.isIn -> entries = entries.map {
                if (it.name == normalized) it.copy(isIn = true) else it
            }
            else -> flashingName = normalized
        }
        tagInput = ""
    }

    fun save() {
        entries.filter { it.tag != null && !it.isIn }.forEach { onRemoveTag(it.tag!!) }
        entries.filter { it.tag == null && it.isIn }.forEach { onAddTag(it.name) }
        onDismiss()
    }

    val view = LocalView.current
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        SideEffect {
            val dialogWindow = (view.parent as? DialogWindowProvider)?.window ?: return@SideEffect
            WindowCompat.setDecorFitsSystemWindows(dialogWindow, false)
            dialogWindow.setSoftInputMode(
                android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .imePadding(),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                shape = RoundedCornerShape(12.dp),
                color = TagDialogBg,
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Title row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Tags",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                        )
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = ::save) {
                            Text("Save", color = TagInColor, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // Tag chips area
                    if (entries.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 200.dp)
                                .verticalScroll(rememberScrollState()),
                        ) {
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                entries.forEach { entry ->
                                    val isActiveIn = entry.isIn && entry.name != flashingName
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(if (isActiveIn) TagInColor else TagOutColor)
                                            .clickable {
                                                entries = entries.map {
                                                    if (it.name == entry.name) it.copy(isIn = !it.isIn) else it
                                                }
                                            }
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                    ) {
                                        Text(
                                            text = entry.name,
                                            fontSize = 12.sp,
                                            color = if (isActiveIn) Color(0xFF1A1A1A) else TagOutTextColor,
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                    }

                    // Text field + plus button
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = tagInput,
                            onValueChange = { new ->
                                if (new.all { it.isLetterOrDigit() }) {
                                    tagInput = new
                                } else {
                                    shakeCount++
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .graphicsLayer { translationX = shakeOffset.value },
                            isError = isShaking,
                            placeholder = { Text("Add tag", fontSize = 13.sp) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { addTag(tagInput) }),
                        )
                        AnimatedVisibility(visible = tagInput.isNotEmpty()) {
                            IconButton(
                                onClick = { addTag(tagInput) },
                                modifier = Modifier
                                    .padding(start = 8.dp)
                                    .background(TagInColor, CircleShape),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add tag",
                                    tint = Color(0xFF1A1A1A),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
