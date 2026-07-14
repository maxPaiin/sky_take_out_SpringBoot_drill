package com.sky.task;

/**
 * 定時任務類
 */
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@Slf4j
public class OrderTask {

    @Autowired
    private OrderMapper orderMapper;

    /**
     * 處理超時訂單
     * 每分鐘觸法一次
     */
    @Scheduled(cron = "0 * * * * ?") //每分鐘執行一次
    public void processTimeOrder() {
        log.info("process time order{}", LocalDateTime.now());
        List<Orders> byStatusAndOrderTimeLT = orderMapper.getByStatusAndOrderTimeLT(Orders.PENDING_PAYMENT, LocalDateTime.now().plusMinutes(-15));
        if (byStatusAndOrderTimeLT != null && !byStatusAndOrderTimeLT.isEmpty()) {
            for (Orders orders : byStatusAndOrderTimeLT) {
                orders.setStatus(Orders.CANCELLED);
                orders.setCancelReason("訂單超時");
                orders.setCancelTime(LocalDateTime.now());
                orderMapper.update(orders);
            }
        }
    }

    /**
     * 一直處於派送中的訂單
     */
    @Scheduled(cron = "0 0 1 * * ?") //每日凌晨1點觸發一次
    public void processTimeOrder2() {
        log.info("process time order{}", LocalDateTime.now());
        List<Orders> byStatusAndOrderTimeLT = orderMapper.getByStatusAndOrderTimeLT(Orders.DELIVERY_IN_PROGRESS,
                LocalDateTime.now().plusMinutes(-61));
        if (byStatusAndOrderTimeLT != null && !byStatusAndOrderTimeLT.isEmpty()) {
            for (Orders orders : byStatusAndOrderTimeLT) {
                orders.setStatus(Orders.COMPLETED);
                orderMapper.update(orders);
            }
        }
    }
}
