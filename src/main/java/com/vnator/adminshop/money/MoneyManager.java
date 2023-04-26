package com.vnator.adminshop.money;

import com.vnator.adminshop.AdminShop;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MoneyManager extends SavedData {

    private static final String COMPOUND_TAG_NAME = "adminshop_ledger";

    private final List<BankAccount> accountList = new ArrayList<>();
    private final Map<String, Integer> accountsOwned = new HashMap<>();
    private final Map<String, Map<Integer, BankAccount>> sortedAccountMap = new HashMap<>();
    private final Map<String, List<BankAccount>> sharedAccounts = new HashMap<>();


    public Map<String, List<BankAccount>> getSharedAccounts() {
        return sharedAccounts;
    }

    public List<BankAccount> getAccountList() {
        return accountList;
    }

    //"Singleton" getter
    public static MoneyManager get(Level checkLevel){
        if(checkLevel.isClientSide()){
            throw new RuntimeException("Don't access this client-side!");
        }
        MinecraftServer serv = ServerLifecycleHooks.getCurrentServer();
        ServerLevel level = serv.getLevel(Level.OVERWORLD);
        assert level != null;
        DimensionDataStorage storage = level.getDataStorage();
        return storage.computeIfAbsent(MoneyManager::new, MoneyManager::new, "moneymanager");
    }

    public BankAccount getBankAccount(String player, int id) {
        if(!sortedAccountMap.containsKey(player) && id != 1) {
            AdminShop.LOGGER.error("User doesn't have any shared bank accounts!");
            return getBankAccount(player, 1);
        } else if(!sortedAccountMap.containsKey(player) && id == 1) {
            AdminShop.LOGGER.info("Creating personal bank account.");
            HashMap<Integer, BankAccount> newPlayerMap = new HashMap<>();
            BankAccount newAccount = new BankAccount(player);
            newPlayerMap.put(1, newAccount);
            sortedAccountMap.put(player, newPlayerMap);
            accountsOwned.put(player, 1);
            accountList.add(sortedAccountMap.get(player).get(id));
            setDirty();
        }
        return sortedAccountMap.get(player).get(id);
    }
    //Money getters/setters
    public long getBalance(String player){
        return  getBankAccount(player, 1).getBalance();
    }

    public long getBalance(String player, int id){
        return  getBankAccount(player, id).getBalance();
    }


    public boolean addBalance(String player, long amount){
        setDirty();
        return getBankAccount(player, 1).addBalance(amount);
    }
    public boolean addBalance(String player, int id, long amount){
        setDirty();
        return getBankAccount(player, id).addBalance(amount);
    }

    public boolean subtractBalance(String player, long amount){
        setDirty();
        return getBankAccount(player, 1).subtractBalance(amount);
    }
    public boolean subtractBalance(String player, int id, long amount){
        setDirty();
        return getBankAccount(player, id).subtractBalance(amount);
    }

    public boolean setBalance(String player, long amount){
        if(amount < 0) return false;
        getBankAccount(player, 1).setBalance(amount);
        setDirty();
        return true;
    }

    //Constructors
    public MoneyManager(){}

    public MoneyManager(CompoundTag tag){
        if (tag.contains(COMPOUND_TAG_NAME)) {
            ListTag ledger = tag.getList(COMPOUND_TAG_NAME, 10);
            accountList.clear();
            accountsOwned.clear();
            sortedAccountMap.clear();
            sharedAccounts.clear();
            ledger.forEach((accountTag) -> {
                BankAccount bankAccount = BankAccount.deserializeTag((CompoundTag) accountTag);
                accountList.add(bankAccount);

                // add to sorted accounts maps
                String owner = bankAccount.getOwner();
                int id = bankAccount.getId();
                if (accountsOwned.containsKey(owner)) {
                    accountsOwned.put(bankAccount.getOwner(), accountsOwned.get(owner) + 1);
                } else {
                    accountsOwned.put(owner, 1);
                }

                if (sortedAccountMap.containsKey(owner)) {
                    sortedAccountMap.get(owner).put(id, bankAccount);
                } else {
                    HashMap<Integer, BankAccount> newPlayerMap = new HashMap<>();
                    newPlayerMap.put(1, bankAccount);
                    sortedAccountMap.put(owner, newPlayerMap);
                }

                // create shared accounts list
                bankAccount.getMembers().forEach(member -> {
                    List<BankAccount> sharedAccountsList;
                    if (!sharedAccounts.containsKey(member)) {
                        sharedAccountsList = new ArrayList<>();

                    } else {
                        sharedAccountsList = sharedAccounts.get(member);
                    }
                    sharedAccountsList.add(bankAccount);
                    sharedAccounts.put(member, sharedAccountsList);
                });
            });
        }
    }

    @Override
    public @NotNull CompoundTag save(CompoundTag tag) {
        ListTag ledger = new ListTag();

        accountList.forEach((account) -> {
            CompoundTag bankAccountTag = account.serializeTag();
            ledger.add(bankAccountTag);
        });
        tag.put(COMPOUND_TAG_NAME, ledger);
        return tag;
    }
}
