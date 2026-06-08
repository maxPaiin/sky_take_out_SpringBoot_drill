package com.sky.service;

import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.ShoppingCart;

import java.util.List;

public interface ShoppingCartService {

    /**
     * 添加購物車的業務方法
     * @param dto
     */
    void addShoppingCart(ShoppingCartDTO dto);

    List<ShoppingCart> showShoppingCart();

    void cleanShoppingCart();

    /**
     * 刪減購物車中一個商品(數量-1,減到0則刪除整行)
     * @param shoppingCartDTO 商品標識(dishId/setmealId/dishFlavor)
     */
    void subShoppingCart(ShoppingCartDTO shoppingCartDTO);
}
