package com.sky.service;

import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.result.PageResult;

import java.util.List;


public interface DishService {

    /**
     * 新增菜品和口味數據
     * @param dishDTO
     */
     void saveWithFlavor(DishDTO dishDTO);

     PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO);

    /**
     * 菜品批量刪除
     * @param ids 傳入的商品(菜品)ID或是ID集合
     */
     void deleteBatch(List<Long> ids);
}
