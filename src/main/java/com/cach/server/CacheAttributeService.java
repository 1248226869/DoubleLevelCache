package com.cach.server;

import com.cach.config.CacheAttributeYmlConfig;
import com.cach.domain.CacheSpace;
import com.cach.remote.RemoteCacha;
import com.cach.doubleLevel.DoubleLevel;
import com.cach.domain.CacheChangeStrategy;
import com.cach.domain.CachePriority;
import com.cach.local.LocalCacha;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;

/**
 * @author zhao tailen
 * @description 解析缓存空间
 * @date 2019-11-06
 */
@Component

public class CacheAttributeService {

    private final Logger log=LoggerFactory.getLogger(CacheAttributeService.class);
    @Autowired
    private RedissonClient redissonClient;
    @Resource
    private CacheAttributeYmlConfig cacheAttributeYmlConfig;

    private static final int MAX_SIZE=1000;
    private static final int EXPIRE_DATE=3600;
    private static final int IDLE=1800;
    private static final float TWOLEVELS_RATIO=0.5f;
    private static final long ACCESS_THRESHOLD=10000;
    private HashSet<String> repeatCacheNameList=new HashSet<>();


    public List<CacheSpace> handleCacheAttribute() {
        CacheSpaceContainer cacheSpaceContainer=CacheSpaceContainer.getCacheSpaceContainer();
        List<CacheSpace> cacheSpaceList=cacheSpaceContainer.getCacheSpaces();
        if (cacheSpaceList == null) {
            cacheSpaceList=new ArrayList<CacheSpace>();
        }else {
            //处理静态代码块中的CacheSpace
            for (int i=0; i<cacheSpaceList.size();i++){
                CacheSpace cacheSpace=cacheSpaceList.get(i);

                if (cacheSpace == null || StringUtils.isBlank(cacheSpace.getName())){
                    continue;
                }

                cacheSpaceList.set(i,handleCacheSpaceInStatic(cacheSpace));
            }
        }
        List<Map<String, String>> cacheSpaceAttributeMapList=cacheAttributeYmlConfig.getLimitSizeList();

        if (cacheSpaceAttributeMapList == null || cacheSpaceAttributeMapList.size() == 0) {
            return cacheSpaceList;
        }
        for (Map<String, String> cacheSpaceAttributeMap : cacheSpaceAttributeMapList) {
            if (StringUtils.isBlank(cacheSpaceAttributeMap.get("name"))){
                continue;
            }
            CacheSpace cacheSpaca=createCacheSpaca(cacheSpaceAttributeMap);
            cacheSpaca.setAllowNullValues(false);
            cacheSpaceList.add(cacheSpaca);
        }
        return cacheSpaceList;
    }

    public SimpleCacheManager createCache(List<CacheSpace> cacheSpaceList) {

        if (cacheSpaceList == null || cacheSpaceList.size() == 0) {
            log.info("no cache to created");
            return null;
        }

        List<Cache> cacheList=new ArrayList<Cache>();

        for (CacheSpace cacheSpace : cacheSpaceList) {
            log.debug("cache is {}",cacheSpace.toString());
            if (Objects.isNull(cacheSpace.getName())) {
                continue;
            }

            CachePriority cachePriority=cacheSpace.getCachePriority();
            if (cachePriority == null) {
                continue;
            }

            if (!repeatCacheNameList.add(cacheSpace.getName())) {
                continue;
            }

            if (CachePriority.ONLY_LOCAL.equals(cachePriority)) {
                LocalCacha localCachaComponent=new LocalCacha(cacheSpace);
                log.info("load local cache <{}> success", cacheSpace.getName());
                cacheList.add(localCachaComponent);
                continue;
            }

            if (CachePriority.ONLY_REMOTE.equals(cachePriority)) {
                RemoteCacha remoteCachaComponent=new RemoteCacha(cacheSpace, redissonClient);
                log.info("load remote cache < {} > success", cacheSpace.getName());
                cacheList.add(remoteCachaComponent);
                continue;
            }

            DoubleLevel twoLevelsCacheComponent=new DoubleLevel(cacheSpace, redissonClient);
            log.info("load doubleLevel cache < {} > success", cacheSpace.getName());
            cacheList.add(twoLevelsCacheComponent);
        }
        SimpleCacheManager cacheManager=new SimpleCacheManager();
        cacheManager.setCaches(cacheList);
        return cacheManager;
    }

    private CacheSpace handleCacheSpaceInStatic(CacheSpace cacheSpace){
        if (cacheSpace.getExpireDate() == null){
             cacheSpace.setExpireDate(EXPIRE_DATE);
        }
        if (cacheSpace.getIdleDate() == null){
            cacheSpace.setIdleDate(IDLE);
        }
        if (cacheSpace.getMaxSize() == null){
            cacheSpace.setMaxSize(MAX_SIZE);
        }
        if (cacheSpace.getAccessThreshold() == null){
            cacheSpace.setAccessThreshold(ACCESS_THRESHOLD);
        }
        if (cacheSpace.getTwoLevelsRatio() == null){
            cacheSpace.setTwoLevelsRatio(TWOLEVELS_RATIO);
        }
        return cacheSpace;
    }

    private CacheSpace createCacheSpaca(Map<String, String> cacheSpaceAttributeMap) {
        CacheSpace cacheSpace=new CacheSpace();
        if (!Objects.isNull(cacheSpaceAttributeMap.get("name"))) {
            cacheSpace.setName(cacheSpaceAttributeMap.get("name"));
        }

        if (!Objects.isNull(cacheSpaceAttributeMap.get("allowNullValues"))) {
            cacheSpace.setAllowNullValues(Boolean.valueOf(cacheSpaceAttributeMap.get("allowNullValues")));
        }

        cacheSpace.setMaxSize(NumberUtils.toInt(cacheSpaceAttributeMap.get("maxSize"), MAX_SIZE));

        cacheSpace.setExpireDate(NumberUtils.toInt(cacheSpaceAttributeMap.get("expireDate"), EXPIRE_DATE));

        cacheSpace.setIdleDate(NumberUtils.toInt(cacheSpaceAttributeMap.get("idleDate"), IDLE));

        if (!Objects.isNull(cacheSpaceAttributeMap.get("cachePriority"))) {
            cacheSpace.setCachePriority(handleCachePriority(cacheSpaceAttributeMap.get("cachePriority")));
        }

        if (!Objects.isNull(cacheSpaceAttributeMap.get("cacheChangeStrategy"))) {
            cacheSpace.setCacheChangeStrategy(handleCacheChangeStrategy(cacheSpaceAttributeMap.get("cacheChangeStrategy")));
        }

        cacheSpace.setTwoLevelsRatio(NumberUtils.toFloat(cacheSpaceAttributeMap.get("twoLevelsRatio"), TWOLEVELS_RATIO));

        cacheSpace.setAccessThreshold(NumberUtils.toLong(cacheSpaceAttributeMap.get("accessThreshold"), ACCESS_THRESHOLD));

        return cacheSpace;
    }

    private CachePriority handleCachePriority(String cachePriortyStr) {
        if (cachePriortyStr.equals("remote")) {
            return CachePriority.ONLY_REMOTE;
        }

        if (cachePriortyStr.equals("local")) {
            return CachePriority.ONLY_LOCAL;
        }

        if (cachePriortyStr.equals("firstLocal")) {
            return CachePriority.FIRST_LOCAL;
        }

        if (cachePriortyStr.equals("firstRemote")) {
            return CachePriority.FIRST_REMOTE;
        }

        if (cachePriortyStr.equals("localRemote")) {
            return CachePriority.LOCAL_REMOTE;
        }

        return CachePriority.ONLY_LOCAL;
    }

    private CacheChangeStrategy handleCacheChangeStrategy(String cachePriortyStr) {
        if (cachePriortyStr.equals("accessThreshold")) {
            return CacheChangeStrategy.ACCESS_THRESHOLD;
        }

        if (cachePriortyStr.equals("overflowMaxSize")) {
            return CacheChangeStrategy.OVERFLOW_MAX_SIZE;
        }

        return CacheChangeStrategy.OVERFLOW_MAX_SIZE;
    }

}
