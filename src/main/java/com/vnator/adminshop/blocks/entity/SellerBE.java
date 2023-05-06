package com.vnator.adminshop.blocks.entity;

import com.vnator.adminshop.blocks.AutoShopMachine;
import com.vnator.adminshop.network.PacketSellerTransaction;
import com.vnator.adminshop.screen.SellerMenu;
import com.vnator.adminshop.setup.Messages;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;

public class SellerBE extends BlockEntity implements AutoShopMachine {
    private String machineOwnerUUID = "";
    private String accOwnerUUID = "";
    private int accID = 1;
    private int tickCounter = 0;
    private final ItemStackHandler itemHandler = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    public final ItemStackHandler getItemHandler() {
        return itemHandler;
    }

    private LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.empty();

    public SellerBE(BlockPos pWorldPosition, BlockState pBlockState) {
        super(ModBlockEntities.SELLER.get(), pWorldPosition, pBlockState);
    }

    @Override
    public Component getDisplayName() {
        return new TextComponent("Auto-Seller");
    }

    public void setAccOwnerUUID(String accOwnerUUID) {
        System.out.println("setAccOwnerUUID("+accOwnerUUID+")");
        this.accOwnerUUID = accOwnerUUID;
        setChanged();
    }
    public void setAccID(int accID) {
        System.out.println("setAccID("+accID+")");
        this.accID = accID;
        setChanged();
    }

    public void setAccInfo(String accOwner, int accId) {
        System.out.println("setAccInfo("+accID+", "+accId+")");
        this.accID = accId;
        this.accOwnerUUID = accOwner;
        setChanged();
    }

    public void setAccInfo(String machineOwner, String accOwner, int accId) {
        System.out.println("setAccInfo("+machineOwner+", "+accID+", "+accId+")");
        this.machineOwnerUUID = machineOwner;
        this.accID = accId;
        this.accOwnerUUID = accOwner;
        setChanged();
    }

    public void setMachineOwnerUUID(String machineOwnerUUID) {
        System.out.println("setMachineOwnerUUID("+machineOwnerUUID+")");
        this.machineOwnerUUID = machineOwnerUUID;
        setChanged();
    }

    public String getMachineOwnerUUID() {
        return machineOwnerUUID;
    }

    public String getAccOwnerUUID() {
        return accOwnerUUID;
    }

    public int getAccID() {
        return accID;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int pContainerId, Inventory pInventory, Player pPlayer) {
        return new SellerMenu(pContainerId, pInventory, this);
    }

    public static void tick(Level pLevel, BlockPos pPos, BlockState pState, SellerBE pBlockEntity) {
        if(hasItem(pBlockEntity)) {
            pBlockEntity.tickCounter++;
            if (pBlockEntity.tickCounter > 20) {
                pBlockEntity.tickCounter = 0;
                // Send sell transaction
                Messages.sendToServer(new PacketSellerTransaction(pPos));
            }
        }
    }

    private static boolean hasItem(SellerBE entity) {
        return !entity.itemHandler.getStackInSlot(0).isEmpty();
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @javax.annotation.Nullable Direction side) {
        if (cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return lazyItemHandler.cast();
        }

        return super.getCapability(cap, side);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        lazyItemHandler = LazyOptional.of(() -> itemHandler);
    }

    @Override
    public void invalidateCaps()  {
        super.invalidateCaps();
        lazyItemHandler.invalidate();
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag) {
        tag.put("inventory", this.itemHandler.serializeNBT());
        tag.putString("machineowner", this.machineOwnerUUID);
        tag.putString("accowner", this.accOwnerUUID);
        tag.putInt("accid", this.accID);
        super.saveAdditional(tag);
    }

    @Override
    public void load(CompoundTag tag) {
        this.itemHandler.deserializeNBT(tag.getCompound("inventory"));
        this.machineOwnerUUID = tag.getString("machineowner");
        this.accOwnerUUID = tag.getString("accowner");
        this.accID = tag.getInt("accid");
        super.load(tag);
    }

    public void drops() {
        SimpleContainer inventory = new SimpleContainer(itemHandler.getSlots());
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            inventory.setItem(i, itemHandler.getStackInSlot(i));
        }

        Containers.dropContents(this.level, this.worldPosition, inventory);
    }
}
