package gregtech.common.metatileentities.storage;

import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.pipeline.ColourMultiplier;
import codechicken.lib.render.pipeline.IVertexOperation;
import codechicken.lib.vec.Matrix4;
import com.google.common.collect.Lists;
import gregtech.api.gui.GuiTextures;
import gregtech.api.gui.ModularUI;
import gregtech.api.gui.widgets.CycleButtonWidget;
import gregtech.api.gui.widgets.ImageWidget;
import gregtech.api.gui.widgets.PhantomFluidWidget;
import gregtech.api.gui.widgets.TextFieldWidget2;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.MetaTileEntityHolder;
import gregtech.api.util.GTUtility;
import gregtech.client.renderer.texture.Textures;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.function.Function;

import static net.minecraftforge.fluids.capability.templates.FluidHandlerItemStack.FLUID_NBT_KEY;

public class MetaTileEntityCreativeTank extends MetaTileEntity {

    private int mBPerCycle = 1;
    private int ticksPerCycle = 1;
    private FluidTank fluidTank = new FluidTank(1);

    private boolean active = false;

    private final List<Character> ALLOWED_CHARS = Lists.newArrayList('0', '1', '2', '3', '4', '5', '6', '7', '8', '9');

    public MetaTileEntityCreativeTank(ResourceLocation metaTileEntityId) {
        super(metaTileEntityId);
    }

    @Override
    public void renderMetaTileEntity(CCRenderState renderState, Matrix4 translation, IVertexOperation[] pipeline) {
        Textures.VOLTAGE_CASINGS[14].render(renderState, translation, ArrayUtils.add(pipeline,
                new ColourMultiplier(GTUtility.convertRGBtoOpaqueRGBA_CL(getPaintingColorForRendering()))));
        Textures.CREATIVE_CONTAINER_OVERLAY.renderSided(EnumFacing.UP, renderState, translation, pipeline);
        Textures.PIPE_OUT_OVERLAY.renderSided(this.getFrontFacing(), renderState, translation, pipeline);
        Textures.FLUID_OUTPUT_OVERLAY.renderSided(this.getFrontFacing(), renderState, translation, pipeline);
    }

    @Override
    public MetaTileEntity createMetaTileEntity(MetaTileEntityHolder holder) {
        return new MetaTileEntityCreativeTank(this.metaTileEntityId);
    }

    @Override
    protected ModularUI createUI(EntityPlayer entityPlayer) {
        ModularUI.Builder builder = ModularUI.builder(GuiTextures.BACKGROUND, 176, 209)
                .bindPlayerInventory(entityPlayer.inventory, 126);
        builder.widget(new PhantomFluidWidget(36, 6, 18, 18,
                () -> this.fluidTank.getFluid(), data -> {
            this.fluidTank.setFluid(data);
        })
                .showTip(false).setBackgroundTexture(GuiTextures.SLOT_DARKENED));
        builder.label(7, 9, "Fluid");
        builder.widget(new ImageWidget(7, 45, 154, 14, GuiTextures.DISPLAY));
        builder.widget(new TextFieldWidget2(9, 47, 152, 10, () -> String.valueOf(mBPerCycle), value -> {
            if (!value.isEmpty()) {
                mBPerCycle = Integer.parseInt(value);
            }
        }).setAllowedChars("0123456789").setMaxLength(19).setValidator(getTextFieldValidator()));
        builder.label(7, 28, "mB per cycle");

        builder.widget(new ImageWidget(7, 82, 154, 14, GuiTextures.DISPLAY));
        builder.widget(new TextFieldWidget2(9, 84, 152, 10, () -> String.valueOf(ticksPerCycle), value -> {
            if (!value.isEmpty()) {
                ticksPerCycle = Integer.parseInt(value);
            }
        }).setMaxLength(10).setNumbersOnly(0, Integer.MAX_VALUE));
        builder.label(7, 65, "Ticks per cycle");


        builder.widget(new CycleButtonWidget(7, 101, 162, 20, () -> active, value -> active = value, "Not active", "Active"));

        return builder.build(getHolder(), entityPlayer);
    }

    @Override
    public void update() {
        super.update();
        if (getOffsetTimer() % ticksPerCycle != 0 || fluidTank.getFluid() == null
                || getWorld().isRemote || !active) return;

        FluidStack stack = fluidTank.getFluid().copy();

        TileEntity tile = getWorld().getTileEntity(getPos().offset(this.getFrontFacing()));
        if (tile != null) {
            IFluidHandler fluidHandler = tile.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, frontFacing);
            if (fluidHandler == null || fluidHandler.getTankProperties().length == 0)
                return;

            stack.amount = mBPerCycle;
            int canInsertAmount = fluidHandler.fill(stack, false);
            stack.amount = Math.min(mBPerCycle, canInsertAmount);

            fluidHandler.fill(stack, true);
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound data) {
        NBTTagCompound fluidTankTag = new NBTTagCompound();
        fluidTank.writeToNBT(fluidTankTag);
        data.setTag("FluidTank", fluidTankTag);
        data.setInteger("mBPerCycle", mBPerCycle);
        data.setInteger("TicksPerCycle", ticksPerCycle);
        data.setBoolean("Active", active);
        return super.writeToNBT(data);
    }

    @Override
    public void readFromNBT(NBTTagCompound data) {
        fluidTank.readFromNBT(data.getCompoundTag("FluidTank"));
        mBPerCycle = data.getInteger("mBPerCycle");
        ticksPerCycle = data.getInteger("TicksPerCycle");
        active = data.getBoolean("Active");
        super.readFromNBT(data);
    }

    public Function<String, String> getTextFieldValidator() {
        return val -> {
            if (val.isEmpty()) {
                return "0";
            }
            long num;
            try {
                num = Long.parseLong(val);
            } catch (NumberFormatException ignored) {
                return "0";
            }
            if (num < 0) {
                return "0";
            }
            return val;
        };
    }

    @Override
    public Pair<TextureAtlasSprite, Integer> getParticleTexture() {
        return Pair.of(Textures.VOLTAGE_CASINGS[14].getParticleSprite(), this.getPaintingColor());
    }

    @Override
    public void initFromItemStackData(NBTTagCompound itemStack) {
        super.initFromItemStackData(itemStack);
        if (itemStack.hasKey(FLUID_NBT_KEY, Constants.NBT.TAG_COMPOUND)) {
            fluidTank.setFluid(FluidStack.loadFluidStackFromNBT(itemStack.getCompoundTag(FLUID_NBT_KEY)));
            mBPerCycle = itemStack.getInteger("mBPerCycle");
            ticksPerCycle = itemStack.getInteger("ticksPerCycle");
        }
    }

    @Override
    public void writeItemStackData(NBTTagCompound itemStack) {
        super.writeItemStackData(itemStack);
        FluidStack stack = fluidTank.getFluid();
        if (stack != null && stack.amount > 0) {
            itemStack.setTag(FLUID_NBT_KEY, stack.writeToNBT(new NBTTagCompound()));
            itemStack.setInteger("mBPerCycle", mBPerCycle);
            itemStack.setInteger("ticksPerCycle", ticksPerCycle);
        }
    }

}