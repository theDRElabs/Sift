package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Item
import com.example.data.ItemType
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: SiftViewModel,
    onNavigateToCapture: (Int) -> Unit
) {
    val items by viewModel.uiState.collectAsStateWithLifecycle()
    val currentFilter by viewModel.filter.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sift", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Medium)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onNavigateToCapture(0) },
                modifier = Modifier.testTag("add_item_fab"),
                containerColor = PrimaryDark,
                contentColor = OnPrimaryDark,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Item")
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = BottomNavBg,
                contentColor = OnSurfaceVariantDark,
                modifier = Modifier.border(width = 1.dp, color = Color.White.copy(alpha = 0.05f))
            ) {
                NavigationBarItem(
                    selected = true,
                    onClick = { },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Home", fontSize = 10.sp, fontWeight = FontWeight.Medium) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PrimaryDark,
                        selectedTextColor = PrimaryDark,
                        indicatorColor = Color.Transparent,
                        unselectedIconColor = OnSurfaceVariantDark,
                        unselectedTextColor = OnSurfaceVariantDark
                    )
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { },
                    icon = { Icon(Icons.Default.Lock, contentDescription = "Vault") },
                    label = { Text("Vault", fontSize = 10.sp, fontWeight = FontWeight.Medium) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PrimaryDark,
                        selectedTextColor = PrimaryDark,
                        indicatorColor = Color.Transparent,
                        unselectedIconColor = OnSurfaceVariantDark,
                        unselectedTextColor = OnSurfaceVariantDark
                    )
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings", fontSize = 10.sp, fontWeight = FontWeight.Medium) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PrimaryDark,
                        selectedTextColor = PrimaryDark,
                        indicatorColor = Color.Transparent,
                        unselectedIconColor = OnSurfaceVariantDark,
                        unselectedTextColor = OnSurfaceVariantDark
                    )
                )
            }
        },
        contentWindowInsets = WindowInsets.systemBars
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = currentFilter == null,
                        onClick = { viewModel.setFilter(null) },
                        label = { Text("All", fontWeight = FontWeight.Medium) },
                        shape = RoundedCornerShape(percent = 50),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SecondaryDark,
                            selectedLabelColor = OnSecondaryDark,
                            containerColor = Color.Transparent,
                            labelColor = OnSurfaceVariantDark
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = currentFilter == null,
                            borderColor = OutlineDark
                        )
                    )
                }
                items(ItemType.entries) { type ->
                    FilterChip(
                        selected = currentFilter == type,
                        onClick = { viewModel.setFilter(type) },
                        label = { Text(type.name.lowercase().replaceFirstChar { it.uppercase() }, fontWeight = FontWeight.Medium) },
                        shape = RoundedCornerShape(percent = 50),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SecondaryDark,
                            selectedLabelColor = OnSecondaryDark,
                            containerColor = Color.Transparent,
                            labelColor = OnSurfaceVariantDark
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = currentFilter == type,
                            borderColor = OutlineDark
                        )
                    )
                }
            }

            if (items.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No items found. Tap + to add one.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(items, key = { it.id }) { item ->
                        ItemCard(
                            item = item,
                            onClick = { onNavigateToCapture(item.id) },
                            onToggleDone = { viewModel.toggleDone(item) },
                            onDelete = { viewModel.deleteItem(item) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ItemCard(
    item: Item,
    onClick: () -> Unit,
    onToggleDone: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .alpha(if (item.isDone) 0.6f else 1f),
        colors = CardDefaults.cardColors(
            containerColor = SurfaceDark
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (item.type == ItemType.TODO) {
                Checkbox(
                    checked = item.isDone,
                    onCheckedChange = { onToggleDone() },
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
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
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = item.type.name,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                fontSize = 10.sp
                            ),
                            color = badgeText
                        )
                    }

                    if (item.type == ItemType.TODO && item.dueAt != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        val dateStr = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(item.dueAt))
                        Text(
                            text = "Due: $dateStr",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = if (item.dueAt < System.currentTimeMillis() && !item.isDone) MaterialTheme.colorScheme.error else OnSurfaceVariantDark
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                LinkifyText(
                    text = item.content,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        textDecoration = if (item.isDone) TextDecoration.LineThrough else TextDecoration.None
                    ),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}
