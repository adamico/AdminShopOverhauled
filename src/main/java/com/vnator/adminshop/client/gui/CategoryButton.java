package com.vnator.adminshop.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.vnator.adminshop.AdminShop;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;

public class CategoryButton extends Button {

    private final ResourceLocation GUI = new ResourceLocation(AdminShop.MODID, "textures/gui/shop_gui.png");
    private boolean isSelected;
    private String name;

    public CategoryButton(int x, int y, String categoryName, OnPress listener) {
        super(x, y, 50, 16, new TextComponent(categoryName), listener);
        this.name = categoryName;
    }

    @Override
    public void renderButton(PoseStack matrix, int mouseX, int mouseY, float partialTicks) {
        if(!visible) {
            return;
        }

        RenderSystem.setShaderTexture(0, GUI);
        if(isSelected){
            this.blit(matrix, x, y, 195, 16, 50, 16);
        }else{
            this.blit(matrix, x, y, 195, 0, 50, 16);
        }
        drawCenteredString(matrix, Minecraft.getInstance().font, name, x+25,
                y+8-Minecraft.getInstance().font.lineHeight/2, 0xFFFFFF);

    }

    public void setSelected(boolean isSelected){
        this.isSelected = isSelected;
    }
}
