package com.cach.server;

import com.cach.domain.CacheSpace;

import java.util.ArrayList;
import java.util.List;

/**
 * @author zhao tailen
 * @description container集装箱
 * @date 2019-11-14
 */
public class CacheSpaceContainer {

    private volatile  List<CacheSpace> cacheSpaces;
    public static volatile CacheSpaceContainer cacheSpaceContainer;

    private CacheSpaceContainer() {
        super();
        cacheSpaces=new ArrayList<>();
    }

    public static CacheSpaceContainer getCacheSpaceContainer() {
        if (cacheSpaceContainer != null) {
            return cacheSpaceContainer;
        }

        synchronized (CacheSpaceContainer.class) {
            if (cacheSpaceContainer != null) {
                return cacheSpaceContainer;
            }

            cacheSpaceContainer=new CacheSpaceContainer();
            return cacheSpaceContainer;
        }
    }

    public synchronized boolean add(CacheSpace cacheSpace) {
        return cacheSpaces.add(cacheSpace);
    }

    public synchronized List<CacheSpace> getCacheSpaces() {
        return cacheSpaces;
    }

}
