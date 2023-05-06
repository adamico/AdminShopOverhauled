package com.vnator.adminshop.network;

import com.vnator.adminshop.AdminShop;
import com.vnator.adminshop.blocks.AutoShopMachine;
import com.vnator.adminshop.money.BankAccount;
import com.vnator.adminshop.money.BuyerTargetInfo;
import com.vnator.adminshop.money.MachineOwnerInfo;
import com.vnator.adminshop.money.MoneyManager;
import com.vnator.adminshop.setup.Messages;
import com.vnator.adminshop.shop.ShopItem;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.network.NetworkEvent;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import static java.lang.Math.ceil;

public class PacketBuyerTransaction {
    private BlockPos pos;
    private int buySize;

    public PacketBuyerTransaction(BlockPos pos, int buySize) {
        this.pos = pos;
        this.buySize = buySize;
    }
    public PacketBuyerTransaction(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
        this.buySize = buf.readInt();
    }
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBlockPos(this.pos);
        buf.writeInt(this.buySize);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier){
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            //Client side accessed here
            //Do NOT call client-only code though, since server needs to access this too
            System.out.println("Processing buyer transaction for "+this.pos+", "+this.buySize);
            BlockEntity blockEntity = ctx.getSender().getLevel().getBlockEntity(this.pos);
            if (!(blockEntity instanceof AutoShopMachine buyerEntity)) {
                System.out.println("Is not a seller!");
                return;
            }
            // item logic
            // Attempt to insert the items, and only perform transaction on what can fit
            ServerLevel level = ctx.getSender().getLevel();
            MoneyManager moneyManager = MoneyManager.get(level);
            MachineOwnerInfo machineOwnerInfo = MachineOwnerInfo.get(level);
            BuyerTargetInfo buyerTargetInfo = BuyerTargetInfo.get(level);
            ShopItem shopItem = buyerTargetInfo.getBuyerTarget(this.pos);
            Item item = shopItem.getItem().getItem();
            ItemStack toInsert = new ItemStack(item);
            toInsert.setCount(buySize);
            ItemStackHandler handler = buyerEntity.getItemHandler();
            ItemStack returned = ItemHandlerHelper.insertItemStacked(handler, toInsert, true);
            if(returned.getCount() == buySize) {
                return;
            }
            int itemCost = shopItem.getPrice();
            long price = (long) ceil((buySize - returned.getCount()) * itemCost);
            // Get MoneyManager and attempt transaction
            Pair<String, Integer> account = machineOwnerInfo.getMachineAccount(this.pos);
            String accOwner = account.getKey();
            int accID = account.getValue();
            // Check if account has enough money, if not reduce amount
            long balance = moneyManager.getBalance(accOwner, accID);
            if (price > balance) {
                if (itemCost > balance) {
                    // not enough money to buy one
                    return;
                }
                // Find max amount he can buy
                buySize = (int) (balance / itemCost);
                price = (long) ceil(buySize * itemCost);
                toInsert.setCount(buySize);
            }
            boolean success = moneyManager.subtractBalance(accOwner, accID, price);
            if (success) {
                ItemHandlerHelper.insertItemStacked(handler, toInsert, false);
                System.out.println("Bought item");
            } else {
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
                Messages.sendToPlayer(new PacketSyncMoneyToClient(usableAccounts), (ServerPlayer) level.
                        getPlayerByUUID(UUID.fromString(memberUUID)));
            });

        });
        return true;
    }
}
