package com.vnator.adminshop.money;

import com.ibm.icu.impl.Pair;
import com.vnator.adminshop.AdminShop;
import com.vnator.adminshop.setup.Config;
import net.minecraft.client.Minecraft;

import java.util.*;

public class ClientMoneyData {
    private static final Pair<String, Integer> personalAccount;

    static {
        assert Minecraft.getInstance().player != null;
        personalAccount = Pair.of(Minecraft.getInstance().player.getStringUUID(), 1);
    }

    private static Set<Pair<String, Integer>> sharedAccountsSet = new HashSet<>();
    private static Map<Pair<String, Integer>, Long> accountBalanceMap = new HashMap<>();


    public static void setSharedAccounts(Set<Pair<String, Integer>> newSharedAccountsSet,
                                         Map<Pair<String, Integer>, Long> newAccountBalanceMap) {
        sharedAccountsSet = newSharedAccountsSet;
        accountBalanceMap = newAccountBalanceMap;
        sharedAccountsSet.add(personalAccount);
        if (!accountBalanceMap.containsKey(personalAccount)) {
            accountBalanceMap.put(personalAccount, Config.STARTING_MONEY.get());
        }
    }

    public static Set<Pair<String, Integer>> getSharedAccountsSet() {
        return sharedAccountsSet;
    }

    public static void setAccountBalanceMap(Map<Pair<String, Integer>, Long> newAccountBalanceMap) {
        accountBalanceMap = newAccountBalanceMap;
    }

    public static Map<Pair<String, Integer>, Long> getAccountBalanceMap() {
        return accountBalanceMap;
    }

    public void addAccount(BankAccount newAccount) {
        String ownerUUID = newAccount.getOwner();
        int accountID = newAccount.getId();
        addAccount(Pair.of(ownerUUID, accountID), newAccount.getBalance());
    }
    public void addAccount(String ownerUUID, int accountID, long balance) {
        addAccount(Pair.of(ownerUUID, accountID), balance);
    }
    public void addAccount(Pair<String, Integer> bankAccount, long balance) {
        sharedAccountsSet.add(bankAccount);
        accountBalanceMap.put(bankAccount, balance);
    }
    public boolean removeAccount(BankAccount removeAccount) {
        String ownerUUID = removeAccount.getOwner();
        Integer accountID = removeAccount.getId();
        return removeAccount(Pair.of(ownerUUID, accountID));
    }

    private boolean removeAccount(String ownerUUID, Integer accountID) {
        return removeAccount(Pair.of(ownerUUID, accountID));
    }

    private boolean removeAccount(Pair<String, Integer> bankAccount) {
        if(!sharedAccountsSet.contains(bankAccount)) {
            AdminShop.LOGGER.error("Couldn't find account in sharedAccountsList.");
            return false;
        } else {
            sharedAccountsSet.remove(bankAccount);
        }
        if (!accountBalanceMap.containsKey(bankAccount)) {
            AdminShop.LOGGER.error("Couldn't find account in accountBalanceMap.");
            return false;
        } else {
            accountBalanceMap.remove(bankAccount);
        }
        return true;
    }

    public static void setMoney(long amt){
        AdminShop.LOGGER.warn("setMoney(long) is deprecated.");
        setMoney(personalAccount, amt);
    }
    public static void setMoney(BankAccount bankAccount, long amt) {
        setMoney(Pair.of(bankAccount.getOwner(), bankAccount.getId()), amt);
    }
    public static void setMoney(String ownerUUID, int accID, long amt) {
        setMoney(Pair.of(ownerUUID, accID), amt);
    }
    public static void setMoney(Pair<String, Integer> bankAccount, long amt) {
        accountBalanceMap.put(bankAccount, amt);
    }

    public static long getMoney(){
        AdminShop.LOGGER.warn("getMoney() is deprecated.");
        return getMoney(personalAccount);
    }
    public static long getMoney(BankAccount bankAccount) {
        return getMoney(Pair.of(bankAccount.getOwner(), bankAccount.getId()));
    }
    public static long getMoney(String ownerUUID, int accID) {
        return getMoney(Pair.of(ownerUUID, accID));
    }
    public static long getMoney(Pair<String, Integer> bankAccount) {
        if (!accountBalanceMap.containsKey(bankAccount)) {
            AdminShop.LOGGER.warn("Could not find bankAccount in accountBalanceMap, adding it");
            AdminShop.LOGGER.warn("OwnerUUID:"+personalAccount.first+", accID:"+personalAccount.second);
            if (bankAccount.equals(personalAccount)) {
                accountBalanceMap.put(bankAccount, Config.STARTING_MONEY.get());
            } else {
                accountBalanceMap.put(bankAccount, 0L);
            }
            return 0;
        }
        return accountBalanceMap.get(bankAccount);
    }

}
