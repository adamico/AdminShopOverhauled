package com.ammonium.adminshop.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.ammonium.adminshop.AdminShop;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;

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
        this.blit(matrix, x, y, 195, 113, 50, 16);

        drawSmallerCenteredString(matrix, Minecraft.getInstance().font, name, x+25,
                y+10-Minecraft.getInstance().font.lineHeight/2, 0xFFFFFF);

    }

    public void drawSmallerCenteredString(PoseStack matrix, Font font, String text, int x, int y, int color) {
        float scale = 0.62f; // Adjust this value to set the desired font size

        matrix.pushPose(); // Save the current pose

        // Apply the scaling
        matrix.scale(scale, scale, scale);

        // Calculate the new x and y positions based on the scaling
        int scaledX = (int) (x / scale);
        int scaledY = (int) (y / scale);

        // Draw the centered string with the new positions
        font.draw(matrix, text, (float) (scaledX - font.width(text) / 2), (float) scaledY, color);

        matrix.popPose(); // Restore the saved pose
    }
}
