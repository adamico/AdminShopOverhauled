package com.vnator.adminshop.money;

import com.ibm.icu.impl.Pair;
import com.vnator.adminshop.AdminShop;
import com.vnator.adminshop.setup.Config;
import net.minecraft.client.Minecraft;

import java.util.*;

public class ClientMoneyData {

    private static final Set<BankAccount> accountSet = new HashSet<>();
    private static final Map<Pair<String, Integer>, BankAccount> accountMap = new HashMap<>();

    public static Set<BankAccount> getAccountSet() {
        return accountSet;
    }

    public static Map<Pair<String, Integer>, BankAccount> getAccountMap() {
        return accountMap;
    }

    public static void setAccountSet(Set<BankAccount> accountSet) {
        ClientMoneyData.accountSet.clear();
        ClientMoneyData.accountSet.addAll(accountSet);
        ClientMoneyData.accountMap.clear();
        ClientMoneyData.accountSet.forEach(account -> {
            ClientMoneyData.accountMap.put(Pair.of(account.getOwner(), account.getId()), account);
        });
    }

    public static BankAccount addAccount(BankAccount newAccount) {
        if (accountMap.containsKey(Pair.of(newAccount.getOwner(), newAccount.getId()))) {
            AdminShop.LOGGER.warn("newAccount already in accountMap.");
            return accountMap.get(Pair.of(newAccount.getOwner(), newAccount.getId()));
        }
        ClientMoneyData.accountSet.add(newAccount);
        ClientMoneyData.accountMap.put(Pair.of(newAccount.getOwner(), newAccount.getId()), newAccount);
        return newAccount;
    }


    public static boolean removeAccount(BankAccount toRemove) {
        if (!accountSet.contains(toRemove)) {
            AdminShop.LOGGER.warn("removeAccount not in accountSet");
            return false;
        }
        accountSet.remove(toRemove);
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
            AdminShop.LOGGER.warn("Could not find bankAccount in accountSet, adding it.");
            AdminShop.LOGGER.warn("OwnerUUID:" + bankAccount.first + ", accID:" + bankAccount.second);
            result = ClientMoneyData.addAccount(new BankAccount(bankAccount.first, bankAccount.second));
        } else {
            result = accountMap.get(bankAccount);
        }
        assert bankAccount != null;
        return result.getBalance();
    }

}
