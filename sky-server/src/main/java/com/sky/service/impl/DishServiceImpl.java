package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.result.PageResult;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
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

    @Autowired
    private SetmealDishMapper setmealDishMapper;


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
            flavors.forEach(dishFlavor -> dishFlavor.setDishId(dishId));
            //向口味表插入多條數據
            dishFlavorMapper.insertBatch(flavors);
        }
    }

    /**
     * 菜品分页查询
     * @param dishPageQueryDTO 菜品分页查询条件
     */
    public PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO) {
        PageHelper.startPage(dishPageQueryDTO.getPage(), dishPageQueryDTO.getPageSize());
        Page<DishVO> page = dishMapper.pageQuery(dishPageQueryDTO);
        return new PageResult(page.getTotal(), page.getResult());
    }


    /**
     * 菜品批量刪除
     * @param ids 傳入的商品(菜品)ID或是ID集合
     */
    @Override
    @Transactional
    public void deleteBatch(List<Long> ids) {
        //判斷菜品是否能刪除——是否是售賣中的
        for(Long id : ids){
            Dish dish = dishMapper.getById(id);
            //菜品起售中,不能刪除
            if(dish.getStatus() == StatusConstant.ENABLE) throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
        }
        //被套餐關聯時,也不能刪除
        List<Long> setmealIds = setmealDishMapper.getDishIdsByDishIds(ids);
        //當前菜品被套餐關聯,不能刪除
        if(!setmealIds.isEmpty())  throw new DeletionNotAllowedException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);
        //可以刪除的情況下——菜品表中的菜品數據
        for(Long id : ids){
            dishMapper.deleteById(id);
            //刪除和菜品關聯的口味數據
            dishFlavorMapper.deleteByDishId(id);
        }
    }
}
