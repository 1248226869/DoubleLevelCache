# 使用文档

## 使用说明
1. 导包。
```
<dependency>
    <groupId>com.chaoxing.academy.component</groupId>
    <artifactId>spring-cacheComponent</artifactId>
    <version>{component.cache.version}</version>
</dependency>
```


2. 属性详情：

   |      属性名称       |              属性说明              |        类型         | 是否必填 | 默认  |
   | :-----------------: | :--------------------------------: | :-----------------: | :------: | ----- |
   |        name         |            缓存空间名称            |       String        |    是    | -     |
   |   allowNullValues   | value是否可以为null【强烈不推荐】  |       boolean       |    否    |       |
   |       maxSize       |         空间内最大缓存数量         |         int         |    否    | 1000  |
   |     expireDate      |         缓存有效时间【秒】         |         Int         |    否    | 3600  |
   |      idleDate       |         缓存空闲时间【秒】         |         Int         |    否    | 1800  |
   |    cachePriority    |          缓存存储的优先级          |    CachePriority    |    否    | -     |
   | cacheChangeStrategy |   缓存的本地和远程之间的切换策略   | CacheChangeStrategy |    否    | -     |
   |   twoLevelsRatio    |      本地和远程的存储数量比率      |        float        |    否    | 0.5f  |
   |   accessThreshold   | 访问次数阀值【适用于先远程后本地】 |         Int         |    否    | 10000 |

   * CachePriority枚举

   |   属性名称   |                 说明                 |
   | :----------: | :----------------------------------: |
   |  ONLY_LOCAL  |               只存本地               |
   | ONLY_REMOTE  |               只存远程               |
   | FIRST_LOCAL  |     先存本地，空间溢出之后存远程     |
   | FIRST_REMOTE | 先存远程，达到访问次数阀值之后存本地 |
   | LOCAL_REMOTE |          本地和远程调试存储          |

   * CacheChangeStrategy枚举

   | 属性名称          | 说明                     |
   | ----------------- | ------------------------ |
   | ACCESS_THRESHOLD  | ACCESS_THRESHOLD         |
   | OVERFLOW_MAX_SIZE | 空间溢出【先本地后远程】 |

   

3. 设置缓存[list]：
* 在application.yml中设置。示例如下:

```
cache:
  creater:
    limitSizeList[0]:
      cachePriority: local
      name: local
      maxSize: 6
    limitSizeList[1]:
      cachePriority: remote
      name: remote
      maxSize: 6
    limitSizeList[2]:
      cachePriority: firstRemote
      name: firstRemote@accessThreshold
      twoLevelsRatio: 0.5
      maxSize: 6
      accessThreshold: 3
      cacheChangeStrategy: accessThreshold
    limitSizeList[3]:
      cachePriority: firstLocal
      name: firstLocal&overflowMaxSize
      maxSize: 6
      cacheChangeStrategy: overflowMaxSize
      twoLevelsRatio: 0.5
    limitSizeList[4]:
      cachePriority: localRemote
      name: localRemote
      twoLevelsRatio: 0.5
      maxSize: 6
```

4. 在需要缓存的方法上添加@Cacheable创建、使用缓存，@CacheEvict清理指定key的缓存【Springboot中的注解】。如下示例:

```

@Cacheable( value = "name",  //使用name缓存
            key = "#id + '/live/test'", //缓存的key为动态的id+静态的''/live/test'
            condition = "#id != '' ", //过滤掉参数id为空的情况
            unless = "#result == null" //过滤掉执行结果为空的情况
    )
```
```
   @CacheEvict( value = "name",  //清除{name}缓存
            key = "#name + '/name/local/get'", //缓存的key为动态的id+静态的''/live/test'
            condition = "#name != '' "  //过滤掉参数id为空的情况
    )
```
5. 在redisson-config.yml配置redis的连接属性
6. 缓存驱逐策略均为LRU
7. 注意：若缓存结果为Obj，则该Obj需要实现序列化，并且有无参构造函数
