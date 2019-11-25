package com.cach.local;

import com.cach.domain.CacheSpace;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.support.AbstractValueAdaptingCache;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

/**
 * @author zhao tailen
 * @description caffeine for local cache
 * @date 2019-11-18
 */
public class LocalCacha extends AbstractValueAdaptingCache {

    private final Logger log=LoggerFactory.getLogger(LocalCacha.class);

    private Cache<Object, Object> localCache;

    private volatile CacheSpace cacheSpace;

    private int maxSize;

    private int expireDate;

    private int idleDate;

    private String name;

    /**
     * @param cacheSpace     缓存属性
     * @description {@code AbstractValueAdaptingCache}创建一个本地缓存
     * @author zhao tailen
     * @date 2019-11-15 14:37
     */
    public LocalCacha(CacheSpace cacheSpace) {
        super(cacheSpace.getAllowNullValues());
        this.cacheSpace=cacheSpace;
        this.name=cacheSpace.getName();
        this.expireDate=cacheSpace.getExpireDate();
        this.idleDate=cacheSpace.getIdleDate();
        this.maxSize=cacheSpace.getMaxSize();
        init();
    }

    /**
     * @description 初始化本地缓存
     * @author zhao tailen
     * @date 2019-11-15 14:39
     */
    private void init() {

        localCache=Caffeine.newBuilder()
                .initialCapacity(maxSize)
                .maximumSize(maxSize)
                .expireAfterAccess(idleDate, TimeUnit.SECONDS)
                .expireAfterWrite(expireDate, TimeUnit.SECONDS)
                .build();
    }

    @Override
    protected Object lookup(Object key) {
        Object value=localCache.getIfPresent(key);
        log.debug("LocalCacha:get cache from localCache >>>>>>>>>> the key is {} and value is {}", key, value);
        return value;
    }


    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public Object getNativeCache() {
        return this;
    }

    @Override
    public ValueWrapper get(Object key) {
        log.debug("LocalCacha:get cache by ValueWrapper;");
        Object value=lookup(key);
        return toValueWrapper(value);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T get(Object key, Callable<T> valueLoader) {
        log.debug("LocalCacha:get cache by <T> ");
        Object value=lookup(key);
        if (value != null) {
            return (T) value;
        }

        try {
            value=valueLoader.call();
        } catch (Exception e) {
            e.printStackTrace();
        }
        Object storeValue=toStoreValue(value);
        put(key, storeValue);
        return (T) value;

    }

    @Override
    public void put(Object key, Object value) {
        if (!super.isAllowNullValues() && value == null) {
            this.evict(key);
            return;
        }

        log.debug("LocalCacha:put local cache >>>>>>>>>>>> key is {} value is {}", key, value);
        localCache.put(key, value);
    }

    @Override
    public ValueWrapper putIfAbsent(Object key, Object value) {

        log.debug("LocalCacha:putIfAbsent local cache >>>>>>>>>>>> key is {} value is {}", key, value);
        localCache.put(key, value);
        return toValueWrapper(value);
    }

    @Override
    public void evict(Object key) {
        log.debug("LocalCacha:clear local cache, the key is : {}", key);
        localCache.invalidate(key);
    }

    @Override
    public void clear() {
        log.debug("LocalCacha:clear all {}'cache ", cacheSpace.getName());
        localCache.invalidateAll();
    }

}

