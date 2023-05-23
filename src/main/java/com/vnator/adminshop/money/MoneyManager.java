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

import java.util.*;

public class MoneyManager extends SavedData {

    private static final String COMPOUND_TAG_NAME = "adminshop_ledger";

    private static final int MAX_ACCOUNTS = 8;

    // SORTED SETS AND MAPS
    // The set of all BankAccounts
    private final Set<BankAccount> accountSet = new HashSet<>();

    // A map with ownerUUID and accountID
    private final Map<String, Integer> accountsOwned = new HashMap<>();

    // A map with ownerUUID and Map<accountID, BankAccount>
    private final Map<String, Map<Integer, BankAccount>> sortedAccountMap = new HashMap<>();

    // A map with playerUUID and a List of every BankAccount player is member or owner of
    private final Map<String, List<BankAccount>> sharedAccounts = new HashMap<>();

    public Map<String, List<BankAccount>> getSharedAccounts() {
        return sharedAccounts;
    }

    public Set<BankAccount> getAccountSet() {
        return accountSet;
    }

    public boolean removeSharedAccount(String playerUUID, String accOwner, int accID) {
        Optional<BankAccount> search = sharedAccounts.get(playerUUID).stream().filter(account -> (account.getOwner()
                .equals(accOwner) && account.getId() == accID)).findAny();
        if (search.isPresent()) {
            BankAccount result = search.get();
            sharedAccounts.get(playerUUID).remove(result);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Creates a new account for owner with given members, if owner still has free account slots.
     * @param owner the UUID of the account owner
     * @param members the set of the account's members' UUIDs, must contain the owner itself
     * @return the ID of the new account, or -1 if no account was created
     */
    public int CreateAccount(String owner, Set<String> members) {
        int newId = -1;
        // Find free account ID, -1 if not found
        for (int i = 1; i < (MAX_ACCOUNTS+1); i++) {
            if(!sortedAccountMap.get(owner).containsKey(i)) {
                newId = i;
                break;
            }
        }
        if (newId == -1) {
            AdminShop.LOGGER.error("Could not find free account ID for owner!");
            return newId;
        }
        // Create account
        return CreateAccount(owner, newId, members);
    }
    public int CreateAccount(String owner, int id) {
        Set<String> members = new HashSet<>();
        members.add(owner);
        return CreateAccount(owner, id, members);
    }
    public int CreateAccount(String owner, int id, Set<String> members) {
        // Check if members set contains owner
        if(!members.contains(owner)) {
            AdminShop.LOGGER.warn("Member set does not contain owner, adding.");
            members.add(owner);
        }
        // Check if owner has reached max accounts
        if (accountsOwned.containsKey(owner) && accountsOwned.get(owner) >= MAX_ACCOUNTS) {
            AdminShop.LOGGER.error("Owner has reached max accounts limit!");
            return -1;
        }
        // Create new account and add to relevant sets/maps
        BankAccount newAccount = new BankAccount(owner, id, members);
        accountSet.add(newAccount);
        // If first account, initialize maps
        if (!accountsOwned.containsKey(owner)) {
            accountsOwned.put(owner, 1);
        } else {
            accountsOwned.put(owner, accountsOwned.get(owner)+1);
        }
        // Add to sorted map
        if (!sortedAccountMap.containsKey(owner)) {
            sortedAccountMap.put(owner, new HashMap<>());
        }
        sortedAccountMap.get(owner).put(id, newAccount);
        newAccount.getMembers().forEach(member -> {
            // If first account, initialize map
            if (!sharedAccounts.containsKey(member)) {
                sharedAccounts.put(member, new ArrayList<>());
            }
            sharedAccounts.get(member).add(newAccount);
        });
        setDirty();
        // return new account ID
        return id;
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

    /**
     * Gets BankAccount given owner and ID
     * @param owner the owner UUID
     * @param id the account's ID
     * @return the specified BankAccount
     */
    public BankAccount getBankAccount(String owner, int id) {
        if(!sortedAccountMap.containsKey(owner) && id != 1) {
            AdminShop.LOGGER.error("User doesn't have any shared bank accounts!");
            return getBankAccount(owner, 1);
        } else if(!sortedAccountMap.containsKey(owner) && id == 1) {
            AdminShop.LOGGER.info("Creating personal bank account.");
            HashMap<Integer, BankAccount> newPlayerMap = new HashMap<>();
            BankAccount newAccount = new BankAccount(owner);
            newPlayerMap.put(1, newAccount);
            sortedAccountMap.put(owner, newPlayerMap);
            if (!accountsOwned.containsKey(owner)) {
                accountsOwned.put(owner, 1);
            } else {
                accountsOwned.put(owner, accountsOwned.get(owner)+1);
            }
            if (!sharedAccounts.containsKey(owner)) {
                sharedAccounts.put(owner, new ArrayList<>());
            }
            sharedAccounts.get(owner).add(newAccount);
            accountSet.add(sortedAccountMap.get(owner).get(id));
            setDirty();
        }
        return sortedAccountMap.get(owner).get(id);
    }

    /**
     * Deletes bank account from memory. Can't delete personal accounts (id 1)
     * @param owner the owner UUID
     * @param id the account's ID
     * @return true if successful, false if not
     */
    public boolean deleteBankAccount(String owner, int id) {
        // Check if trying to delete personal account
        if (id == 1) {
            AdminShop.LOGGER.error("Cannot delete personal account!");
            return false;
        }
        // Check if account exists
        if (!existsBankAccount(owner, id)) {
            AdminShop.LOGGER.error("Trying to delete an account which does not exist!");
            return false;
        }
        // Get account
        BankAccount toDelete = getBankAccount(owner, id);

        // Delete account from set and relevant maps
        accountSet.remove(toDelete);
        accountsOwned.put(owner, accountsOwned.get(owner) - 1);
        sortedAccountMap.get(owner).remove(id);
        toDelete.getMembers().forEach(member -> sharedAccounts.get(member).remove(toDelete));
        return true;
    }

    /**
     * Checks if said bank account exists
     * @param owner Owner UUID
     * @param id Account ID
     * @return true if account exists, false otherwise
     */
    public boolean existsBankAccount(String owner, int id) {
        return sortedAccountMap.containsKey(owner) && sortedAccountMap.get(owner).containsKey(id);
    }

    /**
     * Adds member to bank account
     * @param owner owner UUID
     * @param id account ID
     * @param memberUUID new member UUID
     * @return true if added, false otherwise
     */
    public boolean addMember(String owner, int id, String memberUUID) {
        // Check if account exists
        if (!existsBankAccount(owner, id)) {
            AdminShop.LOGGER.error("Can't add member to account that doesn't exist");
            return false;
        }
        // Get bank account
        BankAccount oldBankAccount = getBankAccount(owner, id);
        // Check if account already has member
        if (oldBankAccount.getMembers().contains(memberUUID)) {
            AdminShop.LOGGER.error("Bank account already has member!");
            return false;
        }
        // Add member to account
        boolean success = oldBankAccount.addMember(memberUUID);
        if (!success) {
            AdminShop.LOGGER.error("Error adding member.");
            return false;
        }

        AdminShop.LOGGER.info("Succesfully removed member from bank account.");
        return true;
    }

    /**
     * Removes member from bank account
     * @param owner owner UUID
     * @param id account ID
     * @param memberUUID member UUID to remove
     * @return true if removed, false otherwise
     */
    public boolean removeMember(String owner, int id, String memberUUID) {
        // Check if account exists
        if (!existsBankAccount(owner, id)) {
            AdminShop.LOGGER.error("Can't remove member from account that doesn't exist!");
            return false;
        }
        // Get bank account
        BankAccount bankAccount = getBankAccount(owner, id);
        // Check if account doesn't have member
        if (!bankAccount.getMembers().contains(memberUUID)) {
            AdminShop.LOGGER.error("Bank account doesn't have member!");
            return false;
        }
        // Check if trying to remove owner
        if (bankAccount.getOwner().equals(memberUUID)) {
            AdminShop.LOGGER.error("Can't remove owner from account!");
            return false;
        }
        // Remove member from account
        boolean success = bankAccount.removeMember(memberUUID);
        if (!success) {
            AdminShop.LOGGER.error("Error removing member.");
            return false;
        }

        AdminShop.LOGGER.info("Succesfully removed member from bank account.");
        return true;
    }

    //Money getters/setters

    /**
     * Gets player's personal account balance
     * @param player the player's UUID
     * @return the player's personal account balance
     * @deprecated Use getBalance(String player, Int id) instead
     */
    @Deprecated
    public long getBalance(String player){
        AdminShop.LOGGER.warn("getBalance(String player) is deprecated.");
        return  getBankAccount(player, 1).getBalance();
    }

    public long getBalance(String player, int id){
        return  getBankAccount(player, id).getBalance();
    }


    @Deprecated
    public boolean addBalance(String player, long amount){
        setDirty();
        return getBankAccount(player, 1).addBalance(amount);
    }
    public boolean addBalance(String player, int id, long amount){
        setDirty();
        return getBankAccount(player, id).addBalance(amount);
    }

    @Deprecated
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
            accountSet.clear();
            accountsOwned.clear();
            sortedAccountMap.clear();
            sharedAccounts.clear();
            ledger.forEach((accountTag) -> {
                BankAccount bankAccount = BankAccount.deserializeTag((CompoundTag) accountTag);
                accountSet.add(bankAccount);

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

        accountSet.forEach((account) -> {
            CompoundTag bankAccountTag = account.serializeTag();
            ledger.add(bankAccountTag);
        });
        tag.put(COMPOUND_TAG_NAME, ledger);
        return tag;
    }
}
