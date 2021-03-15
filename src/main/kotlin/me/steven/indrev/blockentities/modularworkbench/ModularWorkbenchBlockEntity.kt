package me.steven.indrev.blockentities.modularworkbench

import me.steven.indrev.api.machines.Tier
import me.steven.indrev.api.machines.properties.Property
import me.steven.indrev.blockentities.MachineBlockEntity
import me.steven.indrev.config.BasicMachineConfig
import me.steven.indrev.inventories.inventory
import me.steven.indrev.items.armor.IRColorModuleItem
import me.steven.indrev.items.armor.IRModularArmorItem
import me.steven.indrev.items.armor.IRModuleItem
import me.steven.indrev.recipes.machines.ModuleRecipe
import me.steven.indrev.registry.MachineRegistry
import me.steven.indrev.tools.modular.ArmorModule
import me.steven.indrev.tools.modular.IRModularItem
import me.steven.indrev.utils.component1
import me.steven.indrev.utils.component2
import me.steven.indrev.utils.getAllOfType
import net.minecraft.block.BlockState
import net.minecraft.item.ItemStack
import net.minecraft.nbt.CompoundTag
import net.minecraft.screen.ArrayPropertyDelegate
import net.minecraft.util.Identifier

class ModularWorkbenchBlockEntity(tier: Tier) : MachineBlockEntity<BasicMachineConfig>(tier, MachineRegistry.MODULAR_WORKBENCH_REGISTRY) {

    init {
        this.propertyDelegate = ArrayPropertyDelegate(7)
        this.inventoryComponent = inventory(this) {

            maxStackCount = 1

            0 filter { (_, item) -> item !is IRModularItem<*> }
            1 filter { stack -> stack.item is IRModuleItem }
            2 filter { stack -> stack.item is IRModularItem<*> }
            3 until 15 filter { stack, index -> recipe != null && recipe!!.input.size > index - 3 && recipe!!.input[index - 3].ingredient.test(stack) }
            input {
                slots = (1 until 15).map { it }.toIntArray()
            }

            output { slot = 15 }

        }
    }

    override val maxOutput: Double = 0.0

    private var processTime: Int by Property(2, 0)
    private var maxProcessTime: Int by Property(3, 0)
    private var state: State = State.IDLE
        set(value) {
            field = value
            propertyDelegate[4] = value.ordinal
        }

    var selectedRecipe: Identifier? = null
    var recipe: ModuleRecipe? = null
        get() {
            if (selectedRecipe != null)
                field = world!!.recipeManager.getAllOfType(ModuleRecipe.TYPE)[selectedRecipe]!!
            return field
        }

    var moduleProcessTime: Int by Property(5, 0)
    var moduleMaxProcessTime: Int by Property(6, 0)

    override fun machineTick() {
       tickModuleInstall()
    }

    private fun tickModuleCraft() {
    }

    private fun tickModuleInstall() {
        val inventory = inventoryComponent?.inventory ?: return
        val targetStack = inventory.getStack(2)
        val moduleStack = inventory.getStack(1)
        if (moduleStack.item !is IRModuleItem || targetStack.item !is IRModularItem<*>) {
            processTime = 0
            state = State.IDLE
            return
        }
        val targetItem = targetStack.item as IRModularItem<*>
        val moduleItem = moduleStack.item as IRModuleItem
        val module = moduleItem.module
        val compatible = targetItem.getCompatibleModules(targetStack)
        if (inventory.isEmpty) {
            processTime = 0
            workingState = false
            state = State.IDLE
        } else {
            if (isProcessing()
                && compatible.contains(module)
                && use(config.energyCost)) {
                workingState = true
                processTime += config.processSpeed.toInt()
                if (processTime >= maxProcessTime) {
                    inventory.setStack(1, ItemStack.EMPTY)
                    val tag = targetStack.orCreateTag
                    when {
                        module == ArmorModule.COLOR -> {
                            if (targetItem !is IRModularArmorItem) return
                            val colorModuleItem = moduleItem as IRColorModuleItem
                            targetItem.setColor(targetStack, colorModuleItem.color)
                        }
                        tag.contains(module.key) -> {
                            val level = tag.getInt(module.key) + 1
                            tag.putInt(module.key, level.coerceAtMost(module.maxLevel))
                        }
                        else -> tag.putInt(module.key, 1)
                    }
                    processTime = 0
                    state = State.IDLE
                }
            } else if (energy > 0 && !targetStack.isEmpty && !moduleStack.isEmpty && compatible.contains(module)) {
                val tag = targetStack.orCreateTag
                if (tag.contains(module.key)) {
                    val level = module.getMaxInstalledLevel(targetStack)
                    if (module != ArmorModule.COLOR && level >= module.maxLevel) {
                        state = State.MAX_LEVEL
                        return
                    }
                }
                processTime = 1
                maxProcessTime = 1200
                workingState = true
                state = State.INSTALLING
            } else {
                state = State.INCOMPATIBLE
            }
        }
    }

    private fun isProcessing(): Boolean = processTime > 0 && energy > 0

    override fun fromTag(state: BlockState?, tag: CompoundTag?) {
        processTime = tag?.getInt("ProcessTime") ?: 0
        if (tag?.contains("SelectedRecipe") == true)
            selectedRecipe = Identifier(tag.getString("SelectedRecipe"))
        super.fromTag(state, tag)
    }

    override fun toTag(tag: CompoundTag?): CompoundTag {
        tag?.putInt("ProcessTime", processTime)
        if (selectedRecipe != null)
            tag?.putString("SelectedRecipe", selectedRecipe!!.toString())
        return super.toTag(tag)
    }

    override fun fromClientTag(tag: CompoundTag?) {
        processTime = tag?.getInt("ProcessTime") ?: 0
        if (tag?.contains("SelectedRecipe") == true)
            selectedRecipe = Identifier(tag.getString("SelectedRecipe"))
        super.fromClientTag(tag)
    }

    override fun toClientTag(tag: CompoundTag?): CompoundTag {
        tag?.putInt("ProcessTime", processTime)
        if (selectedRecipe != null)
            tag?.putString("SelectedRecipe", selectedRecipe!!.toString())
        return super.toClientTag(tag)
    }

    enum class State {
        IDLE,
        INSTALLING,
        INCOMPATIBLE,
        MAX_LEVEL;
    }
}