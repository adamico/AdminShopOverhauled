package com.vnator.adminshop.network;

import com.ibm.icu.impl.Pair;
import com.vnator.adminshop.AdminShop;
import com.vnator.adminshop.money.BankAccount;
import com.vnator.adminshop.money.MoneyManager;
import com.vnator.adminshop.setup.Messages;
import com.vnator.adminshop.shop.Shop;
import com.vnator.adminshop.shop.ShopItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.wrapper.PlayerMainInvWrapper;
import net.minecraftforge.network.NetworkEvent;

import java.util.*;
import java.util.function.Supplier;

import static java.lang.Math.ceil;

public class PacketPurchaseRequest {

    private final boolean isBuy;
    private final int category;
    private final int itemIndex;
    private final int quantity;
    private final String accOwner;
    private final int accID;

    public PacketPurchaseRequest(BankAccount bankAccount, boolean isBuy, int category, int itemIndex, int quantity){
        this.accOwner = bankAccount.getOwner();
        this.accID = bankAccount.getId();
        this.isBuy = isBuy;
        this.category = category;
        this.itemIndex = itemIndex;
        this.quantity = quantity;
    }
    public PacketPurchaseRequest(Pair<String, Integer> bankAccount, boolean isBuy, int category, int itemIndex, int quantity){
        this.accOwner = bankAccount.first;
        this.accID = bankAccount.second;
        this.isBuy = isBuy;
        this.category = category;
        this.itemIndex = itemIndex;
        this.quantity = quantity;
    }

    public PacketPurchaseRequest(String owner, int ownerId, boolean isBuy, int category, int itemIndex, int quantity){
        this.accOwner = owner;
        this.accID = ownerId;
        this.isBuy = isBuy;
        this.category = category;
        this.itemIndex = itemIndex;
        this.quantity = quantity;
    }

    public PacketPurchaseRequest(FriendlyByteBuf buf){
        accOwner = buf.readUtf();
        accID = buf.readInt();
        isBuy = buf.readBoolean();
        category = buf.readInt();
        itemIndex = buf.readInt();
        quantity = buf.readInt();
    }

    public void toBytes(FriendlyByteBuf buf){
        buf.writeUtf(accOwner);
        buf.writeInt(accID);
        buf.writeBoolean(isBuy);
        buf.writeInt(category);
        buf.writeInt(itemIndex);
        buf.writeInt(quantity);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier){
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            //Client side accessed here
            //Do NOT call client-only code though, since server needs to access this too
            System.out.println("Perform purchase on: "+isBuy+" , "+category+" , "+itemIndex+" , "+quantity);
            ShopItem shopItem = isBuy ? Shop.get().shopStockBuy.get(category).get(itemIndex) : Shop.get().shopStockSell
                    .get(category).get(itemIndex);

            System.out.println("Item: "+shopItem.getItem().getDisplayName().getString());

            if (isBuy) {
                buyTransaction(supplier, shopItem, quantity);
            } else {
                sellTransaction(supplier, shopItem, quantity);
            }

            // Sync money with client
            AdminShop.LOGGER.info("Syncing money with client");
            ServerPlayer player = ctx.getSender();
            assert player != null;

            MoneyManager moneyManager = MoneyManager.get(player.getLevel());
            Set<BankAccount> accountSet = moneyManager.getAccountSet();
            Messages.sendToPlayer(new PacketSyncMoneyToClient(accountSet), player);

        });
        return true;
    }

    private void buyTransaction(Supplier<NetworkEvent.Context> supplier, ShopItem item, int quantity) {
        // IItemHandler inventory = player.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
        NetworkEvent.Context ctx = supplier.get();
        ServerPlayer player = ctx.getSender();
        // Get item handler
        assert player != null;
        Inventory playerInventory = player.getInventory();
        LazyOptional<IItemHandler> mainInventoryHandler = LazyOptional.of(() -> new PlayerMainInvWrapper(playerInventory));
        mainInventoryHandler.ifPresent(iItemHandler -> {
            if(item.isItem()) {
                // item logic
                // Attempt to insert the items, and only perform transaction on what can fit
                AdminShop.LOGGER.info("Buying Item");
                ItemStack toInsert = item.getItem().copy();
                toInsert.setCount(quantity);
                ItemStack returned = ItemHandlerHelper.insertItemStacked(iItemHandler, toInsert, true);
                if(returned.getCount() == quantity) {
                    player.sendMessage(new TextComponent("Not enough inventory space for item!"), player.getUUID());
                }
                int itemCost = item.getPrice();
                long price = (long) ceil((quantity - returned.getCount()) * itemCost);


                boolean success = MoneyManager.get(player.getLevel()).subtractBalance(accOwner, accID, price);
                if (success) {
                    ItemHandlerHelper.insertItemStacked(iItemHandler, toInsert, false);
                } else {
                    player.sendMessage(new TextComponent("Not enough money in account!"), player.getUUID());
                    AdminShop.LOGGER.error("Not enough money in account to perform transaction.");
                }
            } // else {
                // TODO fluid logic
            // }
        });
    }

    private void sellTransaction(Supplier<NetworkEvent.Context> supplier, ShopItem item, int quantity) {
        NetworkEvent.Context ctx = supplier.get();
        ServerPlayer player = ctx.getSender();
        assert player != null;
        Inventory playerInventory = player.getInventory();
        LazyOptional<IItemHandler> mainInventoryHandler = LazyOptional.of(() -> new PlayerMainInvWrapper(playerInventory));
        mainInventoryHandler.ifPresent(iItemHandler -> {
            ItemStack toRemove = item.getItem().copy();
            toRemove.setCount(quantity);
            int numSold = removeItemsFromInventory(iItemHandler, toRemove);
            int itemCost = item.getPrice();
            long price = (long) numSold * itemCost;
            if (numSold == 0) {
                player.sendMessage(new TextComponent("No matching item found."), player.getUUID());
                AdminShop.LOGGER.error("No matching item found.");
                return;
            }

            boolean success = MoneyManager.get(player.getLevel()).addBalance(accOwner, accID, price);
            if (success) {
                AdminShop.LOGGER.info("Sold item.");
            } else {
                AdminShop.LOGGER.error("Error selling item.");
            }
        });
    }

    private boolean itemstacksEqual(ItemStack a, ItemStack b){
        if(a.getItem() == b.getItem() && a.getDamageValue() == b.getDamageValue()) {
            CompoundTag atag = a.getOrCreateTag();
            CompoundTag btag = b.getOrCreateTag();
            if(atag == btag) {
                return true;
            } else {
                return a.getOrCreateTag().equals(b.getOrCreateTag());
            }
        }
        return false;
    }

    /**
     * Removes the equivalent parameter itemstack from the inventory (item and count)
     * @param inv Inventory to remove from
     * @param item ItemStack that represents what is to be removed. item.count() = number of items to be removed
     * @return Number of items actually removed.
     */
    private int removeItemsFromInventory(IItemHandler inv, ItemStack item) {
        int count = item.getCount();
        System.out.println("Removing items from inventory! # to remove: "+count);
        for(int i = 0; i < inv.getSlots(); i++){
            ItemStack comp = inv.getStackInSlot(i);
            if(itemstacksEqual(comp, item)){ //Found items we can remove
                System.out.println("Found item at slot "+i);
                if(count > comp.getCount()){ //Remove entirety of this stack
                    count -= comp.getCount();
                    inv.extractItem(i, comp.getCount(), false);
                }else{ //Remove what is left of stack, return number removed
                    inv.extractItem(i, count, false);
                    System.out.println("Removed count: "+item.getCount());
                    return item.getCount();
                }
            }
        }

        //Removed as much as possible, return count
        System.out.println("Removed count: "+(item.getCount() - count));
        return item.getCount() - count;
    }

}
