package gregtech.api.metatileentity;

import com.google.common.base.Preconditions;
import gregtech.api.GregTech_API;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.metatileentity.IMetaTileEntityFactory;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTankInfo;
import net.minecraftforge.fluids.IFluidHandler;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.wrappers.FluidHandlerWrapper;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.wrapper.SidedInvWrapper;

import javax.annotation.Nullable;

public class GregtechTileEntity extends BaseTileEntity implements IGregTechTileEntity, ISidedInventory, IFluidHandler {

    private MetaTileEntity metaTileEntity;

    @Nullable
    @Override
    public IMetaTileEntity getMetaTileEntity() {
        return metaTileEntity;
    }

    @Override
    public void setMetaTileEntity(@Nullable IMetaTileEntity metaTileEntity) {
        Preconditions.checkArgument(metaTileEntity instanceof MetaTileEntity, "GregtechTileEntity supports only MetaTileEntity child!");
        this.metaTileEntity = (MetaTileEntity) metaTileEntity;
        this.metaTileEntity.holder = this;
        markDirty();
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        if(metaTileEntity != null) {
            compound.setString("MetaTileEntityId", GregTech_API.METATILEENTITY_REGISTRY.getNameForObject(metaTileEntity.factory));
            NBTTagCompound metaTileEntityTag = new NBTTagCompound();
            metaTileEntity.saveNBTData(metaTileEntityTag);
            compound.setTag("MetaTileEntity", metaTileEntityTag);
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        if(compound.hasKey("MetaTileEntityId", Constants.NBT.TAG_STRING)) {
            IMetaTileEntityFactory factory = GregTech_API.METATILEENTITY_REGISTRY.getObject(compound.getString("MetaTileEntityId"));
            this.metaTileEntity = (MetaTileEntity) factory.constructMetaTileEntity();
            NBTTagCompound metaTileEntityTag = compound.getCompoundTag("MetaTileEntity");
            metaTileEntity.loadNBTData(metaTileEntityTag);
        }
        return compound;
    }

    @Override
    public boolean receiveClientEvent(int id, int type) {
        return super.receiveClientEvent(id, type);
    }

    @Override
    public String getName() {
        return metaTileEntity == null ? "" : metaTileEntity.getUnlocalizedName();
    }

    @Override
    public boolean hasCustomName() {
        return false;
    }

    @Override
    public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
        if(capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
            return true;
        } else if(capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return true;
        }
        return super.hasCapability(capability, facing);
    }

    @Override
    public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
        if(capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
            return CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.cast(new FluidHandlerWrapper(this, facing));
        } else if(capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(new SidedInvWrapper(this, facing));
        }
        return super.getCapability(capability, facing);
    }

    @Override
    public int getSizeInventory() {
        return metaTileEntity == null ? 0 : metaTileEntity.getSlotsCount();
    }

    @Nullable
    @Override
    public ItemStack getStackInSlot(int index) {
        return metaTileEntity == null ? null : metaTileEntity.getStackInSlot(index);
    }

    @Nullable
    @Override
    public ItemStack decrStackSize(int index, int count) {
        ItemStack slotStack = getStackInSlot(index);
        if(slotStack == null) {
            return null;
        }
        ItemStack decrStack = slotStack.splitStack(count);
        setInventorySlotContents(index, slotStack);
        return decrStack;
    }

    @Nullable
    @Override
    public ItemStack removeStackFromSlot(int index) {
        ItemStack maybeStack = getStackInSlot(index);
        if(maybeStack != null) {
            setInventorySlotContents(index, null);
        }
        return maybeStack;
    }

    @Override
    public void setInventorySlotContents(int index, @Nullable ItemStack stack) {
        if(metaTileEntity != null) {
            metaTileEntity.setStackInSlot(index, stack == null || stack.stackSize <= 0 ? null : stack);
        }
    }

    @Override
    public int getInventoryStackLimit() {
        return 64;
    }

    @Override
    public boolean isUseableByPlayer(EntityPlayer player) {
        return metaTileEntity != null && metaTileEntity.isAccessAllowed(player);
    }

    @Override
    public void openInventory(EntityPlayer player) {
    }

    @Override
    public void closeInventory(EntityPlayer player) {
    }

    @Override
    public boolean isItemValidForSlot(int index, ItemStack stack) {
        return metaTileEntity != null && metaTileEntity.allowPutStack(index, null, stack);
    }

    @Override
    public int getField(int id) {
        return 0;
    }

    @Override
    public void setField(int id, int value) {
    }

    @Override
    public int getFieldCount() {
        return 0;
    }

    @Override
    public void clear() {
        if(metaTileEntity != null) {
            for(int i = 0; i < metaTileEntity.getSlotsCount(); i++) {
                metaTileEntity.setStackInSlot(i, null);
            }
        }
    }

    @Override
    public int[] getSlotsForFace(EnumFacing side) {
        return metaTileEntity == null ? new int[0] : metaTileEntity.getSlotsForFace(side);
    }

    @Override
    public boolean canInsertItem(int index, ItemStack itemStackIn, EnumFacing direction) {
        return metaTileEntity != null && metaTileEntity.allowPutStack(index, direction, itemStackIn);
    }

    @Override
    public boolean canExtractItem(int index, ItemStack stack, EnumFacing direction) {
        return metaTileEntity != null && metaTileEntity.allowPullStack(index, direction, stack);
    }

    @Override
    public boolean canFill(EnumFacing from, Fluid fluid) {
        if(metaTileEntity != null) {
            for(int i = 0; i < metaTileEntity.getTanksCount(); i++) {
                if(metaTileEntity.allowPutFluid(i, from, fluid)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean canDrain(EnumFacing from, Fluid fluid) {
        if(metaTileEntity != null) {
            for(int i = 0; i < metaTileEntity.getTanksCount(); i++) {
                if(metaTileEntity.allowPullFluid(i, from, fluid)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public int fill(EnumFacing from, FluidStack resource, boolean doFill) {
        if(metaTileEntity != null && resource != null) {
            for(int i = 0; i < metaTileEntity.getTanksCount(); i++) {
                if(metaTileEntity.allowPutFluid(i, from, resource.getFluid())) {
                    FluidStack curFluid = metaTileEntity.getFluidInTank(i);
                    if(curFluid == null) {
                        int amountInserted = Math.min(metaTileEntity.getTankCapacity(i), resource.amount);
                        if(doFill) {
                            curFluid = resource.copy();
                            curFluid.amount = amountInserted;
                            metaTileEntity.setFluidInTank(i, curFluid);
                        }
                        return amountInserted;
                    } else if(curFluid.isFluidEqual(resource)) {
                        int amountInserted = Math.min(metaTileEntity.getTankCapacity(i) - curFluid.amount, resource.amount);
                        if(doFill) {
                            curFluid.amount += amountInserted;
                            metaTileEntity.setFluidInTank(i, curFluid);
                        }
                        return amountInserted;
                    }
                }
            }
        }
        return 0;
    }

    @Override
    public FluidStack drain(EnumFacing from, FluidStack resource, boolean doDrain) {
        if (metaTileEntity != null && resource != null) {
            for(int i = 0; i < metaTileEntity.getTanksCount(); i++) {
                FluidStack fluidStack = metaTileEntity.getFluidInTank(i);
                if(fluidStack != null && fluidStack.isFluidEqual(resource) && metaTileEntity.allowPullFluid(i, from, fluidStack.getFluid())) {
                    int canDrain = Math.min(fluidStack.amount, resource.amount);
                    if(doDrain) {
                        fluidStack.amount -= canDrain;
                        metaTileEntity.setFluidInTank(i, fluidStack);
                    }
                    FluidStack drainStack = fluidStack.copy();
                    drainStack.amount = canDrain;
                    return drainStack;
                }
            }
        }
        return null;
    }

    @Override
    public FluidStack drain(EnumFacing from, int maxDrain, boolean doDrain) {
        if(metaTileEntity != null && maxDrain > 0) {
            for(int i = 0; i < metaTileEntity.getTanksCount(); i++) {
                FluidStack fluidStack = metaTileEntity.getFluidInTank(i);
                if(fluidStack != null && metaTileEntity.allowPullFluid(i, from, fluidStack.getFluid())) {
                    int canDrain = Math.min(fluidStack.amount, maxDrain);
                    if(doDrain) {
                        fluidStack.amount -= canDrain;
                        metaTileEntity.setFluidInTank(i, fluidStack);
                    }
                    FluidStack drainStack = fluidStack.copy();
                    drainStack.amount = canDrain;
                    return drainStack;
                }
            }
        }
        return null;
    }

    @Override
    public FluidTankInfo[] getTankInfo(EnumFacing from) {
        if(metaTileEntity != null) {
            FluidTankInfo[] fluidTankInfo = new FluidTankInfo[metaTileEntity.getTanksCount()];
            for(int i = 0; i < fluidTankInfo.length; i++) {
                fluidTankInfo[i] = new FluidTankInfo(metaTileEntity.getFluidInTank(i), metaTileEntity.getTankCapacity(i));
            }
            return fluidTankInfo;
        }
        return new FluidTankInfo[0];
    }

}
