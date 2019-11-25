package com.cach.local;

import com.github.benmanes.caffeine.cache.RemovalCause;
import com.github.benmanes.caffeine.cache.RemovalListener;
import org.redisson.api.RMapCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * @author zhao tailen
 * @description caffeine'remove listener
 * @date 2019-11-15
 */
public class LocalCacheRemovalListener implements RemovalListener<Object, Object> {
    private final Logger log=LoggerFactory.getLogger(LocalCacheRemovalListener.class);

    private RMapCache<Object, Object> remoteCache;
    private int expireDate;
    private int idleDate;

    public LocalCacheRemovalListener(RMapCache<Object, Object> remoteCache, int expireDate, int idleDate) {
        this.remoteCache=remoteCache;
        this.expireDate=expireDate;
        this.idleDate=idleDate;
    }

    @Override
    public void onRemoval(Object o, Object o2, RemovalCause removalCause) {
        //只监听缓存溢出maxSize时的驱逐
        if ("SIZE".equals(removalCause.name())) {
            // save redis
            //TODO 计算剩余有效时间
            log.debug(  " {} :  remove  key {} &  value is  {}" ,removalCause.name(), o.toString() ,o2.toString());
            remoteCache.putIfAbsent(o, o2, expireDate, TimeUnit.SECONDS, idleDate, TimeUnit.SECONDS);
        }
    }
}
