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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.runtime.mutableIntStateOf
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

enum class CreativeGroup(val label: String, val icon: String) {
    SEARCH("All", "🔍"),
    CONSTRUCTION("Build", "🧱"),
    NATURE("Nature", "🌿"),
    ITEMS("Items", "💎")
}

/**
 * Authentic Bedrock Creative Inventory:
 * - Left side: Group Selector Tabs (Search/All, Construction, Nature, Items)
 * - Upper area: Live search bar and scrollable catalog of 3D isometric block cards
 * - Bottom area: Hand Inventory (9 Hotbar Slots) with active selection indicator
 * - Tapping any catalog block assigns it directly into the selected hand hotbar slot!
 */
@Composable
fun CreativeInventoryDialog(
    hotbarSlots: List<BlockType>,
    selectedHotbarIndex: Int,
    onSelectHotbarIndex: (Int) -> Unit,
    onAssignBlockToSlot: (Int, BlockType) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedGroup by remember { mutableStateOf(CreativeGroup.SEARCH) }
    var activeSlot by remember { mutableIntStateOf(selectedHotbarIndex) }

    val allBlocks = BlockType.CREATIVE_INVENTORY

    val filteredBlocks = remember(searchQuery, selectedGroup) {
        allBlocks.filter { block ->
            val matchesGroup = when (selectedGroup) {
                CreativeGroup.SEARCH -> true
                CreativeGroup.CONSTRUCTION -> block in listOf(
                    BlockType.STONE, BlockType.COBBLESTONE, BlockType.WOOD_PLANKS,
                    BlockType.BRICK, BlockType.BOOKSHELF, BlockType.GLASS, BlockType.BEDROCK
                )
                CreativeGroup.NATURE -> block in listOf(
                    BlockType.GRASS, BlockType.DIRT, BlockType.SAND,
                    BlockType.OAK_WOOD, BlockType.OAK_LEAVES, BlockType.WATER
                )
                CreativeGroup.ITEMS -> block in listOf(
                    BlockType.GLOWSTONE, BlockType.BOOKSHELF, BlockType.BRICK,
                    BlockType.GLASS, BlockType.WOOD_PLANKS
                )
            }
            val matchesSearch = searchQuery.isBlank() || block.displayName.contains(searchQuery.trim(), ignoreCase = true)
            matchesGroup && matchesSearch
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .testTag("creative_inventory_dialog"),
            shape = RoundedCornerShape(6.dp),
            color = Color(0xFFC6C6C6), // Classic Minecraft stone gray GUI
            border = androidx.compose.foundation.BorderStroke(3.dp, Color(0xFF373737))
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                // ==========================================
                // 1. LEFT SIDEBAR: GROUP SELECTOR TABS
                // ==========================================
                Column(
                    modifier = Modifier
                        .width(72.dp)
                        .fillMaxHeight()
                        .background(Color(0xFFB0B0B0))
                        .border(
                            width = 2.dp,
                            color = Color(0xFF373737),
                            shape = RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp)
                        )
                        .padding(vertical = 8.dp, horizontal = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (group in CreativeGroup.values()) {
                        val isSelected = selectedGroup == group
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isSelected) Color(0xFF373737) else Color(0xFF8B8B8B))
                                .padding(1.5.dp)
                                .background(if (isSelected) Color(0xFF4A6B3D) else Color(0xFFC6C6C6))
                                .clickable { selectedGroup = group }
                                .padding(2.dp)
                                .testTag("group_tab_${group.name.lowercase()}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = group.icon,
                                    fontSize = 17.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = group.label,
                                    fontSize = 9.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else Color(0xFF282828),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                // ==========================================
                // 2. MAIN CONTENT AREA
                // ==========================================
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(start = 10.dp, end = 10.dp, top = 8.dp, bottom = 8.dp)
                ) {
                    // Top Bar: Search Bar & Close Button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Search bar
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .height(34.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color(0xFF373737))
                                .padding(1.dp)
                                .background(Color(0xFF8B8B8B))
                                .padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = Color(0xFF222222),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(modifier = Modifier.weight(1f)) {
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        text = "Search blocks...",
                                        fontSize = 12.sp,
                                        color = Color(0xFF444444)
                                    )
                                }
                                BasicTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    singleLine = true,
                                    textStyle = TextStyle(
                                        color = Color.White,
                                        fontSize = 12.sp,
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

                        Spacer(modifier = Modifier.width(10.dp))

                        // Close [✕] Button
                        Box(
                            modifier = Modifier
                                .size(32.dp)
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
                                contentDescription = "Close",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Upper Area: Grid of Catalog Blocks
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
                                    text = "No blocks found in ${selectedGroup.label}",
                                    color = Color(0xFF373737),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(minSize = 72.dp),
                                contentPadding = PaddingValues(2.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(filteredBlocks) { block ->
                                    BedrockCatalogItem(
                                        block = block,
                                        onClick = {
                                            onAssignBlockToSlot(activeSlot, block)
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // ==========================================
                    // 3. BOTTOM AREA: HAND INVENTORY (HOTBAR)
                    // ==========================================
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFFAFAFAF))
                            .border(1.5.dp, Color(0xFF373737), RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Hand Inventory (Tap block to assign to slot #${activeSlot + 1})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF222222)
                            )
                            Text(
                                text = "Slot ${activeSlot + 1}/9",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E6325)
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // 9-slot Hotbar row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            for (index in 0 until 9) {
                                val block = hotbarSlots.getOrElse(index) { BlockType.AIR }
                                val isSelected = activeSlot == index

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(if (isSelected) Color(0xFF55FF55) else Color(0xFF373737))
                                        .padding(if (isSelected) 2.dp else 1.dp)
                                        .background(Color(0xFF8B8B8B))
                                        .clickable {
                                            activeSlot = index
                                            onSelectHotbarIndex(index)
                                        }
                                        .testTag("inventory_hotbar_slot_$index"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (block != BlockType.AIR) {
                                        IsometricBlockIcon(block = block, size = 32.dp)
                                    } else {
                                        Text(
                                            text = "${index + 1}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White.copy(alpha = 0.35f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BedrockCatalogItem(
    block: BlockType,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(68.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(Color(0xFF373737))
            .padding(1.dp)
            .background(Color(0xFF8B8B8B))
            .clickable(onClick = onClick)
            .padding(4.dp)
            .testTag("catalog_item_${block.name.lowercase()}"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            IsometricBlockIcon(block = block, size = 36.dp)
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = block.displayName,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White,
                maxLines = 1
            )
        }
    }
}
