package com.sky.service;

import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.result.PageResult;


public interface DishService {

    /**
     * 新增菜品和口味數據
     * @param dishDTO
     */
     void saveWithFlavor(DishDTO dishDTO);

     PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO);
}
