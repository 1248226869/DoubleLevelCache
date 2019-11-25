package com.cach.server;

/**
 * @author zhao tailen
 * @description
 * @date 2019-11-07
 */
public interface CacheServer {

    void initCache();

    void putCache(Object key,Object value);

    Object getCache(Object key);
    boolean removeCache(String cacheSpaceName);

    boolean removeAllCache();

}
