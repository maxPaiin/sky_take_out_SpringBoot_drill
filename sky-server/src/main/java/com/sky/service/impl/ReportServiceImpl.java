package com.sky.service.impl;

import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.vo.OrderReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
//統計一段時間的營業額
public class ReportServiceImpl implements ReportService {
    //需要已完成的訂單的表
    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private UserMapper userMapper;

    @Override
    public TurnoverReportVO getTurnoverReport(LocalDate begin, LocalDate end) {
        //當前集合用於存放蔥begin到end日期範圍內的每天的日期
        List<LocalDate> dataList = new ArrayList<>();
        dataList.add(begin);

        //日期計算,指定日期到結束日期
        while (!begin.equals(end)) {
            begin = begin.plusDays(1);
            dataList.add(begin);
        }

        List<Double> turnoverList = new ArrayList<>();//存放每天的營業額
        for (LocalDate localDate : dataList) {
            //查詢date日期對應的營業額數據,營業額為“已完成”的訂單金額合計
            LocalDateTime beginTime = LocalDateTime.of(localDate, LocalTime.MIN); // LocalDateTime.of(localDate, LocalTime.MIN) 這一天的起始時間
            LocalDateTime endTime = LocalDateTime.of(localDate, LocalTime.MAX); // LocalDateTime.of(localDate, LocalTime.MAX) 這一天的結束時間
            //select sum(amount) from orders where order_time > beginTime and order_time < endTime and status = 5
            Map<String , Object> map = new HashMap();
            map.put("beginTime", beginTime);
            map.put("endTime", endTime);
            map.put("status", Orders.COMPLETED);
            Double turnover = orderMapper.sumByMap(map);
            turnover = turnover == null ? 0.0 : turnover;
            turnoverList.add(turnover);
        }

        return TurnoverReportVO
                .builder()
                .dateList(StringUtils.join(dataList, ","))
                .turnoverList(StringUtils.join(turnoverList, ","))
                .build();
    }

    //統計指定時間內的用戶流量數據
    @Override
    public UserReportVO getUserStatistics(LocalDate begin, LocalDate end) {

        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(begin);

        while(!begin.equals(end)){
            begin = begin.plusDays(1);
            dateList.add(begin);
        }

        List<Integer> newUserList = new ArrayList<>();//存放每天新增用戶 select count(id) from user where create_time < ? and create_time > ?
        List<Integer> totalUserList = new ArrayList<>();//每天的總用戶數量 select count(id) from user where create_time < ?


        dateList.forEach(date->{
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN); //當天開始時刻
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX); //當天結束時刻

            Map<String , Object> map = new HashMap();
            map.put("end", endTime);
            Integer totalUser = userMapper.countByMap(map);//總用戶數量

            map.put("begin", beginTime);
            Integer  newUser = userMapper.countByMap(map);//新增用戶數量

            newUserList.add(newUser);
            totalUserList.add(totalUser);
        });
        return UserReportVO.builder()
                .dateList(StringUtils.join(dateList, ","))
                .newUserList(StringUtils.join(newUserList, ","))
                .totalUserList(StringUtils.join(totalUserList, ","))
                .build();
    }
    //指定時間內的訂單數據
    @Override
    public OrderReportVO getOrderStatistics(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(begin);

        while(!begin.equals(end)){
            begin = begin.plusDays(1);
            dateList.add(begin);
        }

        List<Integer> orderCountList = new ArrayList<>();
        List<Integer> validOrderCountList = new ArrayList<>();

        //每一天的有效訂單數和訂單總數
        dateList.forEach(date->{
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN); //當天開始時刻
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX); //當天結束時刻

            Integer totalOrder = getOrderCount(beginTime, endTime, null); //當天訂單總數
            Integer validOrder = getOrderCount(beginTime, endTime, Orders.COMPLETED); //當天有效訂單數

            orderCountList.add(totalOrder);
            validOrderCountList.add(validOrder);
        });

        //需要計算時間區間內的訂單總數量
        Integer totalOrderCount = orderCountList.stream().reduce(Integer::sum).get();
        //計算時間區間內的有效訂單數量
        Integer validOrderCount = validOrderCountList.stream().reduce(Integer::sum).get();

        //訂單完成率（回傳 0~1 的小數，前端再自行 *100 顯示百分比）
        Double orderCompletionRate = totalOrderCount == 0 ? 0.0 : validOrderCount.doubleValue() / totalOrderCount;

        return OrderReportVO.builder()
                .dateList(StringUtils.join(dateList, ","))
                .orderCountList(StringUtils.join(orderCountList, ","))
                .validOrderCountList(StringUtils.join(validOrderCountList, ","))
                .totalOrderCount(totalOrderCount)
                .validOrderCount(validOrderCount)
                .orderCompletionRate(orderCompletionRate)
                .build();
    }


    private Integer getOrderCount(LocalDateTime beginTime, LocalDateTime endTime, Integer status){
        Map<String , Object> map = new HashMap<>();
        map.put("beginTime", beginTime);
        map.put("endTime", endTime);
        map.put("status", status);
        return orderMapper.countByMap(map);
    }
}
