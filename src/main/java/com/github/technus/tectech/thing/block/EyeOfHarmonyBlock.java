package com.github.technus.tectech.thing.block;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import com.github.technus.tectech.TecTech;

import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class EyeOfHarmonyBlock extends Block {

    public EyeOfHarmonyBlock() {
        super(Material.iron);
        this.setResistance(20f);
        this.setHardness(-1.0f);
        this.setCreativeTab(TecTech.creativeTabEM);
        this.setBlockName("Eye of Harmony Renderer");
        this.setLightLevel(100.0f);
        registerOther(this);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister iconRegister) {
        blockIcon = iconRegister.registerIcon("gregtech:iconsets/TRANSPARENT");
    }

    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    @Override
    public boolean canRenderInPass(int a) {
        return true;
    }

    @Override
    public boolean renderAsNormalBlock() {
        return false;
    }

    @Override
    public boolean hasTileEntity(int metadata) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(World world, int metadata) {
        return new TileEyeOfHarmony();
    }

    public static void registerOther(Block block) {
        String name = block.getUnlocalizedName().substring(block.getUnlocalizedName().indexOf(".") + 1);
        GameRegistry.registerBlock(block, name.substring(name.indexOf(":") + 1));
    }

}
