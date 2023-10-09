package com.ammonium.adminshop.blocks;

import com.ammonium.adminshop.shop.ShopItem;

public interface BuyerMachine extends ShopMachine {
    void setTargetShopItem(ShopItem item);
}
