package com.sky.service.impl;

import com.sky.context.BaseContext;
import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.entity.ShoppingCart;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.mapper.ShoppingCartMapper;
import com.sky.service.ShoppingCartService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class ShoppingCartServiceImpl implements ShoppingCartService {

    @Autowired
    private ShoppingCartMapper shoppingCartMapper;

    @Autowired
    private DishMapper dishMapper;

    @Autowired
    private SetmealMapper setmealMapper;

    /**
     * 添加購物車的業務方法
     *
     * @param dto 購物車數據
     */
    @Override
    public void addShoppingCart(ShoppingCartDTO dto) {
        var shoppingCart = new ShoppingCart();
        BeanUtils.copyProperties(dto, shoppingCart); //dto當中的數據拷貝給實體類
        Long currentId = BaseContext.getCurrentId();//獲得當前用戶的ID
        shoppingCart.setUserId(currentId);//將當前用戶ID拷貝給實體類
        List<ShoppingCart> list = shoppingCartMapper.list(shoppingCart);
        //判斷當前加入購物車的商品是否存在
        if (list != null && !list.isEmpty()) {//存在的情況下,僅需要將數量+1
            ShoppingCart cart = list.get(0); //其實查詢之後,只會有「一條」(list)數據
            cart.setNumber(cart.getNumber() + 1);
            shoppingCartMapper.updateNumberById(cart);
        } else { //不存在的情況下,插入一條購物車數據
            //添加購物車的菜品還是套餐
            Long dishId = dto.getDishId();//菜品的id
            Long setmealId = dto.getSetmealId();//套餐的id
            if (dishId != null) {
                Dish dish = dishMapper.getById(dishId);
                shoppingCart.setName(dish.getName());
                shoppingCart.setImage(dish.getImage());
                shoppingCart.setAmount(dish.getPrice());
            } else {
                Setmeal setmeal = setmealMapper.getById(setmealId);
                shoppingCart.setName(setmeal.getName());
                shoppingCart.setImage(setmeal.getImage());
                shoppingCart.setAmount(setmeal.getPrice());
            }
            shoppingCart.setNumber(1);
            shoppingCart.setCreateTime(LocalDateTime.now());

            shoppingCartMapper.insert(shoppingCart);
        }
    }

    @Override
    public List<ShoppingCart> showShoppingCart() {
        Long currentId = BaseContext.getCurrentId();
        ShoppingCart build = ShoppingCart.builder().userId(currentId).build();
        return shoppingCartMapper.list(build);
    }

    @Override
    public void cleanShoppingCart() {
        Long currentId = BaseContext.getCurrentId();
        shoppingCartMapper.deleteByUserId(currentId);
    }

    @Override
    public void subShoppingCart(ShoppingCartDTO shoppingCartDTO) {
        ShoppingCart shoppingCart = new ShoppingCart();
        BeanUtils.copyProperties(shoppingCartDTO, shoppingCart);
        //以當前用戶 + 商品標識定位購物車項(list 含 user_id 條件,天然攔截越權)
        shoppingCart.setUserId(BaseContext.getCurrentId());
        List<ShoppingCart> list = shoppingCartMapper.list(shoppingCart);
        if (list != null && !list.isEmpty()) {
            ShoppingCart cart = list.get(0);
            if (cart.getNumber() > 1) {
                //數量大於1,僅減1
                cart.setNumber(cart.getNumber() - 1);
                shoppingCartMapper.updateNumberById(cart);
            } else {
                //數量為1,刪除整行
                shoppingCartMapper.deleteById(cart.getId());
            }
        }
    }
}
