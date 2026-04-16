package com.sky.mapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface SetmealDishMapper {
    /**
     * 批量插入套餐和菜品的关联关系
     * @param  setmealIds
     */
   List<Long> getDishIdsByDishIds(List<Long> setmealIds);
}
