package gregtech.common.tileentities.machines.multi;

import static com.gtnewhorizon.structurelib.structure.StructureUtility.*;
import static gregtech.api.enums.HatchElement.*;
import static gregtech.api.enums.Textures.BlockIcons.*;
import static gregtech.api.util.GTStructureUtility.buildHatchAdder;

import javax.annotation.Nonnull;

import mcp.mobius.waila.api.IWailaConfigHandler;
import mcp.mobius.waila.api.IWailaDataAccessor;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraft.nbt.NBTTagCompound;

import org.jetbrains.annotations.NotNull;

import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;
import com.gtnewhorizons.modularui.api.math.Alignment;
import com.gtnewhorizons.modularui.common.widget.DynamicPositionedColumn;
import com.gtnewhorizons.modularui.common.widget.FakeSyncWidget;
import com.gtnewhorizons.modularui.common.widget.SlotWidget;
import com.gtnewhorizons.modularui.common.widget.TextWidget;

import gregtech.api.GregTechAPI;
import gregtech.api.enums.Textures;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.logic.ProcessingLogic;
import gregtech.api.metatileentity.implementations.MTEExtendedPowerMultiBlockBase;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.recipe.check.CheckRecipeResultRegistry;
import gregtech.api.recipe.check.SimpleCheckRecipeResult;
import gregtech.api.render.TextureFactory;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.MultiblockTooltipBuilder;
import gregtech.api.util.shutdown.SimpleShutDownReason;

import java.util.List;

public class MTEMegaCutter extends MTEExtendedPowerMultiBlockBase<MTEMegaCutter> implements ISurvivalConstructable {


    private static final int FIRST_PRECISION_THRESHOLD = 50;
    private static final int SECOND_PRECISION_THRESHOLD = 65;
    private static final int THIRD_PRECISION_THRESHOLD = 80;
    private int mTier;
    private int currentPrecisionValue;
    private int precisionDisplay;

    // Steel casing
    private static final int CASING_INDEX = 16;
    private static final String STRUCTURE_TIER_1 = "tier1";
    private static final String STRUCTURE_TIER_2 = "tier2";
    private static final String STRUCTURE_TIER_3 = "tier3";
    private static final IStructureDefinition<MTEMegaCutter> STRUCTURE_DEFINITION = StructureDefinition
        .<MTEMegaCutter>builder()
        .addShape(
            STRUCTURE_TIER_1,
            new String[][] { { "BBB", "B~B", "BBB" }, { "BBB", "B-B", "BBB" }, { "BBB", "BBB", "BBB" } })
        .addShape(
            STRUCTURE_TIER_2,
            new String[][] { { "B B", "BBB", "B~B", "BBB" }, { "   ", "B-B", "B-B", "BBB" }, { "B B", "BBB", "BBB", "BBB" } })
        .addShape(
            STRUCTURE_TIER_3,
            new String[][] { { "B B", "BBB", "B~B", "BBB" }, { "B B", "---", "B-B", "BBB" }, { "B B", "BBB", "BBB", "BBB" } })
        .addElement(
            'B',
            buildHatchAdder(MTEMegaCutter.class)
                .atLeast(InputHatch, InputBus, OutputBus, OutputHatch, Maintenance, Energy, ExoticEnergy)
                .casingIndex(CASING_INDEX)
                .dot(1)
                .buildAndChain(onElementPass(MTEMegaCutter::onCasingAdded, ofBlock(GregTechAPI.sBlockCasings2, 0))))
        .build();

    private int mCasingAmount;

    private void onCasingAdded() {
        mCasingAmount++;
    }

    public MTEMegaCutter(final int aID, final String aName, final String aNameRegional) {
        super(aID, aName, aNameRegional);
    }

    public MTEMegaCutter(String aName) {
        super(aName);
    }

    @Override
    public IStructureDefinition<MTEMegaCutter> getStructureDefinition() {
        return STRUCTURE_DEFINITION;
    }

    @Override
    public boolean isCorrectMachinePart(ItemStack aStack) {
        return true;
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
        return new MTEMegaCutter(this.mName);
    }

    @Override
    public ITexture[] getTexture(IGregTechTileEntity baseMetaTileEntity, ForgeDirection sideDirection,
        ForgeDirection facingDirection, int colorIndex, boolean active, boolean redstoneLevel) {
        if (sideDirection == facingDirection) {
            if (active) return new ITexture[] { Textures.BlockIcons.casingTexturePages[0][16], TextureFactory.builder()
                .addIcon(OVERLAY_FRONT_ASSEMBLY_LINE_ACTIVE)
                .extFacing()
                .build(),
                TextureFactory.builder()
                    .addIcon(OVERLAY_FRONT_ASSEMBLY_LINE_ACTIVE_GLOW)
                    .extFacing()
                    .glow()
                    .build() };
            return new ITexture[] { Textures.BlockIcons.casingTexturePages[0][16], TextureFactory.builder()
                .addIcon(OVERLAY_FRONT_ASSEMBLY_LINE)
                .extFacing()
                .build(),
                TextureFactory.builder()
                    .addIcon(OVERLAY_FRONT_ASSEMBLY_LINE_GLOW)
                    .extFacing()
                    .glow()
                    .build() };
        }
        return new ITexture[] { Textures.BlockIcons.casingTexturePages[0][16] };
    }

    @Override
    protected MultiblockTooltipBuilder createTooltip() {
        MultiblockTooltipBuilder tt = new MultiblockTooltipBuilder();
        tt.addMachineType("Cutting Machine")
            .addInfo("Cutting DN since 1984")
            .beginStructureBlock(3, 3, 3, true)
            .toolTipFinisher();
        return tt;
    }

    @Override
    public boolean onRunningTick(ItemStack aStack) {
        if (currentPrecisionValue < FIRST_PRECISION_THRESHOLD) {
            stopMachine(SimpleShutDownReason.ofCritical("critical_precision_insufficient"));
            return false;
        }
        return super.onRunningTick(aStack);
    }

    @Override
    public void onPostTick(IGregTechTileEntity aBaseMetaTileEntity, long aTick) {
        super.onPostTick(aBaseMetaTileEntity, aTick);
        if (this.getControllerSlot() != null) {
            this.currentPrecisionValue = 36 + this.getControllerSlot().stackSize;
        } else {
            this.currentPrecisionValue = 36;
        }
    }

    @Override
    protected void drawTexts(DynamicPositionedColumn screenElements, SlotWidget inventorySlot) {
        super.drawTexts(screenElements, inventorySlot);
        screenElements
            .widget(
                new TextWidget()
                    .setStringSupplier(
                        () -> EnumChatFormatting.WHITE + "Current "
                            + EnumChatFormatting.YELLOW
                            + "precision "
                            + EnumChatFormatting.WHITE
                            + "level: "
                            + EnumChatFormatting.YELLOW
                            + numberFormat.format(precisionDisplay)
                            + "%")
                    .setTextAlignment(Alignment.CenterLeft))
            .widget(new FakeSyncWidget.IntegerSyncer(this::getCurrentPrecisionValue, val -> precisionDisplay = val));
    }

    private int getCurrentPrecisionValue() {
        return currentPrecisionValue;
    }

    @Override
    public void construct(ItemStack stackSize, boolean hintsOnly) {
        if (stackSize.stackSize == 1) {
            buildPiece(STRUCTURE_TIER_1, stackSize, hintsOnly, 1, 1, 0);
        }
        if (stackSize.stackSize == 2) {
            buildPiece(STRUCTURE_TIER_2, stackSize, hintsOnly, 1, 2, 0);
        }
        if (stackSize.stackSize >= 3) {
            buildPiece(STRUCTURE_TIER_3, stackSize, hintsOnly, 1, 2, 0);
        }
    }

    @Override
    public int survivalConstruct(ItemStack stackSize, int elementBudget, ISurvivalBuildEnvironment env) {
        int built = 0;
        if (mMachine) return -1;
        if (stackSize.stackSize == 1) {
            built = survivialBuildPiece(STRUCTURE_TIER_1, stackSize, 1, 1, 0, elementBudget, env, false, true);
        }
        if (stackSize.stackSize == 2) {
            built = survivialBuildPiece(STRUCTURE_TIER_2, stackSize, 1, 2, 0, elementBudget, env, false, true);
        }
        if (stackSize.stackSize >= 3) {
            built = survivialBuildPiece(STRUCTURE_TIER_3, stackSize, 1, 2, 0, elementBudget, env, false, true);
        }
        return built;
    }

    @Override
    public boolean checkMachine(IGregTechTileEntity aBaseMetaTileEntity, ItemStack aStack) {
        if (checkPiece(STRUCTURE_TIER_1, 1, 1, 0)) {
            this.mTier = 1;
        } else if (checkPiece(STRUCTURE_TIER_2, 1, 2, 0)) {
            this.mTier = 2;
        } else if (checkPiece(STRUCTURE_TIER_3, 1, 2, 0)) {
            this.mTier = 3;
        } else this.mTier = 0;
        return (this.mTier > 0);
    }

    @Override
    protected ProcessingLogic createProcessingLogic() {
        return new ProcessingLogic() {

            @NotNull
            @Override
            protected CheckRecipeResult validateRecipe(@Nonnull GTRecipe recipe) {
                if (currentPrecisionValue < FIRST_PRECISION_THRESHOLD) {
                    return SimpleCheckRecipeResult.ofFailure("precision_insufficient");
                }
                return CheckRecipeResultRegistry.SUCCESSFUL;
            }
        };
    }

    @Override
    public void loadNBTData(NBTTagCompound aNBT) {
        mTier = aNBT.getInteger("multiTier");
        super.loadNBTData(aNBT);
    }

    @Override
    public void saveNBTData(NBTTagCompound aNBT) {
        aNBT.setInteger("multiTier", mTier);
        super.saveNBTData(aNBT);
    }

    @Override
    public void getWailaNBTData(EntityPlayerMP player, TileEntity tile, NBTTagCompound tag, World world, int x, int y,
                                int z) {
        super.getWailaNBTData(player, tile, tag, world, x, y, z);
        tag.setInteger("multiTier", mTier);
    }

    @Override
    public void getWailaBody(ItemStack itemStack, List<String> currentTip, IWailaDataAccessor accessor,
                             IWailaConfigHandler config) {
        super.getWailaBody(itemStack, currentTip, accessor, config);
        final NBTTagCompound tag = accessor.getNBTData();
        currentTip.add(
            "Tier: "
                + EnumChatFormatting.WHITE
                + tag.getInteger("multiTier")
                + EnumChatFormatting.RESET);
    }

    @Override
    public RecipeMap<?> getRecipeMap() {
        return RecipeMaps.cutterRecipes;
    }

    @Override
    public int getMaxEfficiency(ItemStack aStack) {
        return 10000;
    }

    @Override
    public int getDamageToComponent(ItemStack aStack) {
        return 0;
    }

    @Override
    public boolean explodesOnComponentBreak(ItemStack aStack) {
        return false;
    }

    @Override
    public boolean supportsVoidProtection() {
        return true;
    }

    @Override
    public boolean supportsBatchMode() {
        return true;
    }

    @Override
    public boolean supportsInputSeparation() {
        return true;
    }

    @Override
    public boolean supportsSingleRecipeLocking() {
        return true;
    }
}
