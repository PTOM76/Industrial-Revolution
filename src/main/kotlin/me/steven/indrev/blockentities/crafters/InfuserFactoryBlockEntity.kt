package me.steven.indrev.blockentities.crafters

import me.steven.indrev.components.CraftingComponent
import me.steven.indrev.components.MultiblockComponent
import me.steven.indrev.components.TemperatureComponent
import me.steven.indrev.inventories.inventory
import me.steven.indrev.items.upgrade.Upgrade
import me.steven.indrev.recipes.machines.IRRecipeType
import me.steven.indrev.recipes.machines.InfuserRecipe
import me.steven.indrev.registry.IRRegistry
import me.steven.indrev.registry.MachineRegistry
import me.steven.indrev.utils.Tier
import net.minecraft.screen.ArrayPropertyDelegate
import net.minecraft.state.property.Properties
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction

class InfuserFactoryBlockEntity(tier: Tier) :
    CraftingMachineBlockEntity<InfuserRecipe>(tier, MachineRegistry.INFUSER_FACTORY_REGISTRY) {

    init {
        this.propertyDelegate = ArrayPropertyDelegate(15)
        this.temperatureComponent = TemperatureComponent({ this }, 0.06, 700..1100, 1400.0)
        this.inventoryComponent = inventory(this) {
            input { slots = intArrayOf(6, 7, 9, 10, 12, 13, 15, 16, 18, 19) }
            output { slots = intArrayOf(8, 11, 14, 17, 20) }
        }
        this.craftingComponents = Array(5) { index ->
            CraftingComponent(index, this).apply {
                inputSlots = intArrayOf(6 + (index * 3), 6 + (index * 3) + 1)
                outputSlots = intArrayOf(6 + (index * 3) + 2)
            }
        }
        this.multiblockComponent = MultiblockComponent.Builder()
            .cube(BlockPos(-1, 1, 1), 3, 3, 1, IRRegistry.FRAME.defaultState)
            .cube(BlockPos(0, 0, 1), 2, 3, 1, IRRegistry.SILO.defaultState)
            .cube(BlockPos(1, -1, 1), 1, 3, 1, INTAKE_STATE)
            .cube(BlockPos(0, -1, 1), 1, 3, 1, DUCT_STATE)
            .cube(BlockPos(-1, 0, 2), 1, 2, 1, CABINE_STATE)
            .add(BlockPos(-1, 0, 1), CONTROLLER_STATE)
            .add(BlockPos(-1, -1, 1), IRRegistry.WARNING_STROBE.defaultState)
            .build(this)
    }

    override fun splitStacks() {
        splitStacks(intArrayOf(6, 9, 12, 15, 18))
        splitStacks(intArrayOf(7, 10, 13, 16, 19))
    }

    override val type: IRRecipeType<InfuserRecipe> = InfuserRecipe.TYPE

    override fun getUpgradeSlots(): IntArray = intArrayOf(2, 3, 4, 5)

    override fun getAvailableUpgrades(): Array<Upgrade> = Upgrade.DEFAULT

    companion object {
        private val CONTROLLER_STATE = IRRegistry.CONTROLLER.defaultState.with(Properties.HORIZONTAL_FACING, Direction.NORTH)
        private val DUCT_STATE = IRRegistry.DUCT.defaultState.with(Properties.HORIZONTAL_FACING, Direction.WEST)
        private val CABINE_STATE = IRRegistry.CABINET.defaultState.with(Properties.HORIZONTAL_FACING, Direction.WEST)
        private val INTAKE_STATE = IRRegistry.INTAKE.defaultState.with(Properties.HORIZONTAL_FACING, Direction.WEST)
    }
}