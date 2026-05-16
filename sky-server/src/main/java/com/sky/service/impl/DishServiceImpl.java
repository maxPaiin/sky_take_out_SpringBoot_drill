package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.entity.Setmeal;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
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

    @Autowired
    private SetmealMapper setmealMapper;

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
        if (flavors != null && !flavors.isEmpty()) {
            flavors.forEach(dishFlavor -> dishFlavor.setDishId(dishId));
            //向口味表插入多條數據
            dishFlavorMapper.insertBatch(flavors);
        }
    }

    /**
     * 菜品分页查询
     *
     * @param dishPageQueryDTO 菜品分页查询条件
     */
    public PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO) {
        PageHelper.startPage(dishPageQueryDTO.getPage(), dishPageQueryDTO.getPageSize());
        Page<DishVO> page = dishMapper.pageQuery(dishPageQueryDTO);
        return new PageResult(page.getTotal(), page.getResult());
    }


    /**
     * 菜品批量刪除
     *
     * @param ids 傳入的商品(菜品)ID或是ID集合
     */
    @Override
    @Transactional
    public void deleteBatch(List<Long> ids) {
        //判斷菜品是否能刪除——是否是售賣中的
        for (Long id : ids) {
            Dish dish = dishMapper.getById(id);
            //菜品起售中,不能刪除
            if (dish.getStatus() == StatusConstant.ENABLE)
                throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
        }
        //被套餐關聯時,也不能刪除
        List<Long> setmealIds = setmealDishMapper.getDishIdsByDishIds(ids);
        //當前菜品被套餐關聯,不能刪除
        if (!setmealIds.isEmpty()) throw new DeletionNotAllowedException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);
        //可以刪除的情況下——菜品表中的菜品數據
//        for(Long id : ids){
//            dishMapper.deleteById(id);
//            //刪除和菜品關聯的口味數據
//            dishFlavorMapper.deleteByDishId(id);
//        }
        //根據菜品id集合批量刪除菜品數據
        dishMapper.deleteByIds(ids);
        //根據菜品id批量刪除菜品口味數據
        dishFlavorMapper.deleteByDishIds(ids);
    }

    /**
     * 根据id查询菜品信息
     *
     * @param id 菜品id(前端來的),所以這邊用VO來接收,形參寫@PathVariable:指示方法參數應綁定到 URI 模板變數的註解
     * @return
     */
    @Override
    public DishVO getByIdWithFlavor(Long id) {
        //根據id查詢菜品數據
        Dish dish = dishMapper.getById(id);
        //根據id查詢口味數據
        List<DishFlavor> dishFlavors = dishFlavorMapper.getByDishId(id);
        //查詢到的數據封裝到VO中
        DishVO dishVO = new DishVO();
        //對象賦值,把VO的數據copy給dish
        BeanUtils.copyProperties(dish, dishVO);
        dishVO.setFlavors(dishFlavors);
        return dishVO;
    }

    /**
     * 修改菜品和口味數據
     * @param dishDTO
     */
    @Override
    public void updateWithFlavor(DishDTO dishDTO) {
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);
        //修改菜品表基本信息
        dishMapper.update(dish);
        //刪除原來的口味數據
        dishFlavorMapper.deleteByDishId(dish.getId());
        //重新插入口味數據
        List<DishFlavor> flavors = dishDTO.getFlavors();
        if (flavors != null && !flavors.isEmpty()) {
            flavors.forEach(
                    dishFlavor -> dishFlavor.setDishId(dishDTO.getId())
            );
            dishFlavorMapper.insertBatch(flavors);
        }
    }

    /**
     * 菜品起售停售
     *
     * @param status
     * @param id
     */
    @Transactional
    public void startOrStop(Integer status, Long id) {
        Dish dish = Dish.builder()
                .id(id)
                .status(status)
                .build();
        dishMapper.update(dish);

        if (status == StatusConstant.DISABLE) {
            // 如果是停售操作，还需要将包含当前菜品的套餐也停售
            List<Long> dishIds = new ArrayList<>();
            dishIds.add(id);
            // select setmeal_id from setmeal_dish where dish_id in (?,?,?)
            List<Long> setmealIds = setmealDishMapper.getSetmealIdsByDishIds(dishIds);
            if (setmealIds != null && setmealIds.size() > 0) {
                for (Long setmealId : setmealIds) {
                    Setmeal setmeal = Setmeal.builder()
                            .id(setmealId)
                            .status(StatusConstant.DISABLE)
                            .build();
                    setmealMapper.update(setmeal);
                }
            }
        }
    }

    /**
     * 根据分类id查询菜品
     * @param categoryId
     * @return
     */
    public List<Dish> list(Long categoryId) {
        Dish dish = Dish.builder()
                .categoryId(categoryId)
                .status(StatusConstant.ENABLE)
                .build();
        return dishMapper.list(dish);
    }


    /**
     * 条件查询菜品和口味
     * @param dish
     * @return
     */
    public List<DishVO> listWithFlavor(Dish dish) {
        List<Dish> dishList = dishMapper.list(dish);

        List<DishVO> dishVOList = new ArrayList<>();

        for (Dish d : dishList) {
            DishVO dishVO = new DishVO();
            BeanUtils.copyProperties(d,dishVO);

            //根据菜品id查询对应的口味
            List<DishFlavor> flavors = dishFlavorMapper.getByDishId(d.getId());

            dishVO.setFlavors(flavors);
            dishVOList.add(dishVO);
        }
        return dishVOList;
    }
}