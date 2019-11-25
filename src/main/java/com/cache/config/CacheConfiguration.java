package com.cache.config;

import com.cache.domain.CacheSpace;
import com.cache.server.CacheAttributeService;
import com.google.common.cache.CacheBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.guava.GuavaCacheManager;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author zhao tailen
 * @description 加载本地缓存
 * @date 2019-11-02
 */
@Configuration
@EnableCaching
/**
 * (mode=AdviceMode.ASPECTJ)
 * EnableCaching的mode属性，可以选择值proxy和aspectj。
 * 默认使用proxy。
 * 当mode为proxy时，只有缓存方法在外部被调用的时候才会生效。这也就意味着如果一个缓存方法在一个对象的内部被调用SpringCache是不会发生作用的。
 * 而mode为aspectj时，就不会有这种问题了。
 * 另外使用proxy的时候，只有public方法上的@Cacheable才会发生作用。
 */
public class CacheConfiguration {

    private final Logger log=LoggerFactory.getLogger(CacheConfiguration.class);

    @Resource
    private CacheAttributeService cacheAttributeService;

    @Bean
    public CacheManager createCacheManager() {

        List<CacheSpace> cacheSpaces=cacheAttributeService.handleCacheAttribute();
        SimpleCacheManager lacalCacheManager=cacheAttributeService.createCache(cacheSpaces);
        if (lacalCacheManager == null) {
            log.info("no local cache be set");
            return buildDefaultGuavaCache();
        }
        return lacalCacheManager;
    }

    private GuavaCacheManager buildDefaultGuavaCache() {
        GuavaCacheManager cacheManager=new GuavaCacheManager();
        cacheManager.setCacheBuilder(CacheBuilder.newBuilder().expireAfterAccess(3600, TimeUnit.SECONDS).maximumSize(1000));
        return cacheManager;
    }
}
