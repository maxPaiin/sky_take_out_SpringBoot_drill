package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.annotation.AutoFill;
import com.sky.dto.EmployeePageQueryDTO;
import com.sky.entity.Employee;
import com.sky.enumeration.OperationType;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface EmployeeMapper {
    /**
     * 根据用户名查询员工
     * @param username
     * @return
     */
    @Select("select * from employee where username = #{username}")
    Employee getByUsername(String username);

    /**
     * 新增員工(單表新增操作),所以沒必要寫在mappers.xml當中
     * @param employee 員工對象
     * @AutoFill 註解用於標識需要自動填充公共字段的方法,這裡是update方法,所以value屬性值為OperationType.INSERT
     */
    @Insert("insert into employee(name,username,password,phone,sex,id_number,status,create_time,update_time,create_user,update_user)"
    + "values"+"(#{name},#{username},#{password},#{phone},#{sex},#{idNumber},#{status},#{createTime},#{updateTime},#{createUser},#{updateUser})")
    @AutoFill(value = OperationType.INSERT)
    void insert(Employee employee);

    /**
     * 員工分頁查詢(動態sql查詢,寫到影射檔之中)
     * @param employeePageQueryDTO
     * @return
     */
    Page<Employee> pageQuery(EmployeePageQueryDTO employeePageQueryDTO);

    /**
     * 根據主鍵動態修改狀態
     * @param employee
     *  @AutoFill 註解用於標識需要自動填充公共字段的方法,這裡是update方法,所以value屬性值為OperationType.UPDATE
     */
    @AutoFill(value = OperationType.UPDATE)
    void update(Employee employee);

    /**
     * 根據id查詢員工信息
     * @param id 員工id
     * @return 返回一個employee對象
     */
    @Select("select * from employee where id = #{id}")
    Employee getById(Long id);
}
