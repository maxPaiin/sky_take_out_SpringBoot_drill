package com.sky.config;

import com.sky.properties.AliOssProperties;
import com.sky.utils.AliOssUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 配置類,用於創建AliOssUtil對象
 * AliOssProperties因為已經被@ConfigurationProperties註解標註了(會去讀取application.yml中的配置),所以它會被Spring容器管理,
 * 可以直接在這裡注入它,然後從它裡面獲取配置的屬性值,創建AliOssUtil對象
 */
@Configuration
@Slf4j
public class OssConifguration {

    //這裡需要@Bean註解,這樣項目啟動時,就會放入Spring容器中,以後需要使用AliOssUtil對象的地方,就可以直接注入它
    @Bean
    @ConditionalOnBean//保證在容器中,只有一個AliOssProperties對象,當沒有這種Bean時,再進行創建
    public AliOssUtil aliOssUtil(AliOssProperties aliOssProperties) {
        log.info("開始創建阿里雲文件上傳工具類對象,參數:{}", aliOssProperties);
        return new AliOssUtil(
                aliOssProperties.getEndpoint(),
                aliOssProperties.getAccessKeyId(),
                aliOssProperties.getAccessKeySecret(),
                aliOssProperties.getBucketName()
        );
    }
}
