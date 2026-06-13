package com.sky.service;

import com.sky.dto.OrdersSubmitDTO;
import com.sky.vo.OrderSubmitVO;

public interface OrderService {

/**
 * 用戶下單的方法
 */
OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO);
}
