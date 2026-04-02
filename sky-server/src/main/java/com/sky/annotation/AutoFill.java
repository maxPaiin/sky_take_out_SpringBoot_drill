package com.sky.annotation;

import com.sky.enumeration.OperationType;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * 自定義註解,用於標識需要填充的公共字段
 */

@Target(java.lang.annotation.ElementType.METHOD) //註解用於方法上
@Retention(java.lang.annotation.RetentionPolicy.RUNTIME) //註解在運行時可用
public @interface AutoFill {
    // 數據庫操作類型:update和insert,這裡使用的是原本準備號的枚舉類OperationType(com.sky.enumeration.OperationType)
    OperationType value();
}
