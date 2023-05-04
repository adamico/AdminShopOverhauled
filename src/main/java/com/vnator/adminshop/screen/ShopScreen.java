package com.vnator.adminshop.screen;

import com.ibm.icu.impl.Pair;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.vnator.adminshop.AdminShop;
import com.vnator.adminshop.blocks.ShopBlock;
import com.vnator.adminshop.blocks.ShopContainer;
import com.vnator.adminshop.client.gui.BuySellButton;
import com.vnator.adminshop.client.gui.ChangeAccountButton;
import com.vnator.adminshop.client.gui.ShopButton;
import com.vnator.adminshop.money.BankAccount;
import com.vnator.adminshop.money.ClientLocalData;
import com.vnator.adminshop.network.MojangAPI;
import com.vnator.adminshop.network.PacketPurchaseRequest;
import com.vnator.adminshop.setup.Messages;
import com.vnator.adminshop.shop.Shop;
import com.vnator.adminshop.shop.ShopItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class ShopScreen extends AbstractContainerScreen<ShopContainer> {
    private final ResourceLocation GUI = new ResourceLocation(AdminShop.MODID, "textures/gui/shop_gui.png");
    private final String GUI_BUY = "gui.buy";
    private final String GUI_SELL = "gui.sell";
    private final String GUI_MONEY = "gui.money_message";
    private static final int NUM_ROWS = 4, NUM_COLS = 9;
    private static final int SHOP_BUTTON_X = 16;
    private static final int SHOP_BUTTON_Y = 33;
    private static final int SHOP_BUTTON_SIZE = 18;
    private int rows_passed = 0;
    private final ShopContainer shopContainer;
    private final List<ShopButton> buyButtons;
    private final List<ShopButton> sellButtons;
    private boolean isBuy; //Whether the Buy option is currently selected
    private Map<Pair<String, Integer>, BankAccount> accountMap;
    private final List<BankAccount> usableAccounts = new ArrayList<>();
    private int usableAccountsIndex;
    private ChangeAccountButton changeAccountButton;
    private BuySellButton buySellButton;
    private EditBox searchBar;
    private int tickCounter = 0;
    private String search = "";

    public ShopScreen(ShopContainer container, Inventory inv, Component name) {
        super(container, inv, name);

        assert Minecraft.getInstance().player != null;
        assert Minecraft.getInstance().level != null;
        assert Minecraft.getInstance().level.isClientSide;

        String playerUUID = Minecraft.getInstance().player.getStringUUID();
        Pair<String, Integer> personalAccount = Pair.of(playerUUID, 1);
        this.accountMap = ClientLocalData.getAccountMap();

        if (!this.accountMap.containsKey(personalAccount)) {
            AdminShop.LOGGER.warn("Couldn't find personal account, creating one.");
            AdminShop.LOGGER.warn(personalAccount.first+":"+personalAccount.second);
            BankAccount personalBankAccount = ClientLocalData.addAccount(new BankAccount(personalAccount.first,
                    personalAccount.second));
            // Refresh account map
            this.accountMap = ClientLocalData.getAccountMap();
        }
        this.usableAccounts.clear();
        this.usableAccounts.addAll(ClientLocalData.getUsableAccounts());
        this.usableAccountsIndex = 0;

        this.shopContainer = container;
        this.imageWidth = 195;
        this.imageHeight = 222;
        this.tickCounter = 0;
        this.search = "";

        buyButtons = new ArrayList<>();
        sellButtons = new ArrayList<>();

        isBuy = true;
    }

//    @SuppressWarnings("resource")
    @Override
    protected void init() {
        super.init();
        int relX = (this.width - this.imageWidth) / 2;
        int relY = (this.height - this.imageHeight) / 2;
        createShopButtons(true, relX, relY);
        createShopButtons(false, relX, relY);
        createBuySellButton(relX, relY);
        createChangeAccountButton(relX, relY);
        createSearchBar(relX, relY);
        refreshShopButtons();
    }

    @Override
    public void render(@NotNull PoseStack matrixStack, int mouseX, int mouseY, float partialTicks){
        this.renderBackground(matrixStack);
        super.render(matrixStack, mouseX, mouseY, partialTicks);
        this.searchBar.render(matrixStack, mouseX, mouseY, partialTicks);
        this.renderTooltip(matrixStack, mouseX, mouseY);
        this.tickCounter++;
        if (this.tickCounter > 20) {
            this.tickCounter = 0;
            filterSearch();
        }
    }

    private void filterSearch() {
        String currSearch = searchBar.getValue();
        if (this.search.equals(currSearch)) {
            return;
        }
        this.search = currSearch;
        int relX = (this.width - this.imageWidth) / 2;
        int relY = (this.height - this.imageHeight) / 2;
        createShopButtons(this.isBuy, relX, relY);
        refreshShopButtons();
    }

    @Override
    protected void renderLabels(PoseStack matrixStack, int mouseX, int mouseY) {
        matrixStack.pushPose();
        //Block Title
        String blockName = I18n.get(ShopBlock.SCREEN_ADMINSHOP_SHOP);
        drawCenteredString(matrixStack, font, blockName, getXSize()/2, 6, 0xffffff);

        //Player Inventory Title
        drawString(matrixStack, font, playerInventoryTitle, 16, getYSize()-94, 0xffffff);

        //Player Balance
        BankAccount selectedAccount = this.usableAccounts.get(this.usableAccountsIndex);
        Pair<String, Integer> selectedAccountInfo = Pair.of(selectedAccount.getOwner(), selectedAccount.getId());
        drawString(matrixStack, Minecraft.getInstance().font,
                I18n.get(GUI_MONEY) + ClientLocalData.getMoney(selectedAccountInfo),
                getXSize() - font.width(I18n.get(GUI_MONEY) + "00000000") - 4,
                6, 0xffffff); //x, y, color

        // Bank account
        drawString(matrixStack, font, MojangAPI.getUsernameByUUID(selectedAccountInfo.first)+":"+
                selectedAccountInfo.second,16,108,0xffffff);

        //Tooltip for item the player is hovering over
        List<ShopButton> shopButtons = getVisibleShopButtons();
        Optional<ShopButton> button = shopButtons.stream().filter(b -> b.isMouseOn).findFirst();
        button.ifPresent(shopButton -> {
            renderTooltip(matrixStack, shopButton.getTooltipContent(),
                    Optional.empty(), mouseX-(this.width - this.imageWidth)/2,
                    mouseY-(this.height - this.imageHeight)/2);
        });
        matrixStack.popPose();

    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        assert minecraft != null;
        Slot slot = this.getSlotUnderMouse();
        if (slot != null && Screen.hasShiftDown()) {
            ItemStack itemStack = slot.getItem();
            if (!itemStack.isEmpty()) {
                // Get item clicked on
                Item item = itemStack.getItem();
                // Check if item is in sell map
                if (Shop.get().getShopSellMap().containsKey(item)) {
                    ShopItem shopItem = Shop.get().getShopSellMap().get(item);
                    // Attempt to sell it
                    attemptTransaction(this.usableAccounts.get(this.usableAccountsIndex), false, shopItem, itemStack.getCount());
                    return false;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void renderBg(@NotNull PoseStack matrixStack, float partialTicks, int mouseX, int mouseY){
        RenderSystem.setShaderTexture(0, GUI);
        int relX = (this.width - this.imageWidth) / 2;
        int relY = (this.height - this.imageHeight) / 2;
        this.blit(matrixStack, relX, relY, 0, 0, this.imageWidth, this.imageHeight);
    }
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.searchBar.keyPressed(keyCode, scanCode, modifiers) || this.searchBar.canConsumeInput()) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (this.searchBar.charTyped(codePoint, modifiers)) {
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }


    private void createShopButtons(boolean isBuy, int x, int y){
        List<ShopItem> shopItems = isBuy ? Shop.get().getShopStockBuy() : Shop.get().getShopStockSell();
        List<ShopButton> shopButtons = isBuy ? buyButtons : sellButtons;
        // Filter by search if it is set
        if (!this.search.equals("")) {
            shopItems = shopItems.stream().filter(shopItem -> shopItem.getItem().getDisplayName().getString()
                    .toLowerCase().strip().contains(this.search.toLowerCase().strip())).toList();
        }
        //Clear shop buttons if they already exist
        shopButtons.forEach(this::removeWidget);
        shopButtons.clear();

        for(int j = 0; j < shopItems.size(); j++){
            final int j2 = j;
            List<ShopItem> finalShopItems = shopItems;
            ShopButton button = new ShopButton(shopItems.get(j),
                    x+SHOP_BUTTON_X+SHOP_BUTTON_SIZE*(j%NUM_COLS),
                    y+SHOP_BUTTON_Y+SHOP_BUTTON_SIZE*((j/NUM_COLS)%NUM_ROWS), itemRenderer, (b) -> {
                int quantity = ((ShopButton)b).getQuantity();
                attemptTransaction(this.usableAccounts.get(this.usableAccountsIndex), isBuy, finalShopItems.get(j2), quantity);
            });
            shopButtons.add(button);
            button.visible = isBuy;
            addRenderableWidget(button);
        }

    }

    private void createChangeAccountButton(int x, int y) {
        if(changeAccountButton != null) {
            removeWidget(changeAccountButton);
        }
        changeAccountButton = new ChangeAccountButton(x+127, y+108, (b) -> {
            changeAccounts();
            assert Minecraft.getInstance().player != null;
            Minecraft.getInstance().player.sendMessage(new TextComponent("Changed account to "+
                    MojangAPI.getUsernameByUUID(this.usableAccounts.get(this.usableAccountsIndex).getOwner())+":"+
                    this.usableAccounts.get(this.usableAccountsIndex).getId()), Minecraft.getInstance().player
                    .getUUID());
        });
        addRenderableWidget(changeAccountButton);
    }

    private void changeAccounts() {
        // Refresh account map and usable accounts
        this.accountMap = ClientLocalData.getAccountMap();
        BankAccount bankAccount = usableAccounts.get(usableAccountsIndex);
        this.usableAccounts.clear();
        this.usableAccounts.addAll(ClientLocalData.getUsableAccounts());
        // Change account, either by resetting to first (personal) account or moving to next sorted account
        if (!this.usableAccounts.contains(bankAccount)) {
            this.usableAccountsIndex = 0;
        } else {
            this.usableAccountsIndex = (this.usableAccounts.indexOf(bankAccount) + 1) % this.usableAccounts.size();
        }
    }
    private void createSearchBar(int x, int y) {
        int searchBarWidth = 70;
        int searchBarHeight = 12;
        searchBar = new EditBox(font, x+16, y+18, searchBarWidth, searchBarHeight, new TextComponent(""));
        addWidget(searchBar);
    }
    private void createBuySellButton(int x, int y){
        if(buySellButton != null){
            removeWidget(buySellButton);
        }
        buySellButton = new BuySellButton(x+15, y+4,
                I18n.get(GUI_BUY), I18n.get(GUI_SELL), isBuy, (b) -> {
            isBuy = ((BuySellButton)b).switchBuySell();
            refreshShopButtons();
        });
        addRenderableWidget(buySellButton);
    }

    private void refreshShopButtons(){
        buyButtons.forEach(b -> b.visible = false);
        sellButtons.forEach(b -> b.visible = false);
        changeAccountButton.visible = false;
        getVisibleShopButtons().forEach(b -> b.visible = true);
        changeAccountButton.visible = true;
    }

    private List<ShopButton> getVisibleShopButtons(){
        List<ShopButton> categoryButtons = isBuy ? buyButtons : sellButtons;
        int numPassed = rows_passed*NUM_COLS;
        return categoryButtons.subList(numPassed, Math.min(numPassed+NUM_ROWS*NUM_COLS, categoryButtons.size()));
    }

    private void attemptTransaction(BankAccount bankAccount, boolean isBuy, ShopItem item, int quantity){
        Pair<String, Integer> accountInfo = Pair.of(bankAccount.getOwner(), bankAccount.getId());
        Messages.sendToServer(new PacketPurchaseRequest(accountInfo, isBuy, item, quantity));
        // Refresh account map
        this.accountMap = ClientLocalData.getAccountMap();
    }

    private void printInfo(){
        System.out.println("Buy Buttons: ");
        buyButtons.forEach(System.out::println);
        System.out.println("Sell Buttons: ");
        sellButtons.forEach(System.out::println);

        System.out.println();
    }
}
