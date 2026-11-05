package com.example.game.engine

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class WorldMetadata(
    val id: String,
    val name: String,
    val type: String, // "infinite" or "flat"
    val seed: Long,
    val lastPlayed: Long,
    val createdAt: Long
)

data class WorldSaveData(
    val metadata: WorldMetadata,
    val playerX: Float,
    val playerY: Float,
    val playerZ: Float,
    val playerYaw: Float,
    val playerPitch: Float,
    val hotbar: List<BlockType>,
    val blockDeltas: Map<String, Byte> // "x,y,z" -> blockTypeId
)

/**
 * Fast, lightweight local JSON World Save and Auto-Save Manager for NovaCraft.
 * Persists world configurations, player positions, hotbar setups, and block modifications.
 */
object WorldSaveManager {

    private const val WORLDS_DIR = "worlds"
    private const val INDEX_FILE = "worlds_index.json"

    private fun getWorldsFolder(context: Context): File {
        val dir = File(context.filesDir, WORLDS_DIR)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    /**
     * Lists all existing saved worlds sorted by last played (most recent first).
     */
    fun listWorlds(context: Context): List<WorldMetadata> {
        val folder = getWorldsFolder(context)
        val indexFile = File(folder, INDEX_FILE)
        if (!indexFile.exists()) {
            // Seed a default initial world if empty
            val defaultWorld = createNewWorld(context, "My World", "infinite", 424242L)
            return listOf(defaultWorld.metadata)
        }

        val list = mutableListOf<WorldMetadata>()
        try {
            val jsonStr = indexFile.readText()
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    WorldMetadata(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        type = obj.optString("type", "infinite"),
                        seed = obj.getLong("seed"),
                        lastPlayed = obj.getLong("lastPlayed"),
                        createdAt = obj.optLong("createdAt", obj.getLong("lastPlayed"))
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list.sortedByDescending { it.lastPlayed }
    }

    private fun updateIndex(context: Context, worlds: List<WorldMetadata>) {
        val folder = getWorldsFolder(context)
        val indexFile = File(folder, INDEX_FILE)
        val array = JSONArray()
        for (w in worlds) {
            val obj = JSONObject().apply {
                put("id", w.id)
                put("name", w.name)
                put("type", w.type)
                put("seed", w.seed)
                put("lastPlayed", w.lastPlayed)
                put("createdAt", w.createdAt)
            }
            array.put(obj)
        }
        indexFile.writeText(array.toString())
    }

    /**
     * Creates a new world with specified parameters and writes initial save file.
     */
    fun createNewWorld(
        context: Context,
        name: String,
        type: String,
        seed: Long
    ): WorldSaveData {
        val now = System.currentTimeMillis()
        val id = "world_${now}_${(1000..9999).random()}"
        val cleanName = if (name.isBlank()) "New World" else name.trim()

        val meta = WorldMetadata(
            id = id,
            name = cleanName,
            type = if (type.equals("flat", ignoreCase = true)) "flat" else "infinite",
            seed = seed,
            lastPlayed = now,
            createdAt = now
        )

        // Initial spawn coordinates:
        // For flat: surface is Y=3, player stands at Y=4
        // For infinite: around Y=36
        val spawnY = if (meta.type == "flat") 4.5f else 36.0f

        val initialData = WorldSaveData(
            metadata = meta,
            playerX = 0f,
            playerY = spawnY,
            playerZ = 0f,
            playerYaw = 0f,
            playerPitch = 0f,
            hotbar = List(9) { BlockType.AIR },
            blockDeltas = emptyMap()
        )

        saveWorldData(context, initialData)

        val existing = listWorldsRaw(context).toMutableList()
        existing.removeAll { it.id == id }
        existing.add(0, meta)
        updateIndex(context, existing)

        return initialData
    }

    private fun listWorldsRaw(context: Context): List<WorldMetadata> {
        val folder = getWorldsFolder(context)
        val indexFile = File(folder, INDEX_FILE)
        if (!indexFile.exists()) return emptyList()

        val list = mutableListOf<WorldMetadata>()
        try {
            val jsonStr = indexFile.readText()
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    WorldMetadata(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        type = obj.optString("type", "infinite"),
                        seed = obj.getLong("seed"),
                        lastPlayed = obj.getLong("lastPlayed"),
                        createdAt = obj.optLong("createdAt", obj.getLong("lastPlayed"))
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }

    /**
     * Loads full world data by ID.
     */
    fun loadWorldData(context: Context, worldId: String): WorldSaveData? {
        val folder = getWorldsFolder(context)
        val saveFile = File(folder, "$worldId.json")
        if (!saveFile.exists()) return null

        return try {
            val root = JSONObject(saveFile.readText())
            val metaObj = root.getJSONObject("metadata")
            val meta = WorldMetadata(
                id = metaObj.getString("id"),
                name = metaObj.getString("name"),
                type = metaObj.optString("type", "infinite"),
                seed = metaObj.getLong("seed"),
                lastPlayed = metaObj.getLong("lastPlayed"),
                createdAt = metaObj.optLong("createdAt", metaObj.getLong("lastPlayed"))
            )

            val pObj = root.optJSONObject("player")
            val px = pObj?.optDouble("x", 0.0)?.toFloat() ?: 0f
            val py = pObj?.optDouble("y", 36.0)?.toFloat() ?: 36f
            val pz = pObj?.optDouble("z", 0.0)?.toFloat() ?: 0f
            val pyaw = pObj?.optDouble("yaw", 0.0)?.toFloat() ?: 0f
            val ppitch = pObj?.optDouble("pitch", 0.0)?.toFloat() ?: 0f

            val hotbarList = mutableListOf<BlockType>()
            val hotbarArr = root.optJSONArray("hotbar")
            if (hotbarArr != null) {
                for (i in 0 until hotbarArr.length()) {
                    val name = hotbarArr.getString(i)
                    hotbarList.add(try { BlockType.valueOf(name) } catch (_: Exception) { BlockType.AIR })
                }
            }
            while (hotbarList.size < 9) hotbarList.add(BlockType.AIR)

            val deltas = mutableMapOf<String, Byte>()
            val deltasObj = root.optJSONObject("deltas")
            if (deltasObj != null) {
                val keys = deltasObj.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    deltas[key] = deltasObj.getInt(key).toByte()
                }
            }

            WorldSaveData(
                metadata = meta,
                playerX = px,
                playerY = py,
                playerZ = pz,
                playerYaw = pyaw,
                playerPitch = ppitch,
                hotbar = hotbarList,
                blockDeltas = deltas
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Saves full world data including player status and placed/broken block deltas.
     */
    fun saveWorldData(context: Context, data: WorldSaveData) {
        val folder = getWorldsFolder(context)
        val saveFile = File(folder, "${data.metadata.id}.json")

        try {
            val root = JSONObject()

            val updatedMeta = data.metadata.copy(lastPlayed = System.currentTimeMillis())
            val metaObj = JSONObject().apply {
                put("id", updatedMeta.id)
                put("name", updatedMeta.name)
                put("type", updatedMeta.type)
                put("seed", updatedMeta.seed)
                put("lastPlayed", updatedMeta.lastPlayed)
                put("createdAt", updatedMeta.createdAt)
            }
            root.put("metadata", metaObj)

            val pObj = JSONObject().apply {
                put("x", data.playerX.toDouble())
                put("y", data.playerY.toDouble())
                put("z", data.playerZ.toDouble())
                put("yaw", data.playerYaw.toDouble())
                put("pitch", data.playerPitch.toDouble())
            }
            root.put("player", pObj)

            val hotbarArr = JSONArray()
            for (b in data.hotbar) {
                hotbarArr.put(b.name)
            }
            root.put("hotbar", hotbarArr)

            val deltasObj = JSONObject()
            for ((key, byteVal) in data.blockDeltas) {
                deltasObj.put(key, byteVal.toInt())
            }
            root.put("deltas", deltasObj)

            saveFile.writeText(root.toString())

            // Update index
            val existing = listWorldsRaw(context).toMutableList()
            existing.removeAll { it.id == updatedMeta.id }
            existing.add(0, updatedMeta)
            updateIndex(context, existing)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Deletes a world save file and removes from index.
     */
    fun deleteWorld(context: Context, worldId: String) {
        val folder = getWorldsFolder(context)
        val saveFile = File(folder, "$worldId.json")
        if (saveFile.exists()) saveFile.delete()

        val existing = listWorldsRaw(context).toMutableList()
        existing.removeAll { it.id == worldId }
        updateIndex(context, existing)
    }
}
