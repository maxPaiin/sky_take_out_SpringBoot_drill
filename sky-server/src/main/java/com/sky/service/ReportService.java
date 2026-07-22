package com.sky.service;

import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;

import java.time.LocalDate;
import java.time.LocalDateTime;

public interface ReportService {
    //統計一段時間的營業額
    TurnoverReportVO getTurnoverReport(LocalDate begin, LocalDate end);

    //統計指定時間內的用戶流量數據
    UserReportVO getUserStatistics(LocalDate begin, LocalDate end);



}
