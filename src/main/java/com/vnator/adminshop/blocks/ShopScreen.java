package com.vnator.adminshop.blocks;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.vnator.adminshop.AdminShop;
import com.vnator.adminshop.client.gui.BuySellButton;
import com.vnator.adminshop.client.gui.CategoryButton;
import com.vnator.adminshop.client.gui.ScrollButton;
import com.vnator.adminshop.client.gui.ShopButton;
import com.vnator.adminshop.network.PacketPurchaseRequest;
import com.vnator.adminshop.setup.Messages;
import com.vnator.adminshop.shop.Shop;
import com.vnator.adminshop.shop.ShopItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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

    private ShopContainer shopContainer;

    private List<List<ShopButton>> buyButtons;
    private List<List<ShopButton>> sellButtons;
    private int[] buyCategoriesPage; //Current page for each category
    private int[] sellCategoriesPage;
    private int buyCategory; //Currently selected category for the Buy option
    private int sellCategory;
    private boolean isBuy; //Whether the Buy option is currently selected

    private BuySellButton buySellButton;
    private ScrollButton upButton;
    private ScrollButton downButton;
    private List<CategoryButton> buyCategoryButtons;
    private List<CategoryButton> sellCategoryButtons;

    public ShopScreen(ShopContainer container, Inventory inv, Component name) {
        super(container, inv, name);
        this.shopContainer = container;
        this.imageWidth = 195;
        this.imageHeight = 222;
        buyButtons = new ArrayList<>();
        sellButtons = new ArrayList<>();
        buyCategoryButtons = new ArrayList<>();
        sellCategoryButtons = new ArrayList<>();
        isBuy = true;
    }

    @SuppressWarnings("resource")
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
        buyCategoriesPage = new int[buyButtons.size()];
        sellCategoriesPage = new int[sellButtons.size()];
        refreshShopButtons();
        //printInfo();
    }

    @Override
    public void render(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks){
        this.renderBackground(matrixStack);
        super.render(matrixStack, mouseX, mouseY, partialTicks);
        this.renderTooltip(matrixStack, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(PoseStack matrixStack, int mouseX, int mouseY) {
        matrixStack.pushPose();
        //Block Title
        String blockName = I18n.get(ShopBlock.SCREEN_ADMINSHOP_SHOP);
        drawCenteredString(matrixStack, font, blockName, getXSize()/2, 6, 0x404040);
        //drawString(name, getXSize()/2 - string getStringWidth(name)/2, 6, 0x404040);

        //Player Inventory Title
        drawString(matrixStack, font, playerInventoryTitle, 16, getYSize()-94, 0x404040);

        //Player Balance
        drawString(matrixStack, Minecraft.getInstance().font,
                I18n.get(GUI_MONEY) + shopContainer.getPlayerBalance(),
                10, 10, 0xffffff); //x, y, color

        //Tooltip for item the player is hovering over
        List<ShopButton> shopButtons = getVisibleShopButtons();
        Optional<ShopButton> button = shopButtons.stream().filter(b -> b.isMouseOn).findFirst();
        button.ifPresent(shopButton -> {
            renderTooltip(matrixStack, shopButton.getTooltipContent(),
                    Optional.empty(), mouseX-(this.width - this.imageWidth) / 2, mouseY);
        });
        matrixStack.popPose();

    }

    @Override
    protected void renderBg(PoseStack matrixStack, float partialTicks, int mouseX, int mouseY){
        RenderSystem.setShaderTexture(0, GUI);
        int relX = (this.width - this.imageWidth) / 2;
        int relY = (this.height - this.imageHeight) / 2;
        this.blit(matrixStack, relX, relY, 0, 0, this.imageWidth, this.imageHeight);
    }

    private void createShopButtons(boolean isBuy, int x, int y){
        List<List<ShopItem>> shopItems = isBuy ? Shop.get().shopStockBuy : Shop.get().shopStockSell;
        List<List<ShopButton>> shopButtons = isBuy ? buyButtons : sellButtons;
        //Clear shop buttons if they already exist
        shopButtons.forEach(l -> l.forEach(b -> removeWidget(b)));
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
                    attemptTransaction(isBuy, i2, j2, quantity);
                });
                shopButtons.get(i).add(button);
                button.visible = isBuy;
                addRenderableWidget(button);
            }
        }
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
            buttons.forEach(b -> removeWidget(b));
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
            buttons.add(button);
            addRenderableWidget(button);
        }
    }

    private void refreshShopButtons(){
        buyButtons.forEach(l -> l.forEach(b -> b.visible = false));
        sellButtons.forEach(l -> l.forEach(b -> b.visible = false));
        buyCategoryButtons.forEach(b -> b.visible = false);
        sellCategoryButtons.forEach(b -> b.visible = false);
//        List<ShopButton> buttons = (isBuy ? buyButtons : sellButtons)
//                .get((isBuy ? buyCategory : sellCategory));
//        int page = (isBuy ? buyCategoriesPage[buyCategory] : sellCategoriesPage[sellCategory]);
//        buttons = buttons.subList(page * NUM_ROWS*NUM_COLS,
//                Math.min((page+1) * NUM_ROWS*NUM_COLS, buttons.size()) );
        getVisibleShopButtons().forEach(b -> b.visible = true);
        (isBuy ? buyCategoryButtons : sellCategoryButtons).forEach(b -> b.visible = true);
    }

    private List<ShopButton> getVisibleShopButtons(){
        int shopPage = isBuy ? buyCategoriesPage[buyCategory] : sellCategoriesPage[sellCategory];
        List<ShopButton> categoryButtons = (isBuy ? buyButtons : sellButtons)
                .get(isBuy ? buyCategory : sellCategory);
        int numPassed = NUM_ROWS*NUM_COLS*shopPage;
        //return (isBuy ? buyButtons : sellButtons).stream().reduce((l1, l2)->{l1.addAll(l2); return l1;}).get();
        return categoryButtons.subList(numPassed, Math.min(numPassed+NUM_ROWS*NUM_COLS, categoryButtons.size()));
    }

    private void attemptTransaction(boolean isbuy, int category, int index, int quantity){
        Messages.sendToServer(new PacketPurchaseRequest(isbuy, category, index, quantity));
    }

    private void printInfo(){
        System.out.println("Buy Buttons: ");
        buyButtons.forEach(l -> System.out.println(l));
        System.out.println("Sell Buttons: ");
        sellButtons.forEach(l -> System.out.println(l));

        System.out.println("");

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
