package com.vnator.adminshop.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.vnator.adminshop.AdminShop;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;

import static net.minecraft.client.gui.GuiComponent.drawCenteredString;

public class ChangeAccountButton extends Button {
    private final ResourceLocation GUI = new ResourceLocation(AdminShop.MODID, "textures/gui/shop_gui.png");
    private static final String name = "Change Account";

    public ChangeAccountButton(int x, int y, Button.OnPress listener) {
        super(x, y, 50, 16, new TextComponent(name), listener);
    }

    @Override
    public void renderButton(PoseStack matrix, int mouseX, int mouseY, float partialTicks) {
        if(!visible) {
            return;
        }

        RenderSystem.setShaderTexture(0, GUI);
        this.blit(matrix, x, y, 195, 0, 50, 16);

        drawCenteredString(matrix, Minecraft.getInstance().font, name, x+25,
                y+8-Minecraft.getInstance().font.lineHeight/2, 0xFFFFFF);

    }
}
