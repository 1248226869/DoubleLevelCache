package com.cache.doubleLevel;

import com.cache.domain.CacheChangeStrategy;
import com.cache.domain.CachePriority;
import com.cache.domain.CacheSpace;
import com.cache.local.LocalCacheRemovalListener;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.redisson.api.RMapCache;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.support.AbstractValueAdaptingCache;

import java.math.BigDecimal;
import java.util.concurrent.*;

/**
 * @author zhao tailen
 * @description 两级缓存实现
 * @date 2019-11-14
 */
public class DoubleLevel extends AbstractValueAdaptingCache {

    private final Logger log=LoggerFactory.getLogger(DoubleLevel.class);

    private RedissonClient redissonClient;

    private RMapCache<Object, Object> remoteCache;

    private Cache<Object, Object> localCache;

    private volatile CacheSpace cacheSpace;

    private int maxLocalSize;

    private int maxRemoteSize;

    private int expireDate;

    private int idleDate;

    private String name;

    private long accessThreshold;

    private CacheChangeStrategy cacheChangeStrategy;

    private CachePriority cachePriority;

    private volatile ConcurrentHashMap<Object, Long> accessThresholdMap=null;

    private ConcurrentHashMap<Object, Boolean> lockMap=null;

    private final Long basiLocal=0L;

    private ThreadPoolExecutor executor;


    /**
     * @param cacheSpace     缓存属性
     * @param redissonClient redission客户端
     * @description {@code AbstractValueAdaptingCache}创建一个二级缓存
     * @author zhao tailen
     * @date 2019-11-15 14:37
     */
    public DoubleLevel(CacheSpace cacheSpace, RedissonClient redissonClient) {
        super(cacheSpace.getAllowNullValues());
        log.debug("cacheSpace is {}", cacheSpace.toString());
        this.cacheSpace=cacheSpace;
        this.redissonClient=redissonClient;
        this.name=cacheSpace.getName();
        this.expireDate=cacheSpace.getExpireDate();
        this.idleDate=cacheSpace.getIdleDate();
        this.cachePriority=cacheSpace.getCachePriority();
        this.cacheChangeStrategy=cacheSpace.getCacheChangeStrategy();
        this.accessThreshold=cacheSpace.getAccessThreshold();
        init();

    }

    public void init() {
        // initing thread pool
        executor=new ThreadPoolExecutor(
                20, 20, 60, TimeUnit.SECONDS,
                new LinkedBlockingDeque<Runnable>(1000), new ThreadPoolExecutor.CallerRunsPolicy());

        // initing remote cache if {twoLevelsCache} is true
        this.maxRemoteSize=getRemoteMaxSize();
        initRemoteCache();

        // initing local cache
        this.maxLocalSize=getLocalMaxSize();
        initLocalCache();
    }


    /**
     * @description 初始化远程缓存
     * @author zhao tailen
     * @date 2019-11-15 14:39
     */
    private void initRemoteCache() {
        this.remoteCache=redissonClient.getMapCache(this.name);
        remoteCache.trySetMaxSize(maxRemoteSize);
    }

    /**
     * @description 初始化本地缓存
     * @author zhao tailen
     * @date 2019-11-15 14:39
     */
    private void initLocalCache() {
        lockMap=new ConcurrentHashMap<Object, Boolean>();
        accessThresholdMap=new ConcurrentHashMap<Object, Long>(maxLocalSize);

        if (CacheChangeStrategy.OVERFLOW_MAX_SIZE.equals(cacheChangeStrategy)) {
            log.debug("先本地后远程");
            localCache=Caffeine.newBuilder()
                    .initialCapacity(maxLocalSize)
                    .maximumSize(maxLocalSize)
                    .expireAfterAccess(idleDate, TimeUnit.SECONDS)
                    .expireAfterWrite(expireDate, TimeUnit.SECONDS)
                    // set remova lListener that overflow maxSize
                    .removalListener(new LocalCacheRemovalListener(remoteCache, expireDate, idleDate))
                    .build();
            return;
        }

        localCache=Caffeine.newBuilder()
                .initialCapacity(maxLocalSize)
                .maximumSize(maxLocalSize)
                .expireAfterAccess(idleDate, TimeUnit.SECONDS)
                .expireAfterWrite(expireDate, TimeUnit.SECONDS)
                .build();
    }

    @Override
    protected Object lookup(Object key) {
        Object value=localCache.getIfPresent(key);
        if (value != null) {
            log.debug("DoubleLevel: get cache from localCache >>>>>>>>>> the key is {} and value is {}", key, value);
            return value;
        }

        log.debug("DoubleLevel: getn't cache from localCache,the key is {}", key);
        value=remoteCache.get(key);
        log.debug("DoubleLevel: get cache from remoteCache >>>>>>>>>> the key is {} and value is {}", key, value);

        if (value != null && CacheChangeStrategy.ACCESS_THRESHOLD.equals(cacheChangeStrategy)) {
            final Object v=value;
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    changeCacheStrategy(key, v);
                }
            });
        }
        return value;
    }

    public void changeCacheStrategy(Object key, Object value) {
        Long v=accessThresholdMap.get(key);
        synchronized (v == null ? basiLocal : v) {
            if (v == null) {
                accessThresholdMap.put(key, 1L);
                return;
            }
            long oldAccessThreshold=accessThresholdMap.get(key);
            long newAccessThreshold=oldAccessThreshold + 1;
            log.debug("oldAccessThreshold is {}", oldAccessThreshold);
            log.debug("newAccessThreshold is {}", newAccessThreshold);
            accessThresholdMap.put(key, newAccessThreshold);
            if (newAccessThreshold > accessThreshold) {
                localCache.put(key, value);
            }
        }
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
        log.debug("DoubleLevel: get cache.......ValueWrapper");
        Object value=lookup(key);
        return toValueWrapper(value);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T get(Object key, Callable<T> valueLoader) {
        log.debug("DoubleLevel: get cache.......<T> ");
        Object value=lookup(key);
        if (value != null) {
            return (T) value;
        }

        value=lookup(key);
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

        log.debug("{}.equals({}) {}",
                CachePriority.FIRST_REMOTE.toString(),
                cachePriority.toString(),
                CachePriority.FIRST_REMOTE.toString().equals(cachePriority.toString()));

        log.debug("1 {}.equals({}) {}",
                CachePriority.FIRST_REMOTE,
                cachePriority,
                CachePriority.FIRST_REMOTE.equals(cachePriority));
        if (CachePriority.LOCAL_REMOTE.equals(cachePriority)) {
            log.debug("DoubleLevel: put local & remote >>>>>>>>>>>> key is {} value is {}", key, value);
            localCache.put(key, value);
            remoteCache.putIfAbsent(key, value, expireDate, TimeUnit.SECONDS, idleDate, TimeUnit.SECONDS);
        }

        if (CachePriority.FIRST_LOCAL.equals(cachePriority)) {
            log.debug("put local >>>>>>>>>>>> key is {} value is {}", key, value);
            localCache.put(key, value);
        }

        if (CachePriority.FIRST_REMOTE.equals(cachePriority)) {
            log.debug("DoubleLevel: put remote >>>>>>>>>>>> key is {} value is {}", key, value);
            remoteCache.putIfAbsent(key, value, expireDate, TimeUnit.SECONDS, idleDate, TimeUnit.SECONDS);
        }

    }

    @Override
    public ValueWrapper putIfAbsent(Object key, Object value) {

        if (CachePriority.LOCAL_REMOTE.equals(cachePriority)) {
            log.debug("DoubleLevel: putIfAbsent local & remote >>>>>>>>>>>> key is {} value is {}", key, value);
            localCache.put(key, value);
            remoteCache.putIfAbsent(key, value, expireDate, TimeUnit.SECONDS, idleDate, TimeUnit.SECONDS);
        }

        if (CachePriority.FIRST_LOCAL.equals(cachePriority)) {
            log.debug("DoubleLevel: putIfAbsent local >>>>>>>>>>>> key is {} value is {}", key, value);
            localCache.put(key, value);
        }

        if (CachePriority.FIRST_REMOTE.equals(cachePriority)) {
            log.debug("DoubleLevel: putIfAbsent remote >>>>>>>>>>>> key is {} value is {}", key, value);
            remoteCache.putIfAbsent(key, value, expireDate, TimeUnit.SECONDS, idleDate, TimeUnit.SECONDS);
        }
        return toValueWrapper(value);
    }

    @Override
    public void evict(Object key) {
        clearLocal(key);
        clearRemote(key);
        accessThresholdMap.remove(key);
        lockMap.remove(key);
    }

    @Override
    public void clear() {
        log.debug("DoubleLevel: clear all {}'cache ", cacheSpace.getName());
        remoteCache.deleteAsync();
        localCache.invalidateAll();
        accessThresholdMap.clear();
        lockMap.clear();
    }

    private void clearLocal(Object key) {
        log.debug("DoubleLevel: clear local cache, the key is : {}", key);
        localCache.invalidate(key);
    }

    private void clearRemote(Object key) {
        remoteCache.remove(key);
        log.debug("DoubleLevel: clear remotr cache, the key is : {}", key);
    }

    private int getRemoteMaxSize() {
        BigDecimal bigDecimalMaxSize=new BigDecimal(cacheSpace.getMaxSize());
        BigDecimal bigDecimalRatio=new BigDecimal(cacheSpace.getTwoLevelsRatio());
        BigDecimal bigDecimal1=new BigDecimal(1);
        BigDecimal size=bigDecimalMaxSize.divide(bigDecimalRatio.add(bigDecimal1));
        return size.intValue();
    }

    private int getLocalMaxSize() {
        BigDecimal bigDecimalMaxSize=new BigDecimal(cacheSpace.getMaxSize());
        BigDecimal bigDecimalRatio=new BigDecimal(cacheSpace.getTwoLevelsRatio());
        BigDecimal bigDecimal1=new BigDecimal(1);
        BigDecimal size=bigDecimalMaxSize.multiply(bigDecimalRatio).divide(bigDecimalRatio.add(bigDecimal1));
        return size.intValue();
    }
}
