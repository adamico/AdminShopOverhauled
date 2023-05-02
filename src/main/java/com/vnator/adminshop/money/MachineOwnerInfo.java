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
    private final Map<BlockPos, Pair<String, Integer>> machineOwnerMap = new HashMap<>();

    public void addMachineOwner(BlockPos pos, String owner, int accid) {
        machineOwnerMap.put(pos, Pair.of(owner, accid));
        setDirty();
    }
    public Pair<String, Integer> getMachineOwner(BlockPos pos) {
        if (!machineOwnerMap.containsKey(pos)) {
            return null;
        }
        return machineOwnerMap.get(pos);
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
            machineOwnerMap.clear();
            ledger.forEach(ownerTag -> {
                int posx = tag.getInt("posx");
                int posy = tag.getInt("posy");
                int posz = tag.getInt("posz");
                String owner = tag.getString("owner");
                int accid = tag.getInt("accid");
                BlockPos pos = new BlockPos(posx, posy, posz);
                machineOwnerMap.put(pos, Pair.of(owner, accid));
            });
        }
    }
    @Override
    public @NotNull CompoundTag save(CompoundTag tag) {
        ListTag ledger = new ListTag();
        machineOwnerMap.forEach((pos, account) -> {
            CompoundTag ownertag = new CompoundTag();
            tag.putInt("posx", pos.getX());
            tag.putInt("posy", pos.getY());
            tag.putInt("posz", pos.getZ());
            tag.putString("owner", account.first);
            tag.putInt("accid", account.second);
            ledger.add(ownertag);
        });
        tag.put(COMPOUND_TAG_NAME, ledger);
        return tag;
    }
}
