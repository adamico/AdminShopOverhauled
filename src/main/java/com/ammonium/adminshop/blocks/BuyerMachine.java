package com.ammonium.adminshop.blocks;

import com.ammonium.adminshop.shop.ShopItem;

public interface BuyerMachine extends AutoShopMachine {
    void setTargetShopItem(ShopItem item);
}
