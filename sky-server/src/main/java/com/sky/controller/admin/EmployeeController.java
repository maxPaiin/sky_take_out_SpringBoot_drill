package com.sky.controller.admin;

import com.sky.constant.JwtClaimsConstant;
import com.sky.dto.EmployeeDTO;
import com.sky.dto.EmployeeLoginDTO;
import com.sky.dto.EmployeePageQueryDTO;
import com.sky.entity.Employee;
import com.sky.properties.JwtProperties;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.EmployeeService;
import com.sky.utils.JwtUtil;
import com.sky.vo.EmployeeLoginVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 员工管理
 */
@RestController
@RequestMapping("/admin/employee")
@Slf4j
@Api(tags = "員工相關的功能")
public class EmployeeController {

    @Autowired
    private EmployeeService employeeService;
    @Autowired
    private JwtProperties jwtProperties;

    /**
     * 登录
     *
     * @param employeeLoginDTO
     * @return
     */
    @PostMapping("/login")
    @ApiOperation(value = "員工登錄")
    public Result<EmployeeLoginVO> login(@RequestBody EmployeeLoginDTO employeeLoginDTO) {
        log.info("员工登录：{}", employeeLoginDTO);

        Employee employee = employeeService.login(employeeLoginDTO);

        //登录成功后，生成jwt令牌
        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtClaimsConstant.EMP_ID, employee.getId());
        String token = JwtUtil.createJWT(
                jwtProperties.getAdminSecretKey(),
                jwtProperties.getAdminTtl(),
                claims);

        EmployeeLoginVO employeeLoginVO = EmployeeLoginVO.builder()
                .id(employee.getId())
                .userName(employee.getUsername())
                .name(employee.getName())
                .token(token)
                .build();

        return Result.success(employeeLoginVO);
    }

    /**
     * 新增員工功能(json格式)
     *
     * @param employeeDTO 用DTO接收前端傳來的員工資料(防止前端/後端字段差異過大)
     * @return Result對象
     */
    @PostMapping
    @ApiOperation(value = "新增員工") // 功能描述
    public Result save(@RequestBody EmployeeDTO employeeDTO) {
        log.info("新增員工：{}", employeeDTO);
        employeeService.save(employeeDTO);
        return Result.success();
    }

    /**
     * 員工分頁查詢
     *
     * @param employeePageQueryDTO
     * @return
     */
    @GetMapping("/page")
    @ApiOperation(value = "員工分頁查詢")
    public Result<PageResult> page(EmployeePageQueryDTO employeePageQueryDTO) {
        log.info("員工分頁查詢：{}", employeePageQueryDTO);
        PageResult pageResult = employeeService.pageQuery(employeePageQueryDTO);
        return Result.success(pageResult);
    }

    /**
     * 員工退出
     * @return
     */
    @PostMapping("/logout")
    @ApiOperation(value = "員工登出")
    public Result<String> logout() {
        return Result.success();
    }

    /**
     * 修改員工狀態
     * PathVariable：從URL路徑中獲取參數
     * @param status 狀態 1:啟用 0:禁用
     * @param id 員工id
     * @return 返回狀態
     */
    @PostMapping("/status/{status}") //當形參和從參數取值的名一致時,不需要再在註解上寫()
    @ApiOperation(value = "修改員工狀態") // 功能描述
    public Result startOrStop(@PathVariable Integer status , Long id){
        log.info("啟用/禁用員工帳號:{},{}",status,id);
        employeeService.startOrStop(status,id);
        return Result.success();
    }
}
