package com.vnator.adminshop.money;

import com.ibm.icu.impl.Pair;
import com.vnator.adminshop.AdminShop;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClientLocalData {

    // Only contains the list of accounts it is either an owner or member on
    private static final List<BankAccount> usableAccounts = new ArrayList<>();
    private static final Map<Pair<String, Integer>, BankAccount> accountMap = new HashMap<>();

    public static List<BankAccount> getUsableAccounts() {
        return usableAccounts;
    }
    private static final Map<BlockPos, Pair<String, Integer>> machineOwnerMap = new HashMap<>();
    private static final Map<String, String> uuidToNameMap = new HashMap<>();

    public static void addMachineOwner(BlockPos pos, Pair<String, Integer> owner) {
        machineOwnerMap.put(pos, owner);
    }
    public static void addUuidToNameMap(String uuid, String name) {
        uuidToNameMap.put(uuid, name);
    }
    public static String getNameFromUUID(String uuid) {
        return uuidToNameMap.get(uuid);
    }
    public static Pair<String, Integer> getMachineOwner(BlockPos pos) {
        return machineOwnerMap.get(pos);
    }
    public static boolean machineOwnerReady(BlockPos pos) {
        return machineOwnerMap.containsKey(pos);
    }

    public static Map<Pair<String, Integer>, BankAccount> getAccountMap() {
        return accountMap;
    }

    public static void setUsableAccounts(List<BankAccount> usableAccounts) {
        ClientLocalData.usableAccounts.clear();
        ClientLocalData.usableAccounts.addAll(usableAccounts);
        ClientLocalData.accountMap.clear();
        ClientLocalData.usableAccounts.forEach(account -> {
            ClientLocalData.accountMap.put(Pair.of(account.getOwner(), account.getId()), account);
        });
    }

    public static BankAccount addAccount(BankAccount newAccount) {
        if (accountMap.containsKey(Pair.of(newAccount.getOwner(), newAccount.getId()))) {
            AdminShop.LOGGER.warn("newAccount already in accountMap.");
            return accountMap.get(Pair.of(newAccount.getOwner(), newAccount.getId()));
        }
        ClientLocalData.usableAccounts.add(newAccount);
        ClientLocalData.accountMap.put(Pair.of(newAccount.getOwner(), newAccount.getId()), newAccount);
        return newAccount;
    }


    public static boolean removeAccount(BankAccount toRemove) {
        if (!usableAccounts.contains(toRemove)) {
            AdminShop.LOGGER.warn("removeAccount not in usableAccounts");
            return false;
        }
        usableAccounts.remove(toRemove);
        accountMap.remove(Pair.of(toRemove.getOwner(), toRemove.getId()));
        return true;
    }

    public static void setMoney(BankAccount bankAccount, long amt) {
        bankAccount.setBalance(amt);
    }

    public static long getMoney(Pair<String, Integer> bankAccount) {
//        if (!accountSet.contains(bankAccount)) {
        BankAccount result;
        if (!accountMap.containsKey(bankAccount)) {
            AdminShop.LOGGER.warn("Could not find bankAccount in usableAccounts, adding it.");
            AdminShop.LOGGER.warn("OwnerUUID:" + bankAccount.first + ", accID:" + bankAccount.second);
            result = ClientLocalData.addAccount(new BankAccount(bankAccount.first, bankAccount.second));
        } else {
            result = accountMap.get(bankAccount);
        }
        assert bankAccount != null;
        return result.getBalance();
    }

}
