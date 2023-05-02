package com.vnator.adminshop.blocks.entity;

import com.vnator.adminshop.AdminShop;
import com.vnator.adminshop.blocks.MachineWithOwnerAndAccount;
import com.vnator.adminshop.money.BankAccount;
import com.vnator.adminshop.money.MachineOwnerInfo;
import com.vnator.adminshop.money.MoneyManager;
import com.vnator.adminshop.network.PacketSyncMoneyToClient;
import com.vnator.adminshop.screen.BuyerMenu;
import com.vnator.adminshop.setup.Messages;
import com.vnator.adminshop.shop.Shop;
import com.vnator.adminshop.shop.ShopItem;
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.UUID;

public class BuyerBE extends BlockEntity implements MachineWithOwnerAndAccount {
    private String machineOwnerUUID = "UNKNOWN";
    private String accOwnerUUID = "UNKNOWN";
    private int accID = 1;
    private int tickCounter = 0;
    // TODO GET SHOPITEM
    private ShopItem item;
    private final ItemStackHandler itemHandler = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    private LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.empty();

    public BuyerBE(BlockPos pWorldPosition, BlockState pBlockState) {
        super(ModBlockEntities.BUYER.get(), pWorldPosition, pBlockState);
    }

    @Override
    public Component getDisplayName() {
        return new TextComponent("Auto-Buyer");
    }

    public void setAccOwnerUUID(String accOwnerUUID) {
        if (this.level == null || this.level.isClientSide) {
            AdminShop.LOGGER.error("Do not set accOwnerUUID from client side!");
            return;
        }
        System.out.println("setAccOwnerUUID("+accOwnerUUID+")");
        this.accOwnerUUID = accOwnerUUID;
        syncMachineOwnerInfo((ServerLevel) this.level, this.getBlockPos(), this.machineOwnerUUID, this.accOwnerUUID,
                this.accID);
        setChanged();
    }
    public void setAccID(int accID) {
        if (this.level == null || this.level.isClientSide) {
            AdminShop.LOGGER.error("Do not set accID from client side!");
            return;
        }
        System.out.println("setAccID("+accID+")");
        this.accID = accID;
        syncMachineOwnerInfo((ServerLevel) this.level, this.getBlockPos(), this.machineOwnerUUID, this.accOwnerUUID, this.accID);
        setChanged();
    }

    public void setAccInfo(String accOwner, int accId) {
        if (this.level == null || this.level.isClientSide) {
            AdminShop.LOGGER.error("Do not set accInfo from client side!");
            return;
        }
        System.out.println("setAccInfo("+accID+", "+accId+")");
        this.accID = accId;
        this.accOwnerUUID = accOwner;
        syncMachineOwnerInfo((ServerLevel) this.level, this.getBlockPos(), this.machineOwnerUUID, accOwner, accId);
        setChanged();
    }

    public void setAccInfo(String machineOwner, String accOwner, int accId) {
        if (this.level == null || this.level.isClientSide) {
            AdminShop.LOGGER.error("Do not set accInfo from client side!");
            return;
        }
        System.out.println("setAccInfo("+machineOwner+", "+accID+", "+accId+")");
        this.machineOwnerUUID = machineOwner;
        this.accID = accId;
        this.accOwnerUUID = accOwner;
        syncMachineOwnerInfo((ServerLevel) this.level, this.getBlockPos(), machineOwner, accOwner, accId);
        setChanged();
    }

    public void setMachineOwnerUUID(String machineOwnerUUID) {
        if (this.level == null || this.level.isClientSide) {
            AdminShop.LOGGER.error("Do not set machineOwnerUUID from client side!");
            return;
        }
        System.out.println("setMachineOwnerUUID("+machineOwnerUUID+")");
        this.machineOwnerUUID = machineOwnerUUID;
        syncMachineOwnerInfo((ServerLevel) this.level, this.getBlockPos(), this.machineOwnerUUID, this.accOwnerUUID,
                this.accID);
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
        return new BuyerMenu(pContainerId, pInventory, this);
    }

    // TODO TICKER
    public static void tick(Level pLevel, BlockPos pPos, BlockState pState, BuyerBE pBlockEntity) {
        pBlockEntity.tickCounter++;
        if (pBlockEntity.tickCounter % 20 == 0) {
            buyItem(pBlockEntity);
        }
    }


    // TODO BUY ITEM
    private static void buyItem(BuyerBE entity) {
        Item item = entity.itemHandler.getStackInSlot(0).getItem();
        int count = entity.itemHandler.getStackInSlot(0).getCount();
        entity.itemHandler.extractItem(0, count, false);
        ShopItem shopItem = Shop.get().getShopSellMap().get(item);
        System.out.println("Attempting sellTransaction(entity, "+entity.getAccOwnerUUID()+","+entity.getAccID()+", "+
                shopItem+", "+count);
        buyTransaction(entity, entity.accOwnerUUID, entity.getAccID(), shopItem, count);
    }

    // TODO BUY TRANSACTION
    private static void buyTransaction(BuyerBE entity, String accOwner, int accID, ShopItem item, int quantity) {
        int itemCost = item.getPrice();
        long price = (long) quantity * itemCost;
        if (quantity == 0) {
            AdminShop.LOGGER.error("No items sold.");
            return;
        }
        // Get local MoneyManager and attempt transaction
        assert entity.level != null;
        assert entity.level instanceof ServerLevel;
        MoneyManager moneyManager = MoneyManager.get(entity.level);
        boolean success = moneyManager.addBalance(accOwner, accID, price);
        if (success) {
            AdminShop.LOGGER.info("Sold item.");
        } else {
            AdminShop.LOGGER.error("Error selling item.");
            return;
        }
        syncAccountData((ServerLevel) entity.level, accOwner, accID);
    }

    private static void syncAccountData(ServerLevel level, String accOwner, int accID) {
        // Get current bank account
        MoneyManager moneyManager = MoneyManager.get(level);

        BankAccount currentAccount = moneyManager.getBankAccount(accOwner, accID);
        // Sync money with bank account's members
        assert currentAccount.getMembers().contains(accOwner);
        currentAccount.getMembers().forEach(memberUUID -> {
            List<BankAccount> usableAccounts = moneyManager.getSharedAccounts().get(memberUUID);
            Messages.sendToPlayer(new PacketSyncMoneyToClient(usableAccounts), (ServerPlayer) level.
                    getPlayerByUUID(UUID.fromString(memberUUID)));
        });
    }
    private static void syncMachineOwnerInfo(ServerLevel level, BlockPos pos, String machineOwner, String accOwner,
                                             int id) {
        System.out.println("Syncing with MachineOwnerInfo");
        MachineOwnerInfo.get(level).addMachineInfo(pos, machineOwner, accOwner, id);
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
