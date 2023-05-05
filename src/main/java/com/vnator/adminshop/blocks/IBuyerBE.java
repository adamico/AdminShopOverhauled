package com.vnator.adminshop.blocks;

import com.vnator.adminshop.shop.ShopItem;

public interface IBuyerBE extends AutoShopMachine {
    void setTargetItem(ShopItem targetItem);
    ShopItem getTargetItem();
}
