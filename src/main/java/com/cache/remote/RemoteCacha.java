package com.cache.remote;

import com.cache.domain.CacheSpace;
import org.redisson.api.RMapCache;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.support.AbstractValueAdaptingCache;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

/**
 * @author zhao tailen
 * @description 远程缓存
 * @date 2019-11-14
 */
public class RemoteCacha extends AbstractValueAdaptingCache {

    private final Logger log=LoggerFactory.getLogger(RemoteCacha.class);

    private RedissonClient redissonClient;

    private RMapCache<Object, Object> remoteCache;

    private volatile CacheSpace cacheSpace;

    private int maxRemoteSize;

    private int expireDate;

    private int idleDate;

    private String name;


    /**
     * @param cacheSpace     缓存属性
     * @param redissonClient redission客户端
     * @description {@code AbstractValueAdaptingCache}创建一个远程缓存
     * @author zhao tailen
     * @date 2019-11-15 14:37
     */
    public RemoteCacha(CacheSpace cacheSpace, RedissonClient redissonClient) {
        super(cacheSpace.getAllowNullValues());
        this.cacheSpace=cacheSpace;
        this.redissonClient=redissonClient;
        this.name=cacheSpace.getName();
        this.expireDate=cacheSpace.getExpireDate();
        this.idleDate=cacheSpace.getIdleDate();
        this.maxRemoteSize=cacheSpace.getMaxSize();
        init();
    }

    /**
     * @description 初始化远程缓存
     * @author zhao tailen
     * @date 2019-11-15 14:39
     */
    private void init() {
        this.remoteCache=redissonClient.getMapCache(this.name);
        remoteCache.trySetMaxSize(maxRemoteSize);
    }


    @Override
    protected Object lookup(Object key) {


        Object value=remoteCache.get(key);
        log.debug("RemoteCacha: get cache from remoteCache >>>>>>>>>> the key is {} and value is {}", key, value);
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
        log.debug("RemoteCacha: get cache.......ValueWrapper");
        Object value=lookup(key);
        return toValueWrapper(value);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T get(Object key, Callable<T> valueLoader) {
        log.debug("RemoteCacha: get cache.......<T> ");
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

        log.debug("RemoteCacha: put remote >>>>>>>>>>>> key is {} value is {}", key, value);
        remoteCache.putIfAbsent(key, value, expireDate, TimeUnit.SECONDS, idleDate, TimeUnit.SECONDS);

    }

    @Override
    public ValueWrapper putIfAbsent(Object key, Object value) {
        log.debug("RemoteCacha: putIfAbsent remote >>>>>>>>>>>> key is {} value is {}", key, value);
        remoteCache.putIfAbsent(key, value, expireDate, TimeUnit.SECONDS, idleDate, TimeUnit.SECONDS);
        return toValueWrapper(value);
    }

    @Override
    public void evict(Object key) {
        log.debug("RemoteCacha: clear remotr cache, the key is : {}", key);
        remoteCache.remove(key);

    }

    @Override
    public void clear() {
        log.debug("RemoteCacha: clear all {}'cache ", cacheSpace.getName());
        remoteCache.deleteAsync();
    }

}

