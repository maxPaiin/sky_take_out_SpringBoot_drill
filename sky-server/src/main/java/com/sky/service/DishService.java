package com.sky.service;

import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.result.PageResult;
import com.sky.vo.DishVO;

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

    /**
     * 根据id查询菜品和口味數據信息
     * @param id
     * @return
     */
     DishVO getByIdWithFlavor(Long id);

     /**
      * 修改菜品和口味數據
      * @param dishDTO
      */
     void updateWithFlavor(DishDTO dishDTO);

    /**
     * 菜品起售停售
     * @param status
     * @param id
     */
    void startOrStop(Integer status, Long id);


    /**
     * 条件查询菜品和口味
     * @param dish
     * @return
     */
    List<DishVO> listWithFlavor(Dish dish);

    /**
     * 根据分类id查询菜品
     * @param categoryId
     * @return
     */
    List<Dish> list(Long categoryId);
}
