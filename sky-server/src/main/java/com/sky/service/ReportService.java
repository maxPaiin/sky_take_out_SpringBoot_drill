package com.sky.service;

import com.sky.vo.TurnoverReportVO;

import java.time.LocalDate;
import java.time.LocalDateTime;

public interface ReportService {
    //統計一段時間的營業額
    TurnoverReportVO getTurnoverReport(LocalDate begin, LocalDate end);

}
