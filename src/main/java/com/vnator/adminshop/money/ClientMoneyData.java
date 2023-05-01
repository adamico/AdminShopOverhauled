package com.vnator.adminshop.money;

import com.ibm.icu.impl.Pair;
import com.vnator.adminshop.AdminShop;
import net.minecraft.core.BlockPos;

import java.util.*;

public class ClientMoneyData {

    // Only contains the list of accounts it is either an owner or member on
    private static final List<BankAccount> usableAccounts = new ArrayList<>();
    private static final Map<Pair<String, Integer>, BankAccount> accountMap = new HashMap<>();
    private static final Map<BlockPos, Pair<String, Integer>> machineOwnerMap = new HashMap<>();

    public static List<BankAccount> getUsableAccounts() {
        return usableAccounts;
    }

    public static Map<Pair<String, Integer>, BankAccount> getAccountMap() {
        return accountMap;
    }

    public static void setUsableAccounts(List<BankAccount> usableAccounts) {
        ClientMoneyData.usableAccounts.clear();
        ClientMoneyData.usableAccounts.addAll(usableAccounts);
        ClientMoneyData.accountMap.clear();
        ClientMoneyData.usableAccounts.forEach(account -> {
            ClientMoneyData.accountMap.put(Pair.of(account.getOwner(), account.getId()), account);
        });
    }

    public static void setMachineOwnerMap(Map<BlockPos, Pair<String, Integer>> machineOwnerMap) {
        ClientMoneyData.machineOwnerMap.clear();
        ClientMoneyData.machineOwnerMap.putAll(machineOwnerMap);
    }

    public static void addMachineOwnership(BlockPos pos, String owner, int id) {
        machineOwnerMap.put(pos, Pair.of(owner, id));
    }

    public static Pair<String, Integer> getMachineOwnership(BlockPos pos) {
        return machineOwnerMap.get(pos);
    }

    public static BankAccount addAccount(BankAccount newAccount) {
        if (accountMap.containsKey(Pair.of(newAccount.getOwner(), newAccount.getId()))) {
            AdminShop.LOGGER.warn("newAccount already in accountMap.");
            return accountMap.get(Pair.of(newAccount.getOwner(), newAccount.getId()));
        }
        ClientMoneyData.usableAccounts.add(newAccount);
        ClientMoneyData.accountMap.put(Pair.of(newAccount.getOwner(), newAccount.getId()), newAccount);
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
            result = ClientMoneyData.addAccount(new BankAccount(bankAccount.first, bankAccount.second));
        } else {
            result = accountMap.get(bankAccount);
        }
        assert bankAccount != null;
        return result.getBalance();
    }

}
