package me.steven.indrev.world.chunkveins

import blue.endless.jankson.Jankson
import me.steven.indrev.utils.identifier
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener
import net.minecraft.resource.ResourceManager
import net.minecraft.util.Identifier
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

class VeinTypeResourceListener : SimpleSynchronousResourceReloadListener {
    override fun apply(manager: ResourceManager?) {
        val jankson = Jankson.builder().build()
        val ids = manager?.findResources("veintypes") { r -> r.endsWith(".json") || r.endsWith(".json5") }
        ids?.forEach { id ->
            val json = jankson.load(manager.getResource(id).inputStream)
            val veinType = VeinType.fromJson(json)
            if (veinType == null) {
                LOGGER.error("Unable to load vein type $id!")
                return@forEach
            }
            veinType.forEach { VeinType.REGISTERED[it.id] = it }
        }
        LOGGER.info("Loaded ${VeinType.REGISTERED.size} vein types!")
    }

    override fun getFabricId(): Identifier = identifier("veintypes")

    companion object {
        val LOGGER: Logger = LogManager.getLogger("Vein Type Resource Reloader")
    }
}