package com.retrivedmods.wclient.game

import android.content.Context
import android.net.Uri
import android.os.Environment
import com.retrivedmods.wclient.application.AppContext
import com.retrivedmods.wclient.game.module.combat.ACAModule
import com.retrivedmods.wclient.game.module.combat.AntiCrystalModule
import com.retrivedmods.wclient.game.module.combat.TriggerBotModule
import com.retrivedmods.wclient.game.module.combat.AutoTotemModule
import com.retrivedmods.wclient.game.module.misc.CommandHandlerModule
import com.retrivedmods.wclient.game.module.visual.CoordinatesModule
import com.retrivedmods.wclient.game.module.misc.MinerModule
import com.retrivedmods.wclient.game.module.world.AntiDebuffModule
import com.retrivedmods.wclient.game.module.world.EffectsModule
import com.retrivedmods.wclient.game.module.motion.SprintModule
import com.retrivedmods.wclient.game.module.visual.FullbrightModule
import com.retrivedmods.wclient.game.module.visual.NetworkInfoModule
import com.retrivedmods.wclient.game.module.visual.NoHurtCameraModule
import com.retrivedmods.wclient.game.module.visual.SpeedDisplayModule
import com.retrivedmods.wclient.game.module.visual.WorldStateModule
import com.retrivedmods.wclient.game.module.world.FreeCameraModule
import com.retrivedmods.wclient.game.module.motion.SpeedModule
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import java.io.File

object ModuleManager {

    private val _modules: MutableList<Module> = ArrayList()

    val modules: List<Module> = _modules

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    init {
        with(_modules) {
            // Combat
            add(ACAModule())
            add(AutoTotemModule())
            add(AntiCrystalModule())
            add(TriggerBotModule())

            // Motion           
            add(SpeedModule())
            add(SprintModule())
           
            // Visual            
            add(CoordinatesModule())
            add(NoHurtCameraModule())
            add(SpeedDisplayModule())
            add(NetworkInfoModule())
            add(WorldStateModule())
            add(FullbrightModule())

            // World
            add(FreeCameraModule())
            add(EffectsModule())
            add(AntiDebuffModule())

            // Misc          
            add(CommandHandlerModule())
            add(MinerModule())
        }
    }

    fun saveConfig() {

        if (!AppContext.isInitialized) {
            return
        }

        val configsDir = AppContext.instance.filesDir.resolve("configs")
        configsDir.mkdirs()

        val config = configsDir.resolve("UserConfig.json")
        val jsonObject = buildJsonObject {
            put("modules", buildJsonObject {
                _modules.forEach {
                    if (it.private) {
                        return@forEach
                    }
                    put(it.name, it.toJson())
                }
            })
        }

        config.writeText(json.encodeToString(JsonObject.serializer(), jsonObject))
    }

    fun loadConfig() {

        if (!AppContext.isInitialized) {
            return
        }

        val configsDir = AppContext.instance.filesDir.resolve("configs")
        configsDir.mkdirs()

        val config = configsDir.resolve("UserConfig.json")
        if (!config.exists()) {
            return
        }

        val jsonString = config.readText()
        if (jsonString.isEmpty()) {
            return
        }

        try {
            val jsonObject = json.parseToJsonElement(jsonString).jsonObject
            val modules = jsonObject["modules"]?.jsonObject ?: return

            _modules.forEach { module ->
                (modules[module.name] as? JsonObject)?.let {
                    module.fromJson(it)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun exportConfig(): String {
        val jsonObject = buildJsonObject {
            put("modules", buildJsonObject {
                _modules.forEach {
                    if (it.private) {
                        return@forEach
                    }
                    put(it.name, it.toJson())
                }
            })
        }
        return json.encodeToString(JsonObject.serializer(), jsonObject)
    }

    fun importConfig(configStr: String) {
        try {
            val jsonObject = json.parseToJsonElement(configStr).jsonObject
            val modules = jsonObject["modules"]?.jsonObject ?: return

            _modules.forEach { module ->
                modules[module.name]?.let {
                    if (it is JsonObject) {
                        module.fromJson(it)
                    }
                }
            }
        } catch (e: Exception) {
            throw IllegalArgumentException("Invalid config format: ${e.message}")
        }
    }

    fun exportConfigToFile(context: Context, filePath: String): Boolean {
        return try {
            val file = if (filePath.contains("/")) {
                File(filePath)
            } else {
                val configsDir = context.getExternalFilesDir("configs")
                configsDir?.mkdirs()
                File(configsDir, if (filePath.endsWith(".json")) filePath else "$filePath.json")
            }

            file.parentFile?.mkdirs()
            file.writeText(exportConfig())
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun getWClientConfigsDirectory(): File? {
        return try {
            val documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)

            val baseDir = if (documentsDir.exists() || documentsDir.mkdirs()) {
                documentsDir
            } else {
                downloadsDir
            }

            val wclientDir = File(baseDir, "WClient")
            val configsDir = File(wclientDir, "configs")

            if (configsDir.exists() || configsDir.mkdirs()) {
                configsDir
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun importConfigFromFile(context: Context, uri: Uri): Boolean {
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                val configStr = input.bufferedReader().readText()
                importConfig(configStr)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}