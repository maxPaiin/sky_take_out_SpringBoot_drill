package com.sky.service;

import com.sky.dto.EmployeeDTO;
import com.sky.dto.EmployeeLoginDTO;
import com.sky.dto.EmployeePageQueryDTO;
import com.sky.entity.Employee;
import com.sky.result.PageResult;
import org.springframework.web.bind.annotation.PathVariable;

public interface EmployeeService {

    /**
     * 员工登录
     *
     * @param employeeLoginDTO
     * @return
     */
    Employee login(EmployeeLoginDTO employeeLoginDTO);

    /**
     * 新增員工
     *
     * @param employeeDTO
     * @return
     */
    void save(EmployeeDTO employeeDTO);

    /**
     * 員工分頁查詢
     * @param employeePageQueryDTO
     * @return
     */
    PageResult pageQuery(EmployeePageQueryDTO employeePageQueryDTO);

    void startOrStop(Integer status, Long id);

    /**
     * 查詢員工信息
     * @param id 員工id
     * @return
     */
    Employee getById(Long id);

    /**
     * 修改員工信息
     * @param employeeDTO 用DTO接收前端傳來的員工資料
     */
    void update(EmployeeDTO employeeDTO);
}
