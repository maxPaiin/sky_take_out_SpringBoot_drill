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
}
