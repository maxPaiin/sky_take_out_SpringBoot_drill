package com.sky.service;

import com.sky.dto.ShoppingCartDTO;
public interface ShoppingCartService {

    /**
     * 添加購物車的業務方法
     * @param dto
     */
    void addShoppingCart(ShoppingCartDTO dto);
}
