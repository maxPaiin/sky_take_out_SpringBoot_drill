package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.PasswordConstant;
import com.sky.constant.StatusConstant;
import com.sky.context.BaseContext;
import com.sky.dto.EmployeeDTO;
import com.sky.dto.EmployeeLoginDTO;
import com.sky.dto.EmployeePageQueryDTO;
import com.sky.entity.Employee;
import com.sky.exception.AccountLockedException;
import com.sky.exception.AccountNotFoundException;
import com.sky.exception.PasswordErrorException;
import com.sky.mapper.EmployeeMapper;
import com.sky.result.PageResult;
import com.sky.service.EmployeeService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class EmployeeServiceImpl implements EmployeeService {

    @Autowired
    private EmployeeMapper employeeMapper;
    @Autowired
    private EmployeeService employeeService;

    /**
     * 员工登录
     *
     * @param employeeLoginDTO
     * @return
     */
    public Employee login(EmployeeLoginDTO employeeLoginDTO) {
        String username = employeeLoginDTO.getUsername();
        String password = employeeLoginDTO.getPassword();

        //1、根据用户名查询数据库中的数据
        Employee employee = employeeMapper.getByUsername(username);

        //2、处理各种异常情况（用户名不存在、密码不对、账号被锁定）
        if (employee == null) {
            //账号不存在
            throw new AccountNotFoundException(MessageConstant.ACCOUNT_NOT_FOUND);
        }

        //密码比对,明文密碼進行md5加密後再比對
        password = DigestUtils.md5DigestAsHex(password.getBytes());

        if (!password.equals(employee.getPassword()))
            //密码错误
            throw new PasswordErrorException(MessageConstant.PASSWORD_ERROR);


        if (employee.getStatus() == StatusConstant.DISABLE)
            //账号被锁定
            throw new AccountLockedException(MessageConstant.ACCOUNT_LOCKED);


        //3、返回实体对象
        return employee;
    }

    @Override
    public void save(EmployeeDTO employeeDTO) {
        Employee employee = new Employee(); //放入持久層的話,仍需要原始的Employee實體對象
        //放入值,使用對象屬性拷貝,package org.springframework.beans;當中的函數,前提是屬性名兩者必須一致
        BeanUtils.copyProperties(employeeDTO, employee);

        //帳號狀態設置(默認),設置為可用狀態
        employee.setStatus(StatusConstant.ENABLE);

        //設置密碼,DTO裡也沒有這個,使用默認密碼
        employee.setPassword(DigestUtils.md5DigestAsHex(PasswordConstant.DEFAULT_PASSWORD.getBytes()));

        //設置當前紀錄的創建時間,系統時間
        employee.setCreateTime(LocalDateTime.now());//創建時間
        employee.setUpdateTime(LocalDateTime.now());//修改時間

        //note 用ThreadLocal技術,在攔截器中(interceptor)獲取當前登錄員工的id,然後放入ThreadLocal中(這個工具類封裝再了BaseContext中),在這裡取出來使用
        employee.setCreateUser(BaseContext.getCurrentId());
        employee.setUpdateUser(BaseContext.getCurrentId());

        //持久層注入
        employeeMapper.insert(employee);

    }

    /**
     * 分頁查詢
     * @param employeePageQueryDTO
     * @return
     */
    @Override
    public PageResult pageQuery(EmployeePageQueryDTO employeePageQueryDTO) {
        //分頁實際是依靠sql語句的查詢和limit語句來進行實現的
        //使用pageHelper
        PageHelper.startPage(employeePageQueryDTO.getPage(), employeePageQueryDTO.getPageSize());
        Page<Employee> page = employeeMapper.pageQuery(employeePageQueryDTO);
        //從Page中獲取總條數和當前頁的數據列表,並裝到PageResult當中返回
        return new PageResult(page.getTotal(), page.getResult());
    }

    /**
     * 修改員工狀態
     * @param status 狀態 1:啟用 0:禁用
     * @param id     員工ID
     */
    @Override
    public void startOrStop(Integer status, Long id) {
        //本質上是修改表的狀態status字段的值
        Employee employee = Employee.builder()
                .status(status)
                .id(id)
                .build();
        employeeMapper.update(employee);
    }

    /**
     * 查詢員工
     * @param id 員工id
     * @return 返回一個employee對象
     */
    @Override
    public Employee getById(Long id) {
        Employee employee = employeeMapper.getById(id);
        employee.setPassword("****");//不讓前端看到密碼
        return employee;
    }
    /**
     * 修改員工信息
     * @param employeeDTO 用DTO接收前端傳來的員工資料
     */
    @Override
    public void update(EmployeeDTO employeeDTO) {
        Employee employee = new Employee();
        BeanUtils.copyProperties(employeeDTO,employee);
        employee.setUpdateTime(LocalDateTime.now());
        employee.setUpdateUser(BaseContext.getCurrentId());
        employeeMapper.update(employee);
    }
}
