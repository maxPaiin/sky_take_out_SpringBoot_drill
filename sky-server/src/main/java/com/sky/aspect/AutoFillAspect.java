package com.sky.aspect;

import com.sky.annotation.AutoFill;
import com.sky.constant.AutoFillConstant;
import com.sky.context.BaseContext;
import com.sky.enumeration.OperationType;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

/**
 * 這是一個切面類(AOP),實現公共字段自動填充
 */
@Aspect
@Component
@Slf4j
public class AutoFillAspect {
    //定義切入點,對哪些類的哪些方法進行自動填充
    @Pointcut("execution(* com.sky.mapper.*.*(..)) && @annotation(com.sky.annotation.AutoFill)")
    public void autoFillPointCut() {}

    //為公共字段賦值(代碼增強),前置通知,要在sql執行前執行
    @Before("autoFillPointCut()")
    public void autoFill(JoinPoint joinPoint) {
        log.info("開始自動填充公共字段...");
        //1.獲取到當前被攔截方法的數據庫操作類型
        MethodSignature signature = (MethodSignature) joinPoint.getSignature(); // 獲取方法簽名對象
        AutoFill annotation = signature.getMethod().getAnnotation(AutoFill.class); //獲得方法上的註解對象
        OperationType value = annotation.value(); //獲取數據庫操作類型 
        
        //2.獲取到方法的參數(一定是一個和表有關係的對象)
        Object[] args = joinPoint.getArgs(); // 獲得所有參數
        //防止空指針
        if(args == null || args.length == 0){
            return;
        }
        Object arg = args[0];//獲得了實體對象

        //3.準備賦值數據
        LocalDateTime now = LocalDateTime.now();
        Long currentId = BaseContext.getCurrentId();

        //4.根據數據庫操作類型,通過反射為對象的公共字段賦值
        if(value == OperationType.INSERT){
            //通過反射為4個公共字段賦值
            try {
                //獲取
                Method setCreateTime = arg.getClass().getDeclaredMethod(AutoFillConstant.SET_CREATE_TIME, LocalDateTime.class);
                Method setCreateUser = arg.getClass().getDeclaredMethod(AutoFillConstant.SET_CREATE_USER, Long.class);
                Method setUpdateTime = arg.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_TIME, LocalDateTime.class);
                Method setUpdateUser = arg.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_TIME, Long.class);
                //通過反射賦值
                setCreateTime.invoke(arg, now);
                setCreateUser.invoke(arg, currentId);
                setUpdateTime.invoke(arg, now);
                setUpdateUser.invoke(arg, currentId);
            } catch (Exception e) {
                e.printStackTrace();
            }

        }else if(value == OperationType.UPDATE){
            //為2個公共字段賦值
            try{
                arg.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_TIME, LocalDateTime.class).invoke(arg, now);
                arg.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_TIME, Long.class).invoke(arg, currentId);
            }catch(Exception e){
                e.printStackTrace();
            }

        }

    }
}