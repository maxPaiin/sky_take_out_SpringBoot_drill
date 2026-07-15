package com.sky.service.impl;

import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.service.ReportService;
import com.sky.vo.TurnoverReportVO;
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
}
