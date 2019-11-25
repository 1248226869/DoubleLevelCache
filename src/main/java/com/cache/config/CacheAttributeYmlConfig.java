package com.cache.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author zhao tailen
 * @description  获取客户配置的缓存空间
 * @date 2019-11-01
 */
@Configuration
@ConfigurationProperties(prefix = "cache.creater")
public class CacheAttributeYmlConfig {
    private List<Map<String,String>> limitSizeList = new ArrayList<>();

    public List<Map<String,String>> getLimitSizeList() {
        return limitSizeList;
    }

    public void setLimitSizeList( List<Map<String,String>> limitSizeList) {
        this.limitSizeList = limitSizeList;
    }
}
