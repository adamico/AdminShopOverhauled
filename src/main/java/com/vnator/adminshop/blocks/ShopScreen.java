package com.vnator.adminshop.blocks;

import com.ibm.icu.impl.Pair;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.vnator.adminshop.AdminShop;
import com.vnator.adminshop.client.gui.*;
import com.vnator.adminshop.money.BankAccount;
import com.vnator.adminshop.money.ClientMoneyData;
import com.vnator.adminshop.network.MojangAPI;
import com.vnator.adminshop.network.PacketPurchaseRequest;
import com.vnator.adminshop.setup.Messages;
import com.vnator.adminshop.shop.Shop;
import com.vnator.adminshop.shop.ShopItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

public class ShopScreen extends AbstractContainerScreen<ShopContainer> {

    private final ResourceLocation GUI = new ResourceLocation(AdminShop.MODID, "textures/gui/shop_gui.png");
    private final String GUI_BUY = "gui.buy";
    private final String GUI_SELL = "gui.sell";
    private final String GUI_MONEY = "gui.money_message";

    private static final int NUM_ROWS = 6, NUM_COLS = 6;
    private static final int SHOP_BUTTON_X = 62;
    private static final int SHOP_BUTTON_Y = 18;
    private static final int SHOP_BUTTON_SIZE = 18;
    public static final int SHOP_BUTTONS_PER_PAGE = 36;

    private final ShopContainer shopContainer;

    private final List<List<ShopButton>> buyButtons;
    private final List<List<ShopButton>> sellButtons;
    private int[] buyCategoriesPage; //Current page for each category
    private int[] sellCategoriesPage;
    private int buyCategory; //Currently selected category for the Buy option
    private int sellCategory;
    private boolean isBuy; //Whether the Buy option is currently selected
    private final String playerUUID;
    private Map<Pair<String, Integer>, BankAccount> accountMap;
    private Pair<String, Integer> personalAccount;
    private final List<Pair<String, Integer>> usableAccounts = new ArrayList<>();
    private int usableAccountsIndex;

    private ChangeAccountButton changeAccountButton;
    private BuySellButton buySellButton;
    private ScrollButton upButton;
    private ScrollButton downButton;
    private final List<CategoryButton> buyCategoryButtons;
    private final List<CategoryButton> sellCategoryButtons;

    public ShopScreen(ShopContainer container, Inventory inv, Component name) {
        super(container, inv, name);

        assert Minecraft.getInstance().player != null;
        assert Minecraft.getInstance().level != null;
        assert Minecraft.getInstance().level.isClientSide;

        this.playerUUID = Minecraft.getInstance().player.getStringUUID();
        this.personalAccount = Pair.of(this.playerUUID, 1);
        this.accountMap = ClientMoneyData.getAccountMap();

        if (!this.accountMap.containsKey(personalAccount)) {
            AdminShop.LOGGER.warn("Couldn't find personal account, creating one.");
            AdminShop.LOGGER.warn(personalAccount.first+":"+personalAccount.second);
            BankAccount personalBankAccount = ClientMoneyData.addAccount(new BankAccount(this.personalAccount.first,
                    this.personalAccount.second));
            // Refresh account map
            this.accountMap = ClientMoneyData.getAccountMap();
        }

        this.usableAccounts.clear();
        List<BankAccount> usableBankAccounts = this.accountMap.values().stream().filter(account -> (account.getMembers()
                .contains(playerUUID) || account.getOwner().equals(playerUUID))).toList();
        this.usableAccounts.addAll(usableBankAccounts.stream().map(account -> Pair.of(account.getOwner(),
                account.getId())).collect(Collectors.toSet()));
        sortUsableAccounts();
        this.usableAccountsIndex = 0;

        this.shopContainer = container;
        this.imageWidth = 195;
        this.imageHeight = 222;

        buyButtons = new ArrayList<>();
        sellButtons = new ArrayList<>();
        buyCategoryButtons = new ArrayList<>();
        sellCategoryButtons = new ArrayList<>();
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
        createScrollButtons(relX, relY);
        createCategoryButtons(true, relX, relY);
        createCategoryButtons(false, relX, relY);
        createChangeAccountButton(relX, relY);
        buyCategoriesPage = new int[buyButtons.size()];
        sellCategoriesPage = new int[sellButtons.size()];
        refreshShopButtons();
//        printInfo();
    }

    @Override
    public void render(@NotNull PoseStack matrixStack, int mouseX, int mouseY, float partialTicks){
        this.renderBackground(matrixStack);
        super.render(matrixStack, mouseX, mouseY, partialTicks);
        this.renderTooltip(matrixStack, mouseX, mouseY);
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
        drawString(matrixStack, Minecraft.getInstance().font,
                I18n.get(GUI_MONEY) + ClientMoneyData.getMoney(this.usableAccounts.get(this.usableAccountsIndex)),
                getXSize() - font.width(I18n.get(GUI_MONEY) + "00000000") - 4,
                6, 0xffffff); //x, y, color

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
    protected void renderBg(@NotNull PoseStack matrixStack, float partialTicks, int mouseX, int mouseY){
        RenderSystem.setShaderTexture(0, GUI);
        int relX = (this.width - this.imageWidth) / 2;
        int relY = (this.height - this.imageHeight) / 2;
        this.blit(matrixStack, relX, relY, 0, 0, this.imageWidth, this.imageHeight);
    }

    private void createShopButtons(boolean isBuy, int x, int y){
        List<List<ShopItem>> shopItems = isBuy ? Shop.get().shopStockBuy : Shop.get().shopStockSell;
        List<List<ShopButton>> shopButtons = isBuy ? buyButtons : sellButtons;
        //Clear shop buttons if they already exist
        shopButtons.forEach(l -> l.forEach(this::removeWidget));
        shopButtons.clear();
        for(int i = 0; i < shopItems.size(); i++){
            shopButtons.add(new ArrayList<>());
            for(int j = 0; j < shopItems.get(i).size(); j++){
                final int i2 = i;
                final int j2 = j;
                ShopButton button = new ShopButton(shopItems.get(i).get(j),
                        x+SHOP_BUTTON_X+SHOP_BUTTON_SIZE*(j%NUM_COLS),
                        y+SHOP_BUTTON_Y+SHOP_BUTTON_SIZE*((j/NUM_COLS)%NUM_ROWS), itemRenderer, (b) -> {
                    int quantity = ((ShopButton)b).getQuantity();
                    attemptTransaction(this.usableAccounts.get(this.usableAccountsIndex), isBuy, i2, j2, quantity);
                });
                shopButtons.get(i).add(button);
                button.visible = isBuy;
                addRenderableWidget(button);
            }
        }
    }

    private void createChangeAccountButton(int x, int y) {
        if(changeAccountButton != null) {
            removeWidget(changeAccountButton);
        }
        changeAccountButton = new ChangeAccountButton(x+9, y+108, (b) -> {
            changeAccounts();
            assert Minecraft.getInstance().player != null;
            assert Minecraft.getInstance().level != null;
            Minecraft.getInstance().player.sendMessage(new TextComponent("Changed account to "+
                    MojangAPI.getUsernameByUUID(this.usableAccounts.get(this.usableAccountsIndex).first)+":"+
                    this.usableAccounts.get(this.usableAccountsIndex).second), Minecraft.getInstance().player.getUUID());
            refreshShopButtons();
        });
        addRenderableWidget(changeAccountButton);
    }

    // Sort usableAccounts, first by pair.first == playerUUID, if not sort alphabetically, then by pair.second in
    // ascending order. Index is preserved to the original account it pointed to.
    private void sortUsableAccounts() {
        Pair<String, Integer> selectedBankAccount = usableAccounts.get(usableAccountsIndex);
        this.usableAccounts.sort((o1, o2) -> {
            if (o1.first.equals(playerUUID) && !o2.first.equals(playerUUID)) {
                return -1;
            } else if (!o1.first.equals(playerUUID) && o2.first.equals(playerUUID)) {
                return 1;
            } else if (o1.first.equals(o2.first)) {
                return o1.second.compareTo(o2.second);
            } else {
                return o1.first.compareTo(o2.first);
            }
        });
        this.usableAccountsIndex = usableAccounts.indexOf(selectedBankAccount);
    }

    private void changeAccounts() {
        // Refresh account map
        this.accountMap = ClientMoneyData.getAccountMap();

        // Check for new accounts
        Set<Pair<String, Integer>> newUsableAccounts = accountMap.values().stream().filter(account -> (account.getMembers()
                .contains(playerUUID) || account.getOwner().equals(playerUUID))).map(account ->
                Pair.of(account.getOwner(), account.getId())).collect(Collectors.toSet());

        // If usableAccounts doesn't contain all, add the new one
        if(!new HashSet<>(this.usableAccounts).containsAll(newUsableAccounts)) {
            newUsableAccounts.forEach(account -> {
                if(!this.usableAccounts.contains(account)) {
                    // Add new account, and sort
                    usableAccounts.add(account);
                    sortUsableAccounts();
                }
            });
        }

        // If newUsableAccounts doesn't contain all, remove the deleted one
        if(!newUsableAccounts.containsAll(this.usableAccounts)) {
            usableAccounts.forEach(account -> {
                if(!newUsableAccounts.contains(account)) {
                    boolean isCurrentIndex = usableAccounts.indexOf(account) == this.usableAccountsIndex;
                    // We can never remove the first account (personal account)
                    if (!(this.usableAccounts.size() > 1 && !this.usableAccounts.get(0).equals(account))) {
                        AdminShop.LOGGER.error("Missing personal account in accountMap!");
                    } else {
                        // Remove this account
                        this.usableAccounts.remove(account);
                        // reset index if removed account used to be the current index
                        if (isCurrentIndex) {
                            this.usableAccountsIndex = 0;
                        }
                    }
                }
            });
        }

        this.usableAccountsIndex = (this.usableAccountsIndex + 1) % this.usableAccounts.size();

    }
    private void createBuySellButton(int x, int y){
        if(buySellButton != null){
            removeWidget(buySellButton);
        }
        buySellButton = new BuySellButton(x+8, y+4,
                I18n.get(GUI_BUY), I18n.get(GUI_SELL), isBuy, (b) -> {
            isBuy = ((BuySellButton)b).switchBuySell();
            refreshShopButtons();
        });
        addRenderableWidget(buySellButton);
    }

    private void createScrollButtons(int x, int y){
        if(upButton != null){
            removeWidget(upButton);
        }
        upButton = new ScrollButton(x+170, y+18, true, b -> {
            if(isBuy) {
                buyCategoriesPage[buyCategory] = Math.max(buyCategoriesPage[buyCategory] - 1, 0);
            }else{
                sellCategoriesPage[sellCategory] = Math.max(sellCategoriesPage[sellCategory] - 1, 0);
            }
            refreshShopButtons();
        });
        addRenderableWidget(upButton);

        if(downButton != null){
            removeWidget(downButton);
        }
        downButton = new ScrollButton(x+170, y+108, false, b -> {
            if(isBuy) {
                int maxScroll = ((buyButtons.get(buyCategory).size()-1) / (NUM_ROWS * NUM_COLS));
                buyCategoriesPage[buyCategory] = Math.min(buyCategoriesPage[buyCategory] + 1, maxScroll);
            }else{
                int maxScroll = ((sellButtons.get(sellCategory).size()-1) / (NUM_ROWS * NUM_COLS));
                sellCategoriesPage[sellCategory] = Math.min(sellCategoriesPage[sellCategory] + 1, maxScroll);
            }
            refreshShopButtons();
        });
        addRenderableWidget(downButton);
    }

    private void createCategoryButtons(boolean isBuy, int x, int y){
        List<CategoryButton> buttons = (isBuy ? buyCategoryButtons : sellCategoryButtons);
        if(buttons != null){
            buttons.forEach(this::removeWidget);
            buttons.clear();
        }
        List<String> names = (isBuy ? Shop.get().categoryNamesBuy : Shop.get().categoryNamesSell);
        for(int i = 0; i < names.size(); i++){
            int index = i;
            CategoryButton button = new CategoryButton(x+9, y+18+18*i, names.get(i), b -> {
                if(isBuy){
                    buyCategory = index;
                    buyCategoryButtons.forEach(but -> ((CategoryButton)but).setSelected(false));
                }else{
                    sellCategory = index;
                    sellCategoryButtons.forEach(but -> ((CategoryButton)but).setSelected(false));
                }
                ((CategoryButton)b).setSelected(true);
                refreshShopButtons();
            });
            if(i == 0){
                button.setSelected(true);
            }
            button.visible = isBuy == this.isBuy;

            if (buttons != null) {
                buttons.add(button);
            }
            addRenderableWidget(button);
        }
    }

    private void refreshShopButtons(){
        buyButtons.forEach(l -> l.forEach(b -> b.visible = false));
        sellButtons.forEach(l -> l.forEach(b -> b.visible = false));
        buyCategoryButtons.forEach(b -> b.visible = false);
        sellCategoryButtons.forEach(b -> b.visible = false);
        changeAccountButton.visible = false;
//        List<ShopButton> buttons = (isBuy ? buyButtons : sellButtons)
//                .get((isBuy ? buyCategory : sellCategory));
//        int page = (isBuy ? buyCategoriesPage[buyCategory] : sellCategoriesPage[sellCategory]);
//        buttons = buttons.subList(page * NUM_ROWS*NUM_COLS,
//                Math.min((page+1) * NUM_ROWS*NUM_COLS, buttons.size()) );
        getVisibleShopButtons().forEach(b -> b.visible = true);
        (isBuy ? buyCategoryButtons : sellCategoryButtons).forEach(b -> b.visible = true);
        changeAccountButton.visible = true;
    }

    private List<ShopButton> getVisibleShopButtons(){
        int shopPage = isBuy ? buyCategoriesPage[buyCategory] : sellCategoriesPage[sellCategory];
        List<ShopButton> categoryButtons = (isBuy ? buyButtons : sellButtons)
                .get(isBuy ? buyCategory : sellCategory);
        int numPassed = NUM_ROWS*NUM_COLS*shopPage;
        //return (isBuy ? buyButtons : sellButtons).stream().reduce((l1, l2)->{l1.addAll(l2); return l1;}).get();
        return categoryButtons.subList(numPassed, Math.min(numPassed+NUM_ROWS*NUM_COLS, categoryButtons.size()));
    }

    private void attemptTransaction(Pair<String, Integer> bankAccount, boolean isBuy, int category, int index, int quantity){
        Messages.sendToServer(new PacketPurchaseRequest(bankAccount, isBuy, category, index, quantity));
        // Refresh account map
        this.accountMap = ClientMoneyData.getAccountMap();
    }

    private void printInfo(){
        System.out.println("Buy Buttons: ");
        buyButtons.forEach(System.out::println);
        System.out.println("Sell Buttons: ");
        sellButtons.forEach(System.out::println);

        System.out.println();

        System.out.println("Buy Category pages | Selected: "+buyCategory);
        for (int i : buyCategoriesPage) {
            System.out.print(i+" ");
        }
        System.out.println();
        System.out.println("Sell Category pages | Selected: "+sellCategory);
        for (int i : sellCategoriesPage) {
            System.out.print(i+" ");
        }

        System.out.println("\n");
    }
}
