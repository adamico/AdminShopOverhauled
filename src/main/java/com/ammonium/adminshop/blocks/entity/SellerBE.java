package com.ammonium.adminshop.blocks.entity;

import com.ammonium.adminshop.AdminShop;
import com.ammonium.adminshop.blocks.AutoShopMachine;
import com.ammonium.adminshop.money.BankAccount;
import com.ammonium.adminshop.money.MachineOwnerInfo;
import com.ammonium.adminshop.money.MoneyManager;
import com.ammonium.adminshop.network.PacketSyncMoneyToClient;
import com.ammonium.adminshop.screen.SellerMenu;
import com.ammonium.adminshop.setup.Messages;
import com.ammonium.adminshop.shop.Shop;
import com.ammonium.adminshop.shop.ShopItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.UUID;

public class SellerBE extends BlockEntity implements AutoShopMachine {
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
                if (!pLevel.isClientSide) {
                    assert pLevel instanceof ServerLevel;
                    sellerTransaction(pPos, pBlockEntity, (ServerLevel) pLevel);
                }
            }
        }
    }

    public static void sellerTransaction(BlockPos pos, SellerBE sellerEntity, ServerLevel level) {
        System.out.println("Processing seller transaction for "+pos);
        ItemStackHandler itemHandler = sellerEntity.getItemHandler();
        Item item = itemHandler.getStackInSlot(0).getItem();
        int count = itemHandler.getStackInSlot(0).getCount();
        itemHandler.extractItem(0, count, false);
        ShopItem shopItem = Shop.get().getShopSellMap().get(item);
        long itemCost = shopItem.getPrice();
        long price = (long) count * itemCost;
        if (count == 0) {
            return;
        }
        // Get local MoneyManager
        MoneyManager moneyManager = MoneyManager.get(level);
        // attempt transaction
        MachineOwnerInfo machineOwnerInfo = MachineOwnerInfo.get(level);
        Pair<String, Integer> machineAccount = machineOwnerInfo.getMachineAccount(pos);
        // Check if account still exists
        if (!moneyManager.existsBankAccount(machineAccount.getKey(), machineAccount.getValue())) {
            AdminShop.LOGGER.error("Seller machine account does not exist");
            return;
        }
        String accOwner = machineAccount.getKey();
        int accID = machineAccount.getValue();
        // Check if account has necessary trade permit
        if (!moneyManager.getBankAccount(accOwner, accID).hasPermit(shopItem.getPermitTier())) {
            AdminShop.LOGGER.warn("Seller machine account does not have necessary trade permit");
            return;
        }
        boolean success = moneyManager.addBalance(accOwner, accID, price);
        if (!success) {
            AdminShop.LOGGER.error("Error selling item.");
            return;
        }
        // Sync account data
        // Get current bank account
        BankAccount currentAccount = moneyManager.getBankAccount(accOwner, accID);
        // Sync money with bank account's members
        assert currentAccount.getMembers().contains(accOwner);
        currentAccount.getMembers().forEach(memberUUID -> {
            List<BankAccount> usableAccounts = moneyManager.getSharedAccounts().get(memberUUID);
            ServerPlayer playerByUUID = (ServerPlayer) level.getPlayerByUUID(UUID.fromString(memberUUID));
            if (playerByUUID != null) {
                Messages.sendToPlayer(new PacketSyncMoneyToClient(usableAccounts), playerByUUID);
            }
        });
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
        super.saveAdditional(tag);
    }

    @Override
    public void load(CompoundTag tag) {
        this.itemHandler.deserializeNBT(tag.getCompound("inventory"));
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
