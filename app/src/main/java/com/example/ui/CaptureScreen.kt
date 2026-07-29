package com.example.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Item
import com.example.data.ItemType
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaptureScreen(
    itemId: Int,
    viewModel: SiftViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var isEditing by remember { mutableStateOf(itemId == 0) }
    var currentItem by remember { mutableStateOf<Item?>(null) }
    
    var type by remember { mutableStateOf(ItemType.IDEA) }
    var content by remember { mutableStateOf("") }
    var dueAt by remember { mutableStateOf<Long?>(null) }
    var isDone by remember { mutableStateOf(false) }

    LaunchedEffect(itemId) {
        if (itemId != 0) {
            val item = viewModel.getItemById(itemId)
            currentItem = item
            if (item != null) {
                type = item.type
                content = item.content
                dueAt = item.dueAt
                isDone = item.isDone
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (itemId == 0) "Capture" else if (isEditing) "Edit Item" else "View Item") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (isEditing) {
                        IconButton(onClick = {
                            if (content.isNotBlank()) {
                                viewModel.saveItem(itemId, type, content, dueAt, isDone)
                                onNavigateBack()
                            }
                        }, modifier = Modifier.testTag("save_button")) {
                            Icon(Icons.Default.Check, contentDescription = "Save")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = androidx.compose.ui.graphics.Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        contentWindowInsets = WindowInsets.systemBars
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            if (!isEditing && currentItem != null) {
                // VIEW MODE
                val item = currentItem!!
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val (badgeBg, badgeText) = when {
                        item.isDone -> DoneBadgeBg to DoneBadgeText
                        item.type == ItemType.IDEA -> IdeaBadgeBg to IdeaBadgeText
                        item.type == ItemType.INFO -> InfoBadgeBg to InfoBadgeText
                        item.type == ItemType.TODO -> TodoBadgeBg to TodoBadgeText
                        else -> DoneBadgeBg to DoneBadgeText
                    }
                    
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(badgeBg)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = item.type.name,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            ),
                            color = badgeText
                        )
                    }
                    
                    Row {
                        if (item.type == ItemType.TODO) {
                            Checkbox(
                                checked = isDone,
                                onCheckedChange = { 
                                    isDone = it
                                    val updatedItem = item.copy(isDone = it)
                                    currentItem = updatedItem
                                    viewModel.toggleDone(updatedItem) 
                                }
                            )
                        }
                        IconButton(onClick = { isEditing = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = OnSurfaceVariantDark)
                        }
                        IconButton(onClick = {
                            viewModel.deleteItem(item)
                            onNavigateBack()
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Created: " + SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault()).format(Date(item.createdAt)),
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceVariantDark
                )
                
                if (item.type == ItemType.TODO && dueAt != null) {
                    Text(
                        text = "Due: " + SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault()).format(Date(dueAt!!)),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (dueAt!! < System.currentTimeMillis() && !isDone) MaterialTheme.colorScheme.error else OnSurfaceVariantDark
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                LinkifyText(
                    text = content,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None
                    )
                )

            } else {
                // EDIT MODE
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ItemType.entries.forEachIndexed { index, itemType ->
                        SegmentedButton(
                            selected = type == itemType,
                            onClick = { type = itemType },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = ItemType.entries.size),
                            colors = SegmentedButtonDefaults.colors(
                                activeContainerColor = SecondaryDark,
                                activeContentColor = OnSecondaryDark,
                                inactiveContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                                inactiveContentColor = OnSurfaceVariantDark
                            )
                        ) {
                            Text(itemType.name.lowercase().replaceFirstChar { it.uppercase() })
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("What's on your mind?") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 150.dp)
                        .testTag("content_input"),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    textStyle = MaterialTheme.typography.bodyLarge,
                    placeholder = { Text("Enter your idea, info, or to-do here...") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryDark,
                        focusedLabelColor = PrimaryDark,
                        unfocusedBorderColor = OutlineDark,
                        cursorColor = PrimaryDark
                    )
                )

                if (type == ItemType.TODO) {
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (dueAt != null) {
                                "Due: " + SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault()).format(Date(dueAt!!))
                            } else {
                                "No Due Date"
                            },
                            style = MaterialTheme.typography.bodyLarge
                        )
                        
                        Button(
                            onClick = {
                                val calendar = Calendar.getInstance()
                                if (dueAt != null) {
                                    calendar.timeInMillis = dueAt!!
                                }
                                
                                DatePickerDialog(
                                    context,
                                    { _, year, month, dayOfMonth ->
                                        TimePickerDialog(
                                            context,
                                            { _, hourOfDay, minute ->
                                                val newDate = Calendar.getInstance().apply {
                                                    set(year, month, dayOfMonth, hourOfDay, minute, 0)
                                                }
                                                dueAt = newDate.timeInMillis
                                            },
                                            calendar.get(Calendar.HOUR_OF_DAY),
                                            calendar.get(Calendar.MINUTE),
                                            true
                                        ).show()
                                    },
                                    calendar.get(Calendar.YEAR),
                                    calendar.get(Calendar.MONTH),
                                    calendar.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PrimaryDark,
                                contentColor = OnPrimaryDark
                            )
                        ) {
                            Text("Set Date")
                        }
                    }
                    
                    if (dueAt != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(onClick = { dueAt = null }) {
                            Text("Clear Due Date", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = {
                        if (content.isNotBlank()) {
                            viewModel.saveItem(itemId, type, content, dueAt, isDone)
                            onNavigateBack()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("submit_button"),
                    enabled = content.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryDark,
                        contentColor = OnPrimaryDark
                    )
                ) {
                    Text("Save")
                }
            }
        }
    }
}

