package com.example.game.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.game.engine.BlockType

enum class InventoryCategory(val label: String) {
    ALL("All"),
    BUILDING("Building"),
    NATURAL("Natural"),
    UTILITY("Utility")
}

/**
 * Authentic Minecraft Bedrock Creative Inventory window:
 * Classical stone-gray container (#C6C6C6) with beveled borders, live search bar,
 * category tabs, and 3D isometric block icons.
 */
@Composable
fun CreativeInventoryDialog(
    selectedHotbarIndex: Int,
    onSelectBlock: (BlockType) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(InventoryCategory.ALL) }

    val allBlocks = BlockType.CREATIVE_INVENTORY

    val filteredBlocks = remember(searchQuery, selectedCategory) {
        allBlocks.filter { block ->
            val matchesCategory = when (selectedCategory) {
                InventoryCategory.ALL -> true
                InventoryCategory.BUILDING -> block in listOf(
                    BlockType.STONE, BlockType.COBBLESTONE, BlockType.WOOD_PLANKS,
                    BlockType.BRICK, BlockType.BOOKSHELF
                )
                InventoryCategory.NATURAL -> block in listOf(
                    BlockType.GRASS, BlockType.DIRT, BlockType.SAND,
                    BlockType.OAK_WOOD, BlockType.OAK_LEAVES, BlockType.WATER
                )
                InventoryCategory.UTILITY -> block in listOf(
                    BlockType.GLOWSTONE, BlockType.GLASS
                )
            }
            val matchesSearch = searchQuery.isBlank() || block.displayName.contains(searchQuery.trim(), ignoreCase = true)
            matchesCategory && matchesSearch
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .testTag("creative_inventory_dialog"),
            shape = RoundedCornerShape(6.dp),
            color = Color(0xFFC6C6C6), // Classic Minecraft stone gray GUI
            border = androidx.compose.foundation.BorderStroke(3.dp, Color(0xFF373737))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                // Header with Bedrock style
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Creative Inventory",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF282828)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color(0xFF4A4A4A))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Slot #${selectedHotbarIndex + 1}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF68D391)
                            )
                        }
                    }

                    // Bedrock styled close button
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color(0xFF373737))
                            .padding(1.dp)
                            .background(Color(0xFF8B8B8B))
                            .clickable(onClick = onDismiss)
                            .testTag("close_inventory_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Inventory",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Search Bar in Minecraft inset-stone style
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color(0xFF373737))
                        .padding(1.5.dp)
                        .background(Color(0xFF8B8B8B))
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = Color(0xFF373737),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "Search blocks...",
                                fontSize = 13.sp,
                                color = Color(0xFF4A4A4A)
                            )
                        }
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            singleLine = true,
                            textStyle = TextStyle(
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                fontFamily = FontFamily.SansSerif
                            ),
                            cursorBrush = SolidColor(Color.White),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("inventory_search_input")
                        )
                    }
                    if (searchQuery.isNotEmpty()) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear search",
                            tint = Color.White,
                            modifier = Modifier
                                .size(16.dp)
                                .clickable { searchQuery = "" }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Category Tabs
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(InventoryCategory.values()) { cat ->
                        val isSelected = (selectedCategory == cat)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(3.dp))
                                .background(if (isSelected) Color(0xFF373737) else Color(0xFF7A7A7A))
                                .padding(1.dp)
                                .background(if (isSelected) Color(0xFF4A6B3D) else Color(0xFF8B8B8B))
                                .clickable { selectedCategory = cat }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                                .testTag("category_tab_${cat.name.lowercase()}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = cat.label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else Color(0xFF282828)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Inner beveled slot panel with 3D blocks
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF8B8B8B))
                        .border(2.dp, Color(0xFF373737), RoundedCornerShape(4.dp))
                        .padding(6.dp)
                ) {
                    if (filteredBlocks.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No blocks matching \"$searchQuery\"",
                                color = Color(0xFF373737),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 78.dp),
                            contentPadding = PaddingValues(2.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(filteredBlocks) { block ->
                                BedrockInventorySlot(
                                    block = block,
                                    onClick = {
                                        onSelectBlock(block)
                                        onDismiss()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Single Minecraft Bedrock item slot with inset borders and 3D Isometric block icon
 */
@Composable
private fun BedrockInventorySlot(
    block: BlockType,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(82.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(Color(0xFF373737)) // Dark outer shadow
            .padding(1.5.dp)
            .background(Color(0xFF8F8F8F)) // Stone slot interior
            .clickable(onClick = onClick)
            .testTag("block_card_${block.name.lowercase()}"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(3.dp)
        ) {
            // Authentic 3D Isometric Voxel Cube
            IsometricBlockIcon(block = block, size = 36.dp)

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = block.displayName,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1
            )
        }
    }
}
