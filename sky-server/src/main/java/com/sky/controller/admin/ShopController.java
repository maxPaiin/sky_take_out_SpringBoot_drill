package com.sky.controller.admin;

import com.sky.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

@RestController("adminShopController")
@RequestMapping("/admin/shop")
@Slf4j
@Api(tags = "店鋪相關")
public class ShopController {

    public static final String KEY = "shop_status";

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 設置店鋪營業狀態
     * @param status 前端傳來的狀態碼
     * @return 一個Result對象
     */
    @PutMapping("/{status}")
    @ApiOperation("設置店鋪營業狀態")
    public Result setSatus(@PathVariable Integer status){
        log.info("店鋪營業狀態:{}", status == 1 ? "營業中":"打烊");
        //設置redis數據,用字符串的方式
        redisTemplate.opsForValue().set(KEY, status);
        return Result.success();
    }

    @GetMapping("/status")
    @ApiOperation("獲取店鋪營業狀態")
    public Result<Integer> getStatus(){
        Integer status = (Integer) redisTemplate.opsForValue().get(KEY);
        log.info("店鋪營業狀態:{}", status == 1 ? "營業中":"打烊");
        return Result.success(status);
    }
}