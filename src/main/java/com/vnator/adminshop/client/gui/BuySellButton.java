package com.vnator.adminshop.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.vnator.adminshop.AdminShop;
import com.vnator.adminshop.shop.ShopItem;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;

/**
 * To swap between Buy and Sell modes
 */
public class BuySellButton extends Button {

    private final ResourceLocation GUI = new ResourceLocation(AdminShop.MODID, "textures/gui/shop_gui.png");
    private static final int TYPE_BUY = 0;
    private static final int TYPE_SELL = 1;

    private boolean isBuy;

    public BuySellButton(int x, int y, String buyText, String sellText, boolean isBuy, OnPress listener) {
        super(x, y, 50, 12, new TextComponent(""), listener);
        this.isBuy = isBuy;
    }

    /**
     * Switches buy and sell mode
     * @return Whether in Buy mode. false = sell mode
     */
    public boolean switchBuySell(){
        isBuy = !isBuy;
        return isBuy;
    }

    @Override
    public void renderButton(PoseStack matrix, int mouseX, int mouseY, float partialTicks) {
        if(!visible) {
            return;
        }

        RenderSystem.setShaderTexture(0, GUI);
        if(isBuy){
            this.blit(matrix, x, y,195, 68, 50, 12);
        }else{
            this.blit(matrix, x, y, 195, 56, 50, 12);
        }
    }
}
