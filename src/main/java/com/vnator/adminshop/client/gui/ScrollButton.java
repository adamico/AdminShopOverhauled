package com.vnator.adminshop.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.vnator.adminshop.AdminShop;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;


public class ScrollButton extends Button {

    private final ResourceLocation GUI = new ResourceLocation(AdminShop.MODID, "textures/gui/shop_gui.png");
    private boolean isUp;

    public ScrollButton(int x, int y, boolean isUp, OnPress listener){
        super(x, y, 16, 16, new TextComponent(""), listener);
    }

    @Override
    public void renderButton(PoseStack matrix, int mouseX, int mouseY, float partialTicks) {
        //super.renderButton(matrix, mouseX, mouseY, partialTicks);
        if(!visible) {
            return;
        }

        RenderSystem.setShaderTexture(0, GUI);
        //TODO add button textures. Use background arrows for now
    }
}
