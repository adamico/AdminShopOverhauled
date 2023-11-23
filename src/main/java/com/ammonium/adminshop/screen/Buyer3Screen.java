package com.ammonium.adminshop.screen;

import com.ammonium.adminshop.AdminShop;
import com.ammonium.adminshop.blocks.entity.Buyer3BE;
import com.ammonium.adminshop.client.gui.ChangeAccountButton;
import com.ammonium.adminshop.money.BankAccount;
import com.ammonium.adminshop.money.ClientLocalData;
import com.ammonium.adminshop.network.MojangAPI;
import com.ammonium.adminshop.network.PacketMachineAccountChange;
import com.ammonium.adminshop.network.PacketSetBuyerTarget;
import com.ammonium.adminshop.network.PacketUpdateRequest;
import com.ammonium.adminshop.setup.Messages;
import com.ammonium.adminshop.shop.Shop;
import com.ammonium.adminshop.shop.ShopItem;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Buyer3Screen extends AbstractContainerScreen<Buyer3Menu> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(AdminShop.MODID, "textures/gui/buyer_3.png");
    private final BlockPos blockPos;
    private Buyer3BE buyerEntity;
    private String ownerUUID;
    private Pair<String, Integer> account;
    private ShopItem shopTarget;
    private ChangeAccountButton changeAccountButton;
    private final List<Pair<String, Integer>> usableAccounts = new ArrayList<>();
    // -1 if bankAccount is not in usableAccounts
    private int usableAccountsIndex;

    public Buyer3Screen(Buyer3Menu pMenu, Inventory pPlayerInventory, Component pTitle, BlockPos blockPos) {
        super(pMenu, pPlayerInventory, pTitle);
        this.blockPos = blockPos;
    }

    private Pair<String, Integer> getAccountDetails() {
        if (usableAccountsIndex == -1 || usableAccountsIndex >= this.usableAccounts.size()) {
            AdminShop.LOGGER.error("Account isn't properly set!");
            return this.usableAccounts.get(0);
        }
        return this.usableAccounts.get(this.usableAccountsIndex);
    }

    private BankAccount getBankAccount() {
        return ClientLocalData.getAccountMap().get(getAccountDetails());
    }

    private void createChangeAccountButton(int x, int y) {
        if(changeAccountButton != null) {
            removeWidget(changeAccountButton);
        }
        changeAccountButton = new ChangeAccountButton(x+119, y+62, (b) -> {
            Minecraft mc = Minecraft.getInstance();
            Player player = mc.player;
            assert player != null;
            // Check if player is the owner
            if (!player.getStringUUID().equals(ownerUUID)) {
                player.sendSystemMessage(Component.literal("You are not the owner of this machine!"));
                return;
            }
            // Change accounts
            changeAccounts();
            Minecraft newMc = Minecraft.getInstance();
            Player newPlayer = newMc.player;
            assert newPlayer != null;
            newPlayer.sendSystemMessage(Component.literal("Changed account to "+
                MojangAPI.getUsernameByUUID(getAccountDetails().getKey())+":"+ getAccountDetails().getValue()));
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
        Pair<String, Integer> bankAccount = usableAccounts.get(usableAccountsIndex);
        List<Pair<String, Integer>> localAccountData = new ArrayList<>();
        ClientLocalData.getUsableAccounts().forEach(account -> localAccountData.add(Pair.of(account.getOwner(),
                account.getId())));
        if (!this.usableAccounts.equals(localAccountData)) {
            this.usableAccounts.clear();
            this.usableAccounts.addAll(localAccountData);
        }
        // Change account, either by resetting to first (personal) account or moving to next sorted account
        if (!this.usableAccounts.contains(bankAccount)) {
            this.usableAccountsIndex = 0;
        } else {
            this.usableAccountsIndex = (this.usableAccounts.indexOf(bankAccount) + 1) % this.usableAccounts.size();
        }
        // Send change packet
//        System.out.println("Registering account change with server...");
        Messages.sendToServer(new PacketMachineAccountChange(this.ownerUUID, getAccountDetails().getKey(),
                getAccountDetails().getValue(), this.blockPos));
    }
    @Override
    protected void init() {
        super.init();
        int relX = (this.width - this.imageWidth) / 2;
        int relY = (this.height - this.imageHeight) / 2;
        createChangeAccountButton(relX, relY);

        // Request update from server
//        System.out.println("Requesting update from server");
        Messages.sendToServer(new PacketUpdateRequest(this.blockPos));
    }
    
    private void updateInformation() {
        this.ownerUUID = this.buyerEntity.getOwnerUUID();
        this.account = this.buyerEntity.getAccount();
        this.shopTarget = this.buyerEntity.getTargetShopItem();

        this.usableAccounts.clear();
        ClientLocalData.getUsableAccounts().forEach(account -> this.usableAccounts.add(Pair.of(account.getOwner(),
                account.getId())));
        Optional<Pair<String, Integer>> search = this.usableAccounts.stream().filter(baccount ->
                this.account.equals(Pair.of(baccount.getKey(), baccount.getValue()))).findAny();
        if (search.isEmpty()) {
            AdminShop.LOGGER.error("Player does not have access to this seller!");
            this.usableAccountsIndex = -1;
        } else {
            Pair<String, Integer> result = search.get();
            this.usableAccountsIndex = this.usableAccounts.indexOf(result);
        }
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
                AdminShop.LOGGER.debug("Clicked on item "+itemStack.getDisplayName().getString());
                // Check if item with NBT is in buy map
                ItemStack singleStack = itemStack.copy();
                itemStack.setCount(1);
                boolean isShopItem = Shop.get().hasBuyShopItemNBT(singleStack);
                ShopItem shopItem = null;
                if (isShopItem) {
                    shopItem = Shop.get().getShopBuyItemNBT(singleStack);
                } else {
                    // Check if item w/o NBT is in buy map
                    isShopItem = Shop.get().hasBuyShopItem(itemStack.getItem());
                    if (isShopItem) {
                        shopItem = Shop.get().getBuyShopItem(itemStack.getItem());
                    }
                }
                // Return super if not in buy map
                if (!isShopItem || shopItem == null) {
                    AdminShop.LOGGER.debug("Item not in buy map: "+itemStack.getDisplayName().getString());
                    return super.mouseClicked(mouseX, mouseY, button);
                }
                // Set buyer target
                // Check if account has permit to buy item
                if (getBankAccount().hasPermit(shopItem.getPermitTier())) {
                    this.buyerEntity.setTargetShopItem(this.shopTarget);
                    this.shopTarget = shopItem;
                    Messages.sendToServer(new PacketSetBuyerTarget(this.blockPos, this.shopTarget));
                    return false;
                } else {
                    Minecraft mc = Minecraft.getInstance();
                    LocalPlayer player = mc.player;
                    assert player != null;
                    player.sendSystemMessage(Component.literal("You haven't unlocked that yet!"));
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float pPartialTicks, int pMouseX, int pMouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, TEXTURE);
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        guiGraphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);
        if (this.shopTarget != null) {
            renderItem(guiGraphics, this.shopTarget.getItem(), x+104, y+14);
            if (this.shopTarget.hasNBT()) {
                guiGraphics.drawString(font, "+NBT", x+104-font.width("+NBT")-1, y+14, 0xFF55FF);
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int pMouseX, int pMouseY) {
        super.renderLabels(guiGraphics, pMouseX, pMouseY);
        if (this.usableAccounts == null || this.usableAccountsIndex == -1 || this.usableAccountsIndex >=
                this.usableAccounts.size()) {
            return;
        }
        Pair<String, Integer> account = getAccountDetails();
        boolean accAvailable = this.usableAccountsIndex != -1 && ClientLocalData.accountAvailable(account.getKey(),
                account.getValue());
        int color = accAvailable ? 0xffffff : 0xff0000;
        guiGraphics.drawString(font, MojangAPI.getUsernameByUUID(account.getKey())+":"+ account.getValue(),
                7,62,color);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, delta);
        renderTooltip(guiGraphics, mouseX, mouseY);

        // Get data from BlockEntity
        this.buyerEntity = this.getMenu().getBlockEntity();

        String buyerOwnerUUID = this.buyerEntity.getOwnerUUID();
        Pair<String, Integer> buyerAccount = this.buyerEntity.getAccount();
        ShopItem buyerShopTarget = this.buyerEntity.getTargetShopItem();

        boolean shouldUpdateDueToNulls = (this.ownerUUID == null && buyerOwnerUUID != null) ||
                (this.account == null && buyerAccount != null) ||
                (this.shopTarget == null && buyerShopTarget != null);

        boolean shouldUpdateDueToDifferences = (this.ownerUUID != null && !this.ownerUUID.equals(buyerOwnerUUID)) ||
                (this.account != null && !this.account.equals(buyerAccount)) ||
                (this.shopTarget != buyerShopTarget);

        if (shouldUpdateDueToNulls || shouldUpdateDueToDifferences) {
            updateInformation();
        }
    }

    private void renderItem(GuiGraphics guiGraphics, ItemStack itemStack, int x, int y) {
        guiGraphics.renderFakeItem(itemStack, x, y);
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        guiGraphics.renderItemDecorations(font, itemStack, x, y);
    }
}
