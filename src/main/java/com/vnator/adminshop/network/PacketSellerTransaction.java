package com.vnator.adminshop.network;

import com.vnator.adminshop.AdminShop;
import com.vnator.adminshop.blocks.entity.SellerBE;
import com.vnator.adminshop.money.BankAccount;
import com.vnator.adminshop.money.MachineOwnerInfo;
import com.vnator.adminshop.money.MoneyManager;
import com.vnator.adminshop.setup.Messages;
import com.vnator.adminshop.shop.Shop;
import com.vnator.adminshop.shop.ShopItem;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.network.NetworkEvent;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
public class PacketSellerTransaction {
    private BlockPos pos;

    public PacketSellerTransaction(BlockPos pos) {
        this.pos = pos;
    }
    public PacketSellerTransaction(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
    }
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBlockPos(this.pos);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier){
        NetworkEvent.Context ctx = supplier.get();
        ctx.enqueueWork(() -> {
            //Client side accessed here
            //Do NOT call client-only code though, since server needs to access this too
            System.out.println("Processing seller transaction for "+this.pos);
            BlockEntity blockEntity = ctx.getSender().getLevel().getBlockEntity(this.pos);
            if (!(blockEntity instanceof SellerBE sellerEntity)) {
                System.out.println("Is not a seller!");
                return;
            }
            ItemStackHandler itemHandler = sellerEntity.getItemHandler();
            Item item = itemHandler.getStackInSlot(0).getItem();
            int count = itemHandler.getStackInSlot(0).getCount();
            itemHandler.extractItem(0, count, false);
            ShopItem shopItem = Shop.get().getShopSellMap().get(item);
            int itemCost = shopItem.getPrice();
            long price = (long) count * itemCost;
            if (count == 0) {
                return;
            }
            // Get local MoneyManager and attempt transaction
            ServerLevel level = ctx.getSender().getLevel();
            MoneyManager moneyManager = MoneyManager.get(level);
            MachineOwnerInfo machineOwnerInfo = MachineOwnerInfo.get(level);
            Pair<String, Integer> machineAccount = machineOwnerInfo.getMachineAccount(this.pos);
            String accOwner = machineAccount.getKey();
            int accID = machineAccount.getValue();
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
                Messages.sendToPlayer(new PacketSyncMoneyToClient(usableAccounts), (ServerPlayer) level.
                        getPlayerByUUID(UUID.fromString(memberUUID)));
            });

        });
        return true;
    }
}
