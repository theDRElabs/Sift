package com.example.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.example.data.ItemType
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
    var type by remember { mutableStateOf(ItemType.IDEA) }
    var content by remember { mutableStateOf("") }
    var dueAt by remember { mutableStateOf<Long?>(null) }
    var isDone by remember { mutableStateOf(false) }

    LaunchedEffect(itemId) {
        if (itemId != 0) {
            val item = viewModel.getItemById(itemId)
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
                title = { Text(if (itemId == 0) "Capture" else "Edit Item") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        if (content.isNotBlank()) {
                            viewModel.saveItem(itemId, type, content, dueAt, isDone)
                            onNavigateBack()
                        }
                    }, modifier = Modifier.testTag("save_button")) {
                        Icon(Icons.Default.Check, contentDescription = "Save")
                    }
                }
            )
        },
        contentWindowInsets = WindowInsets.systemBars
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth()
            ) {
                ItemType.entries.forEachIndexed { index, itemType ->
                    SegmentedButton(
                        selected = type == itemType,
                        onClick = { type = itemType },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = ItemType.entries.size)
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
                    .weight(1f)
                    .testTag("content_input"),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                textStyle = MaterialTheme.typography.bodyLarge,
                placeholder = { Text("Enter your idea, info, or to-do here...") }
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
                    
                    Button(onClick = {
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
                    }) {
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
                enabled = content.isNotBlank()
            ) {
                Text("Save")
            }
        }
    }
}
