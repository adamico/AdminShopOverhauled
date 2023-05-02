package com.vnator.adminshop.blocks;

import net.minecraft.world.MenuProvider;

public interface MachineWithOwnerAndAccount extends MenuProvider {
    void setAccOwnerUUID(String accOwnerUUID);
    void setAccID(int accID);

    void setAccInfo(String accOwner, int accId);

    void setAccInfo(String machineOwner, String accOwner, int accId);

    void setMachineOwnerUUID(String machineOwnerUUID);

    String getMachineOwnerUUID();

    String getAccOwnerUUID();

    int getAccID();

}
