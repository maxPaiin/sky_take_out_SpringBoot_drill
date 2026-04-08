package com.sky.service;

import com.sky.dto.DishDTO;


public interface DishService {

    /**
     * 新增菜品和口味數據
     * @param dishDTO
     */
     void saveWithFlavor(DishDTO dishDTO);
}
