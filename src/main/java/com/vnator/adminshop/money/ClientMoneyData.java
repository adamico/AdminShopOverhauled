package com.vnator.adminshop.money;

import com.ibm.icu.impl.Pair;
import com.vnator.adminshop.AdminShop;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;

public class ClientMoneyData {
    private static long money;
    private static Pair<String, Integer> personalAccount;
    private static List<Pair<String, Integer>> sharedAccountsList = new ArrayList<>();

    public ClientMoneyData() {
        assert Minecraft.getInstance().player != null;
        String playerUUID = Minecraft.getInstance().player.getStringUUID();
        personalAccount = Pair.of(playerUUID, 1);
    }

    public static void setSharedAccountsList(List<Pair<String, Integer>> newSharedAccountsList) {
        sharedAccountsList = newSharedAccountsList;
        if (!sharedAccountsList.contains(personalAccount)) {
            sharedAccountsList.add(personalAccount);
        }
    }

    public static List<Pair<String, Integer>> getSharedAccountsList() {
        return sharedAccountsList;
    }
    public void addAccount(BankAccount newAccount) {
        String ownerUUID = newAccount.getOwner();
        int accountID = newAccount.getId();
        addAccount(ownerUUID, accountID);
    }
    public void addAccount(String ownerUUID, int accountID) {
        sharedAccountsList.add(Pair.of(ownerUUID, accountID));
    }
    public boolean removeAccount(BankAccount removeAccount) {
        String ownerUUID = removeAccount.getOwner();
        Integer accountID = removeAccount.getId();
        return removeAccount(ownerUUID, accountID);
    }

    private boolean removeAccount(String ownerUUID, Integer accountID) {
        if(!sharedAccountsList.contains(Pair.of(ownerUUID, accountID))) {
            AdminShop.LOGGER.error("Couldn't find account in sharedAccountsList.");
            return false;
        } else {
            sharedAccountsList.remove(Pair.of(ownerUUID, accountID));
            return true;
        }
    }

    // TODO make this work with BankAccount-s
    public static void setMoney(long amt){
        money = amt;
    }

    // TODO make this work with BankAccount-s
    public static long getMoney(){
        return money;
    }

}
