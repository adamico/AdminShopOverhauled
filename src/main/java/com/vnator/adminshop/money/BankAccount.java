package com.vnator.adminshop.money;

import com.vnator.adminshop.AdminShop;
import com.vnator.adminshop.setup.Config;
import net.minecraft.nbt.CompoundTag;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class BankAccount {
    public static final int MAX_MEMBERS = 8;
    private String owner;
    private Set<String> members;
    private int id;
    private long balance;

    public BankAccount() { }

    // Create personal account (id: 1)
    public BankAccount(String own) {
        this.owner = own;
        this.members = new HashSet<>();
        this.members.add(own);
        this.id = 1;
        this.balance = Config.STARTING_MONEY.get();
    }

    public BankAccount(String own, Set<String> mem, int nid) {
        if(nid == 1){
            throw new RuntimeException("Can't set shared bank account id to 1!");
        }
        this.owner = own;
        this.members = mem;
        this.id = nid;
        this.balance = 0;
    }

    public CompoundTag serializeTag() {
        CompoundTag tag = new CompoundTag();
        tag.putString("owner", owner);
        tag.putString("members", String.join(";", members));
        tag.putInt("id", id);
        tag.putLong("balance", balance);
        return tag;
    }

    public static BankAccount deserializeTag(CompoundTag tag) {
        BankAccount bankAccount = new BankAccount();
        bankAccount.setOwner(tag.getString("owner"));
        String joinedMembers = tag.getString("members");
        Set<String> origMembers = new HashSet<>(Arrays.asList(joinedMembers.split(";")));
        bankAccount.setMembers(origMembers);
        bankAccount.setId(tag.getInt("id"));
        bankAccount.setBalance(tag.getLong("balance"));
        return bankAccount;
    }

    public long getBalance() {
        return balance;
    }
    public void setBalance(long balance) {
        this.balance = balance;
    }
    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }
    public String getOwner() {
        return owner;
    }
    private void setOwner(String owner) {
        this.owner = owner;
    }
    public Set<String> getMembers() {
        return members;
    }
    public void setMembers(Set<String> members) {
        this.members = members;
    }

    public boolean addMember(String newMember) {
        if (id == 1) {
            AdminShop.LOGGER.error("Can't add members to personal account!");
            return false;
        }
        if (members.contains(newMember)) {
            AdminShop.LOGGER.error("Bank account already contains member!");
            return false;
        }
        if (members.size() >= MAX_MEMBERS) {
            AdminShop.LOGGER.error("Bank account at max member size!");
            return false;
        }
        members.add(newMember);
        AdminShop.LOGGER.info("Added "+newMember+" to bank account.");
        return true;
    }

    public boolean removeMember(String toRemove) {
        if (Objects.equals(toRemove, owner)) {
            AdminShop.LOGGER.error("Can't remove owner from his bank account");
            return false;
        }
        if (!members.contains(toRemove)) {
            AdminShop.LOGGER.error("Player is not a member of this bank account!");
            return false;
        }
        members.remove(toRemove);
        AdminShop.LOGGER.info("Removed "+toRemove+" from bank account.");
        return true;
    }

    public boolean addBalance(long add) {
        this.balance += add;
        return true;
    }

    public boolean subtractBalance(long sub) {
        if (this.balance < sub) {
            return false;
        }

        this.balance -= sub;
        return true;
    }
}
