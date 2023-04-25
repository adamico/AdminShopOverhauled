package com.vnator.adminshop.money;

import com.vnator.adminshop.setup.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.HashMap;
import java.util.Map;

public class MoneyManager extends SavedData {

    private static final String COMPOUND_TAG_NAME = "adminshop_ledger";
    private Map<String, Long> moneyMap = new HashMap<>();

    //"Singleton" getter
    public static MoneyManager get(Level checkLevel){
        if(checkLevel.isClientSide()){
            throw new RuntimeException("Don't access this client-side!");
        }
        MinecraftServer serv = ServerLifecycleHooks.getCurrentServer();
        Level level = serv.getLevel(Level.OVERWORLD);
        DimensionDataStorage storage = ((ServerLevel) level).getDataStorage();
        return storage.computeIfAbsent(MoneyManager::new, MoneyManager::new, "moneymanager");
    }

    //Money getters/setters
    public long getBalance(String player){
        if(!moneyMap.containsKey(player)) {
            moneyMap.put(player, Config.STARTING_MONEY.get());
        }
        return moneyMap.get(player);
    }

    public boolean addBalance(String player, long amount){
        moneyMap.put(player, getBalance(player)+amount);
        setDirty();
        return true;
    }

    public boolean subtractBalance(String player, long amount){
        long balance = getBalance(player);
        if(balance < amount) {
            return false;
        }

        moneyMap.put(player, balance-amount);
        setDirty();
        return true;
    }

    public boolean setBalance(String player, long amount){
        if(amount < 0) return false;
        moneyMap.put(player, amount);
        setDirty();
        return true;
    }

    //Constructors
    public MoneyManager(){}

    public MoneyManager(CompoundTag tag){
        CompoundTag ledger = tag.getCompound(COMPOUND_TAG_NAME);
        moneyMap.clear();
        ledger.getAllKeys().forEach(k -> moneyMap.put(k, ledger.getLong(k)));
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        CompoundTag ledger = new CompoundTag();
        moneyMap.forEach((player, money) -> ledger.putLong(player, money));
        tag.put(COMPOUND_TAG_NAME, ledger);
        return tag;
    }
}
