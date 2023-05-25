package com.ammonium.adminshop.network;

import org.apache.commons.lang3.tuple.Pair;
import com.ammonium.adminshop.AdminShop;
import com.ammonium.adminshop.money.BankAccount;
import com.ammonium.adminshop.money.MoneyManager;
import com.ammonium.adminshop.setup.Messages;
import com.ammonium.adminshop.shop.Shop;
import com.ammonium.adminshop.shop.ShopItem;
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

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import static java.lang.Math.ceil;

public class PacketPurchaseRequest {

    private final boolean isBuy;
    private final int quantity;
    private final String accOwner;
    private final int accID;
    private final ShopItem item; // final

    public PacketPurchaseRequest(BankAccount bankAccount, boolean isBuy, ShopItem item, int quantity){
        this.accOwner = bankAccount.getOwner();
        this.accID = bankAccount.getId();
        this.isBuy = isBuy;
        this.item = item;
        this.quantity = quantity;
    }

    public PacketPurchaseRequest(Pair<String, Integer> bankAccount, boolean isBuy, ShopItem item, int quantity){
        this.accOwner = bankAccount.getKey();
        this.accID = bankAccount.getValue();
        this.isBuy = isBuy;
        this.item = item;
        this.quantity = quantity;
    }

    public PacketPurchaseRequest(String owner, int ownerId, boolean isBuy, ShopItem item, int quantity){
        this.accOwner = owner;
        this.accID = ownerId;
        this.isBuy = isBuy;
        this.item = item;
        this.quantity = quantity;
    }

    public PacketPurchaseRequest(FriendlyByteBuf buf){
        this.accOwner = buf.readUtf();
        this.accID = buf.readInt();
        this.isBuy = buf.readBoolean();
        int shopItemIndex = buf.readInt();
        List<ShopItem> shopItemList = isBuy ? Shop.get().getShopStockBuy() : Shop.get().getShopStockSell();
        this.item = shopItemList.get(shopItemIndex);
        this.quantity = buf.readInt();
    }

    public void toBytes(FriendlyByteBuf buf){
        buf.writeUtf(accOwner);
        buf.writeInt(accID);
        buf.writeBoolean(isBuy);
        List<ShopItem> shopItemList = isBuy ? Shop.get().getShopStockBuy() : Shop.get().getShopStockSell();
        buf.writeInt(shopItemList.indexOf(item));
        buf.writeInt(quantity);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier){
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            //Client side accessed here
            //Do NOT call client-only code though, since server needs to access this too
            System.out.println("Perform purchase on: "+isBuy+" , "+item.getItem().getDisplayName().getString()+" , "+quantity);

            System.out.println("Item: "+item.getItem().getDisplayName().getString());

            if (isBuy) {
                buyTransaction(supplier, item, quantity);
            } else {
                sellTransaction(supplier, item, quantity);
            }

            // Sync money with affected clients
            AdminShop.LOGGER.info("Syncing money with clients");
            ServerPlayer player = ctx.getSender();
            assert player != null;

            // Get current bank account
            MoneyManager moneyManager = MoneyManager.get(player.getLevel());
            BankAccount currentAccount = moneyManager.getBankAccount(this.accOwner, this.accID);

            // Sync money with bank account's members
            assert currentAccount.getMembers().contains(this.accOwner);
            currentAccount.getMembers().forEach(memberUUID -> {
                List<BankAccount> usableAccounts = moneyManager.getSharedAccounts().get(memberUUID);
                Messages.sendToPlayer(new PacketSyncMoneyToClient(usableAccounts), (ServerPlayer) player.getLevel()
                        .getPlayerByUUID(UUID.fromString(memberUUID)));
            });
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
            }
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
