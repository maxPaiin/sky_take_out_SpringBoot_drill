package com.sky.service.impl;

import com.sky.dto.DishDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.service.DishService;
import io.swagger.v3.oas.annotations.servers.Server;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 新增菜品和口味數據
 */
@Service
@Slf4j
public class DishServiceImpl implements DishService {
    @Autowired
    private DishMapper dishMapper;

    @Autowired
    private DishFlavorMapper dishFlavorMapper;

    @Override
    @Transactional //开启事务管理
    public void saveWithFlavor(DishDTO dishDTO) {
        Dish dish = new Dish();
        //對象賦值,把DTO的數據copy給dish
        BeanUtils.copyProperties(dishDTO, dish);
        //向菜品表插入一條數據
        dishMapper.insert(dish);

        //獲取insert語句獲取的主鍵值,這個在DishMapper.xml中已配置,插入數據後會自動回填到dish對象中
        Long dishId = dish.getId();

        //口味是用List的方式放在DishDTO中,所以需要獲取到口味數據,然後對每個口味數據設置菜品id,最後批量插入口味數據到口味表中
        List<DishFlavor> flavors = dishDTO.getFlavors();
        if(flavors != null && !flavors.isEmpty()){
            flavors.forEach(dishFlavor -> {
                dishFlavor.setDishId(dishId);
            });
            //向口味表插入多條數據
            dishFlavorMapper.insertBatch(flavors);
        }
    }
}
