package com.flutter.stepstonesflt.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.flutter.stepstonesflt.data.local.entity.Album

@Composable
fun AlbumSelector(
    albums: List<Album>,
    selectedAlbum: Album?,
    onAlbumSelected: (Album) -> Unit,
    onCreateAlbum: (String) -> Unit,
    onRenameAlbum: (Album, String) -> Unit,
    onDeleteAlbum: (Album) -> Unit,
    modifier: Modifier = Modifier,
    trigger: @Composable (() -> Unit)? = null,
) {
    var expanded by remember { mutableStateOf(false) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var albumToRename by remember { mutableStateOf<Album?>(null) }
    var albumToDelete by remember { mutableStateOf<Album?>(null) }

    Box(modifier = modifier) {
        Box(modifier = Modifier.clickable { expanded = true }) {
            trigger?.invoke()
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            albums.forEach { album ->
                AlbumDropdownItem(
                    album = album,
                    isSelected = album.id == selectedAlbum?.id,
                    onSelect = { onAlbumSelected(album); expanded = false },
                    onRename = if (!album.isDefault) {
                        { albumToRename = album; expanded = false }
                    } else null,
                    onDelete = if (!album.isDefault) {
                        { albumToDelete = album; expanded = false }
                    } else null,
                )
            }

            HorizontalDivider()

            DropdownMenuItem(
                text = { Text("Create album") },
                leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
                onClick = { showCreateDialog = true; expanded = false },
            )
        }
    }

    if (showCreateDialog) {
        AlbumNameDialog(
            title = "Create album",
            initialName = "",
            onConfirm = { onCreateAlbum(it); showCreateDialog = false },
            onDismiss = { showCreateDialog = false },
        )
    }

    albumToRename?.let { album ->
        AlbumNameDialog(
            title = "Rename",
            initialName = album.name,
            onConfirm = { onRenameAlbum(album, it); albumToRename = null },
            onDismiss = { albumToRename = null },
        )
    }

    albumToDelete?.let { album ->
        AlertDialog(
            onDismissRequest = { albumToDelete = null },
            title = { Text("Delete album") },
            text = { Text("Delete \"${album.name}\"? All media in this album will be permanently deleted.") },
            confirmButton = {
                TextButton(onClick = { onDeleteAlbum(album); albumToDelete = null }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { albumToDelete = null }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun AlbumDropdownItem(
    album: Album,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onRename: (() -> Unit)?,
    onDelete: (() -> Unit)?,
) {
    var subExpanded by remember { mutableStateOf(false) }

    DropdownMenuItem(
        text = { Text(album.name) },
        onClick = onSelect,
        leadingIcon = {
            if (isSelected) {
                Icon(Icons.Default.Check, contentDescription = null)
            } else {
                Spacer(Modifier.size(24.dp))
            }
        },
        trailingIcon = if (onRename != null || onDelete != null) {
            {
                Box {
                    IconButton(onClick = { subExpanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Album options")
                    }
                    DropdownMenu(
                        expanded = subExpanded,
                        onDismissRequest = { subExpanded = false },
                    ) {
                        onRename?.let {
                            DropdownMenuItem(
                                text = { Text("Rename") },
                                onClick = { subExpanded = false; it() },
                            )
                        }
                        onDelete?.let {
                            DropdownMenuItem(
                                text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                                onClick = { subExpanded = false; it() },
                            )
                        }
                    }
                }
            }
        } else null,
    )
}

@Composable
private fun AlbumNameDialog(
    title: String,
    initialName: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Album name") },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onConfirm(name.trim()) },
                enabled = name.isNotBlank(),
            ) { Text("Confirm") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
