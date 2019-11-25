package com.cach.domain;

/**
 * @author zhao tailen
 * @description 缓存属性
 * @date 2019-11-05
 */
public class CacheSpace {

    /**
     * 缓存空间名称
     * */
    private String name;

    /**
     * value是否可以为null【强烈不推荐设置为true】
     * */
    private Boolean allowNullValues;

    /**
     * 空间内最大缓存数量
     * */
    private Integer maxSize;

    /**
     *缓存有效时间【是指一个缓存空间下的所以的缓存的生命存活<创建到死忙>时长，不区分本地和远程】
     * */
    private Integer expireDate;

    /**
     * 缓存空闲时间
     * */
    private Integer idleDate;

    /**
     * 缓存存储的优先级
     * */
    private CachePriority cachePriority;

    /**
     * 两级缓存中的本地和远程之间的切换策略
     * 突破访问次数阀【先远程后本地】
     * 空间溢出【先本地后远程】
     * */
    private CacheChangeStrategy cacheChangeStrategy;

    /**
     * 本地和远程的存储数量比率
     * 本地数量：maxSize*twoLevelsRatio/(1.0+twoLevelsRatio)
     * 远程数量：maxSize/(1.0+twoLevelsRatio)
     * */
    private Float twoLevelsRatio;

    /**
     * 访问次数阀值【先远程后本地】
     * */
    private Long accessThreshold;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name=name;
    }

    public Boolean getAllowNullValues() {
        return allowNullValues;
    }

    public void setAllowNullValues(Boolean allowNullValues) {
        this.allowNullValues=allowNullValues;
    }

    public Integer getMaxSize() {
        return maxSize;
    }

    public void setMaxSize(Integer maxSize) {
        this.maxSize=maxSize;
    }

    public Integer getExpireDate() {
        return expireDate;
    }

    public void setExpireDate(Integer expireDate) {
        this.expireDate=expireDate;
    }

    public Integer getIdleDate() {
        return idleDate;
    }

    public void setIdleDate(Integer idleDate) {
        this.idleDate=idleDate;
    }

    public CachePriority getCachePriority() {
        return cachePriority;
    }

    public void setCachePriority(CachePriority cachePriority) {
        this.cachePriority=cachePriority;
    }

    public CacheChangeStrategy getCacheChangeStrategy() {
        return cacheChangeStrategy;
    }

    public void setCacheChangeStrategy(CacheChangeStrategy cacheChangeStrategy) {
        this.cacheChangeStrategy=cacheChangeStrategy;
    }

    public Float getTwoLevelsRatio() {
        return twoLevelsRatio;
    }

    public void setTwoLevelsRatio(Float twoLevelsRatio) {
        this.twoLevelsRatio=twoLevelsRatio;
    }

    public Long getAccessThreshold() {
        return accessThreshold;
    }

    public void setAccessThreshold(Long accessThreshold) {
        this.accessThreshold=accessThreshold;
    }

    @Override
    public String toString() {
        return "CacheSpace{" +
                "name='" + name + '\'' +
                ", allowNullValues=" + allowNullValues +
                ", maxSize=" + maxSize +
                ", expireDate=" + expireDate +
                ", idleDate=" + idleDate +
                ", cachePriority=" + cachePriority +
                ", cacheChangeStrategy=" + cacheChangeStrategy +
                ", twoLevelsRatio=" + twoLevelsRatio +
                ", accessThreshold=" + accessThreshold +
                '}';
    }
}
