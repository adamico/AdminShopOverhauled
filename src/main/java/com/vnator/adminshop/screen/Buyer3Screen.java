package com.vnator.adminshop.screen;

import org.apache.commons.lang3.tuple.Pair;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.vnator.adminshop.AdminShop;
import com.vnator.adminshop.client.gui.ChangeAccountButton;
import com.vnator.adminshop.money.BankAccount;
import com.vnator.adminshop.money.ClientLocalData;
import com.vnator.adminshop.network.MojangAPI;
import com.vnator.adminshop.network.PacketMachineAccountChange;
import com.vnator.adminshop.network.PacketSetBuyerTarget;
import com.vnator.adminshop.setup.Messages;
import com.vnator.adminshop.shop.Shop;
import com.vnator.adminshop.shop.ShopItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Buyer3Screen extends AbstractContainerScreen<Buyer3Menu> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(AdminShop.MODID, "textures/gui/buyer_3.png");
    private final BlockPos blockPos;

    private final String ownerUUID;
    private ChangeAccountButton changeAccountButton;
    private final List<BankAccount> usableAccounts = new ArrayList<>();
    // -1 if bankAccount is not in usableAccounts
    private int usableAccountsIndex;

    public Buyer3Screen(Buyer3Menu pMenu, Inventory pPlayerInventory, Component pTitle, BlockPos blockPos) {
        super(pMenu, pPlayerInventory, pTitle);
        this.blockPos = blockPos;
        Pair<String, Integer> bankAccount = ClientLocalData.getMachineAccount(this.blockPos);
        this.ownerUUID = ClientLocalData.getMachineOwner(this.blockPos);
        this.usableAccounts.clear();
        this.usableAccounts.addAll(ClientLocalData.getUsableAccounts());
        Optional<BankAccount> search = this.usableAccounts.stream().filter(account ->
                bankAccount.equals(Pair.of(account.getOwner(), account.getId()))).findAny();
        if (search.isEmpty()) {
            AdminShop.LOGGER.warn("Player does not have access to this buyer!");
            this.usableAccountsIndex = -1;
        } else {
            BankAccount result = search.get();
            this.usableAccountsIndex = this.usableAccounts.indexOf(result);
        }
    }

    private Pair<String, Integer> getBankAccount() {
        return Pair.of(this.usableAccounts.get(this.usableAccountsIndex).getOwner(),
                this.usableAccounts.get(this.usableAccountsIndex).getId());
    }

    private void createChangeAccountButton(int x, int y) {
        if(changeAccountButton != null) {
            removeWidget(changeAccountButton);
        }
        changeAccountButton = new ChangeAccountButton(x+119, y+62, (b) -> {
            Player player = Minecraft.getInstance().player;
            assert player != null;
            // Check if player is the owner
            if (!player.getStringUUID().equals(ownerUUID)) {
                player.sendMessage(new TextComponent("You are not the owner of this machine!"), player.getUUID());
                return;
            }
            // Change accounts
            changeAccounts();
            Minecraft.getInstance().player.sendMessage(new TextComponent("Changed account to "+
                    MojangAPI.getUsernameByUUID(getBankAccount().getKey())+":"+getBankAccount().getValue()),
                    Minecraft.getInstance().player.getUUID());
        });
        addRenderableWidget(changeAccountButton);
    }

    private void changeAccounts() {
        // Check if bankAccount was in usableAccountsIndex
        if (this.usableAccountsIndex == -1) {
            AdminShop.LOGGER.error("BankAccount is not in usableAccountsIndex");
            return;
        }
        // Refresh usable accounts
        BankAccount bankAccount = usableAccounts.get(usableAccountsIndex);
        this.usableAccounts.clear();
        this.usableAccounts.addAll(ClientLocalData.getUsableAccounts());
        // Change account, either by resetting to first (personal) account or moving to next sorted account
        if (!this.usableAccounts.contains(bankAccount)) {
            this.usableAccountsIndex = 0;
        } else {
            this.usableAccountsIndex = (this.usableAccounts.indexOf(bankAccount) + 1) % this.usableAccounts.size();
        }
        // Send change package
        System.out.println("Registering account change with server...");
        Messages.sendToServer(new PacketMachineAccountChange(this.ownerUUID, getBankAccount().getKey(),
                getBankAccount().getValue(), this.blockPos));
    }
    @Override
    protected void init() {
        super.init();
        int relX = (this.width - this.imageWidth) / 2;
        int relY = (this.height - this.imageHeight) / 2;
        createChangeAccountButton(relX, relY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        Slot slot = this.getSlotUnderMouse();
        if (slot != null) {
            ItemStack itemStack = slot.getItem();
            boolean isMachineSlot = slot.index >= this.menu.getTeInventoryFirstSlotIndex() && slot.index < this.menu
                    .getTeInventoryFirstSlotIndex() + this.menu.getTeInventorySlotCount();
            if (!itemStack.isEmpty() && !isMachineSlot) {
                // Get item clicked on
                Item item = itemStack.getItem();
                System.out.println("Clicked on item "+item.getRegistryName());
                // Check if item is in buy map
                if (Shop.get().getShopBuyMap().containsKey(item)) {
                    System.out.println("Item is in buy map");
                    ShopItem shopItem = Shop.get().getShopBuyMap().get(item);
                    Messages.sendToServer(new PacketSetBuyerTarget(this.blockPos, shopItem.getItem().getItem()
                            .getRegistryName()));
                    ClientLocalData.addBuyerTarget(this.blockPos, shopItem);
                    return false;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void renderBg(PoseStack pPoseStack, float pPartialTicks, int pMouseX, int pMouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, TEXTURE);
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        this.blit(pPoseStack, x, y, 0, 0, imageWidth, imageHeight);
        if (ClientLocalData.hasTarget(this.blockPos)) {
            renderItem(pPoseStack, ClientLocalData.getBuyerTarget(this.blockPos).getItem().getItem(), x+104, y+14);
        }
    }

    @Override
    protected void renderLabels(PoseStack pPoseStack, int pMouseX, int pMouseY) {
        super.renderLabels(pPoseStack, pMouseX, pMouseY);
        drawString(pPoseStack, font, MojangAPI.getUsernameByUUID(getBankAccount().getKey())+":"+getBankAccount()
                        .getValue(),7,62,0xffffff);
    }

    @Override
    public void render(PoseStack pPoseStack, int mouseX, int mouseY, float delta) {
        renderBackground(pPoseStack);
        super.render(pPoseStack, mouseX, mouseY, delta);
        renderTooltip(pPoseStack, mouseX, mouseY);
    }

    private void renderItem(PoseStack matrixStack, Item item, int x, int y) {
        ItemRenderer itemRenderer = this.minecraft.getItemRenderer();
        ItemStack itemStack = new ItemStack(item);
        itemRenderer.renderAndDecorateFakeItem(itemStack, x, y);
    }
}
