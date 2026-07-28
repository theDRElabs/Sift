package com.example

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.ItemType
import com.example.ui.SiftViewModel
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ShareReceiverActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val action = intent.action
        val mimeType = intent.type

        var sharedText = ""
        if (Intent.ACTION_SEND == action && "text/plain" == mimeType) {
            val subject = intent.getStringExtra(Intent.EXTRA_SUBJECT)
            val text = intent.getStringExtra(Intent.EXTRA_TEXT)
            sharedText = buildString {
                if (!subject.isNullOrBlank()) {
                    append(subject)
                    append("\n")
                }
                if (!text.isNullOrBlank()) {
                    append(text)
                }
            }
        }

        setContent {
            MyApplicationTheme {
                val viewModel: SiftViewModel = viewModel()
                var type by remember { mutableStateOf(ItemType.INFO) }
                var content by remember { mutableStateOf(sharedText) }
                var dueAt by remember { mutableStateOf<Long?>(null) }
                var isSaving by remember { mutableStateOf(false) }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .clickable(enabled = !isSaving) { finish() },
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .clickable { }, // Prevent outside click from closing when clicking inside
                        shape = RoundedCornerShape(16.dp),
                        color = SurfaceDark,
                        contentColor = OnSurfaceDark
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth()
                        ) {
                            Text(
                                text = "Save to Sift",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium),
                                color = OnSurfaceDark
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            SingleChoiceSegmentedButtonRow(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                ItemType.entries.forEachIndexed { index, itemType ->
                                    SegmentedButton(
                                        selected = type == itemType,
                                        onClick = { type = itemType },
                                        shape = SegmentedButtonDefaults.itemShape(
                                            index = index,
                                            count = ItemType.entries.size
                                        ),
                                        colors = SegmentedButtonDefaults.colors(
                                            activeContainerColor = SecondaryDark,
                                            activeContentColor = OnSecondaryDark,
                                            inactiveContainerColor = Color.Transparent,
                                            inactiveContentColor = OnSurfaceVariantDark
                                        )
                                    ) {
                                        Text(itemType.name.lowercase().replaceFirstChar { it.uppercase() })
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            OutlinedTextField(
                                value = content,
                                onValueChange = { content = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 100.dp, max = 200.dp),
                                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                                textStyle = MaterialTheme.typography.bodyLarge,
                                placeholder = { Text("Content") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryDark,
                                    focusedLabelColor = PrimaryDark,
                                    unfocusedBorderColor = OutlineDark,
                                    cursorColor = PrimaryDark
                                )
                            )

                            if (type == ItemType.TODO) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = if (dueAt != null) {
                                            "Due: " + SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(dueAt!!))
                                        } else {
                                            "No Due Date"
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = OnSurfaceDark
                                    )
                                    
                                    Button(
                                        onClick = {
                                            val calendar = Calendar.getInstance()
                                            if (dueAt != null) {
                                                calendar.timeInMillis = dueAt!!
                                            }
                                            
                                            DatePickerDialog(
                                                this@ShareReceiverActivity,
                                                { _, year, month, dayOfMonth ->
                                                    TimePickerDialog(
                                                        this@ShareReceiverActivity,
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
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                        modifier = Modifier.height(32.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = PrimaryDark,
                                            contentColor = OnPrimaryDark
                                        )
                                    ) {
                                        Text("Set Date", style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(
                                    onClick = { finish() },
                                    enabled = !isSaving,
                                    colors = ButtonDefaults.textButtonColors(
                                        contentColor = PrimaryDark
                                    )
                                ) {
                                    Text("Cancel")
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        if (content.isNotBlank()) {
                                            isSaving = true
                                            lifecycleScope.launch {
                                                viewModel.saveItem(0, type, content, dueAt, false).join()
                                                finish()
                                            }
                                        }
                                    },
                                    enabled = content.isNotBlank() && !isSaving,
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
            }
        }
    }
}
