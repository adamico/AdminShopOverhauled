package com.vnator.adminshop.blocks;

import com.vnator.adminshop.money.ClientMoneyData;
import com.vnator.adminshop.money.MoneyManager;
import com.vnator.adminshop.setup.Registration;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;

public class ShopContainer extends AbstractContainerMenu {

    private Player playerEntity;
    private IItemHandler playerInventory;

    public ShopContainer(int windowId, Inventory playerInventory, Player player){
        super(Registration.SHOP_CONTAINER.get(), windowId);
        this.playerEntity = player;
        this.playerInventory = new InvWrapper(playerInventory);

        layoutPlayerInventorySlots(16, 140);
        trackMoney();
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    private void trackMoney(){
        addDataSlot(new DataSlot() {
            @Override public int get() {return (int)(getPlayerBalance() & 0xFFFF);}
            @Override public void set(int value) {setPlayerBalance(value, 0);}
        });
        addDataSlot(new DataSlot() {
            @Override public int get() {return (int)((getPlayerBalance() >> 16) & 0xFFFF);}
            @Override public void set(int value) {setPlayerBalance(value, 1);}
        });
        addDataSlot(new DataSlot() {
            @Override public int get() {return (int)((getPlayerBalance() >> 32) & 0xFFFF);}
            @Override public void set(int value) {setPlayerBalance(value, 2);}
        });
        addDataSlot(new DataSlot() {
            @Override public int get() {return (int)((getPlayerBalance() >> 48) & 0xFFFF);}
            @Override public void set(int value) {setPlayerBalance(value, 3);}
        });
    }

    public long getPlayerBalance(){
        if(playerEntity.level.isClientSide()){
            return ClientMoneyData.getMoney();
        }
        return MoneyManager.get(playerEntity.level).getBalance(playerEntity.getStringUUID());
    }

    public void setPlayerBalance(int value, int index){
        long money = ClientMoneyData.getMoney();
        long change = value << (index*16);
        ClientMoneyData.setMoney(money | change);
    }

    private int addSlotRange(IItemHandler handler, int index, int x, int y, int amount, int dx) {
        for (int i = 0 ; i < amount ; i++) {
            addSlot(new SlotItemHandler(handler, index, x, y));
            x += dx;
            index++;
        }
        return index;
    }

    private int addSlotBox(IItemHandler handler, int index, int x, int y, int horAmount, int dx, int verAmount, int dy) {
        for (int j = 0 ; j < verAmount ; j++) {
            index = addSlotRange(handler, index, x, y, horAmount, dx);
            y += dy;
        }
        return index;
    }

    private void layoutPlayerInventorySlots(int leftCol, int topRow) {
        // Player inventory
        addSlotBox(playerInventory, 9, leftCol, topRow, 9, 18, 3, 18);

        // Hotbar
        topRow += 58;
        addSlotRange(playerInventory, 0, leftCol, topRow, 9, 18);
    }
}
