package com.sky.service.impl;

import com.sky.constant.MessageConstant;
import com.sky.constant.PasswordConstant;
import com.sky.constant.StatusConstant;
import com.sky.context.BaseContext;
import com.sky.dto.EmployeeDTO;
import com.sky.dto.EmployeeLoginDTO;
import com.sky.entity.Employee;
import com.sky.exception.AccountLockedException;
import com.sky.exception.AccountNotFoundException;
import com.sky.exception.PasswordErrorException;
import com.sky.mapper.EmployeeMapper;
import com.sky.service.EmployeeService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.time.LocalDateTime;

@Service
public class EmployeeServiceImpl implements EmployeeService {

    @Autowired
    private EmployeeMapper employeeMapper;

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

        //note 用ThreadLocal技術,在攔截器中(interceptor)獲取當前登錄員工的id,然後放入ThreadLocal中,在這裡取出來使用
        employee.setCreateUser(BaseContext.getCurrentId());
        employee.setUpdateUser(BaseContext.getCurrentId());

        //持久層注入
        employeeMapper.insert(employee);

    }
}
