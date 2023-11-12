package com.ammonium.adminshop.client.gui;

import com.ammonium.adminshop.AdminShop;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class SetDefaultAccountButton extends Button {
    private final ResourceLocation GUI = new ResourceLocation(AdminShop.MODID, "textures/gui/shop_gui.png");
    private static final String name = "Change Account";

    public SetDefaultAccountButton(int x, int y, OnPress listener) {
        super(x, y, 50, 16, Component.literal(name), listener);
    }

    @Override
    public void render(@NotNull PoseStack matrix, int mouseX, int mouseY, float partialTicks) {
        if(!visible) {
            return;
        }
        RenderSystem.setShaderTexture(0, GUI);
        blit(matrix, x, y, 195, 129, 16, 16);

    }
}
