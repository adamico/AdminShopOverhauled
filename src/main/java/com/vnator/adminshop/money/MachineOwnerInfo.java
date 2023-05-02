package com.vnator.adminshop.money;

import com.ibm.icu.impl.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class MachineOwnerInfo extends SavedData {

    private final String COMPOUND_TAG_NAME = "adminshop_machineownership";
    private final Map<BlockPos, Pair<String, Integer>> machineAccountMap = new HashMap<>();
    private final Map<BlockPos, String> machineOwnerMap = new HashMap<>();

    public void addMachineInfo(BlockPos pos, String machineOwner, String accowner, int accid) {
        machineOwnerMap.put(pos, machineOwner);
        machineAccountMap.put(pos, Pair.of(accowner, accid));
        setDirty();
    }
    public String getMachineOwner(BlockPos pos) {
        return machineOwnerMap.get(pos);
    }
    public Pair<String, Integer> getMachineAccount(BlockPos pos) {
        if (!machineAccountMap.containsKey(pos)) {
            return null;
        }
        return machineAccountMap.get(pos);
    }

    public static MachineOwnerInfo get(Level checkLevel){
        if(checkLevel.isClientSide()){
            throw new RuntimeException("Don't access this client-side!");
        }
        MinecraftServer serv = ServerLifecycleHooks.getCurrentServer();
        ServerLevel level = serv.getLevel(Level.OVERWORLD);
        assert level != null;
        DimensionDataStorage storage = level.getDataStorage();
        return storage.computeIfAbsent(MachineOwnerInfo::new, MachineOwnerInfo::new, "machineownership");
    }

    //Constructors
    public MachineOwnerInfo(){}

    public MachineOwnerInfo(CompoundTag tag){
        if (tag.contains(COMPOUND_TAG_NAME)) {
            ListTag ledger = tag.getList(COMPOUND_TAG_NAME, 10);
            machineAccountMap.clear();
            ledger.forEach(ownerTag -> {
                int posx = tag.getInt("posx");
                int posy = tag.getInt("posy");
                int posz = tag.getInt("posz");
                String machineOwner = tag.getString("machineowner");
                String accowner = tag.getString("accowner");
                int accid = tag.getInt("accid");
                BlockPos pos = new BlockPos(posx, posy, posz);
                machineOwnerMap.put(pos, machineOwner);
                machineAccountMap.put(pos, Pair.of(accowner, accid));
            });
        }
    }
    @Override
    public @NotNull CompoundTag save(CompoundTag tag) {
        ListTag ledger = new ListTag();
        machineAccountMap.forEach((pos, account) -> {
            CompoundTag ownertag = new CompoundTag();
            tag.putInt("posx", pos.getX());
            tag.putInt("posy", pos.getY());
            tag.putInt("posz", pos.getZ());
            tag.putString("machineowner", machineOwnerMap.get(pos));
            tag.putString("accowner", account.first);
            tag.putInt("accid", account.second);
            ledger.add(ownertag);
        });
        tag.put(COMPOUND_TAG_NAME, ledger);
        return tag;
    }
}
