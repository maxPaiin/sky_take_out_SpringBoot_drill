package com.sky.service;

import com.sky.vo.OrderReportVO;
import com.sky.vo.SalesTop10ReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;

import java.time.LocalDate;
import java.time.LocalDateTime;

public interface ReportService {
    //統計一段時間的營業額
    TurnoverReportVO getTurnoverReport(LocalDate begin, LocalDate end);

    //統計指定時間內的用戶流量數據
    UserReportVO getUserStatistics(LocalDate begin, LocalDate end);

    //指定時間內的訂單數據
    OrderReportVO getOrderStatistics(LocalDate begin, LocalDate end);

    //銷量排行數據統計
    SalesTop10ReportVO getSalesTop10Report(LocalDate begin, LocalDate end);

}
