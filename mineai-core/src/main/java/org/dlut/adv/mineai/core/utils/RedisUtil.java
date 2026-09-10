package org.dlut.adv.mineai.core.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * @Package：org.dlut.adv.mineai.core.utils
 * @Project：mineai
 * @name：RedisUtil
 * @Filename：RedisUtil
 * @Desc：redis工具类
 */

@Component
@Slf4j
public class RedisUtil {

    private RedisTemplate<Object, Object> redisTemplate;


    public RedisUtil(RedisTemplate<Object, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // 设置过期时间
    public boolean expire(String key, long time, TimeUnit timeUnit) {
        try {
            if (time > 0) {
                redisTemplate.expire(key, time, timeUnit);
            }
        } catch (Exception e) {
            log.error("RedisUtils expire key {} time {} error:{}", key, time, e.getMessage());
            return false;
        }
        return true;
    }

    // 获取过期时间
    public long getExpire(Object key) {
        return redisTemplate.getExpire(key, TimeUnit.SECONDS);
    }

    // 判断key是否存在
    public boolean hasKey(String key) {
        try {
            return redisTemplate.hasKey(key);
        } catch (Exception e) {
            log.error("RedisUtils hasKey key {} error:{}", key, e.getMessage());
            return false;
        }
    }

    // 删除缓存
    public void del(String... keys) {
        if (keys != null && keys.length > 0) {
            if (keys.length == 1) {
                // 如果只有一个key，直接删除
                redisTemplate.delete(keys[0]);
            } else {
                // 如果有多个key，转换为Collection
                redisTemplate.delete(Arrays.asList(keys));
            }
        }
    }

    // 获取缓存
    public Object get(String key) {
        return key == null ? null : redisTemplate.opsForValue().get(key);
    }

    // 放入缓存
    public boolean set(String key, Object value) {
        try {
            redisTemplate.opsForValue().set(key, value);
            return true;
        } catch (Exception e) {
            log.error("RedisUtils set key {} value {} error:{}", key, value, e.getMessage());
            return false;
        }
    }

    // 放入缓存 （带过期时间）
    public boolean set(String key, Object value, Long time, TimeUnit timeUnit) {
        try {
            if (time > 0) {
                redisTemplate.opsForValue().set(key, value, time, timeUnit);
            } else {
                set(key, value);
            }
            return true;
        } catch (Exception e) {
            log.error("RedisUtils set key {} value {} time {} error:{}", key, value, time, e.getMessage());
            return false;
        }
    }

    // get hash
    public Object hGet(String key, String item) {
        return redisTemplate.opsForHash().get(key, item);
    }

    // mGet hash
    public Map<Object, Object> hmGet(String key) {
        return redisTemplate.opsForHash().entries(key);
    }

    // set hash
    public boolean hSet(String key, String item, String value) {
        try {
            redisTemplate.opsForHash().put(key, item, value);
            return true;
        } catch (Exception e) {
            log.error("RedisUtils hset key {} item {} value {} error:{}", key, item, value, e.getMessage());
            return false;
        }
    }

    // set hash expire
    public boolean hSet(String key, String item, String value, long time, TimeUnit timeUnit) {
        try {
            redisTemplate.opsForHash().put(key, item, value);
            if (time > 0) {
                expire(key, time, timeUnit);
            }
            return true;
        } catch (Exception e) {
            log.error("RedisUtils hset key {} item {} value {} time {} error:{}", key, item, value, time, e.getMessage());
            return false;
        }
    }

    // mSet hash
    public boolean hmSet(String key, Map<String, Object> map) {
        try {
            redisTemplate.opsForHash().putAll(key, map);
            return true;
        } catch (Exception e) {
            log.error("RedisUtil hmSet key {} map {} error {}", key, map, e.getMessage());
            return false;
        }
    }

    // mSet hash expire
    public boolean hmSet(String key, Map<String, Object> map, long time, TimeUnit timeUnit) {
        try {
            redisTemplate.opsForHash().putAll(key, map);
            if (time > 0) {
                expire(key, time, timeUnit);
            }
            return true;
        } catch (Exception e) {
            log.error("RedisUtil hmSet key {} map {} time {} error {}", key, map, time, e.getMessage());
            return false;
        }
    }

    // del hash item
    public void hDel(String key, Object... item) {
        redisTemplate.opsForHash().delete(key, item);
    }

    // have hash item
    public boolean hHasKey(String key, String item) {
        return redisTemplate.opsForHash().hasKey(key, item);
    }

    // get hashSet
    public Set<Object> sGet(String key) {
        return redisTemplate.opsForSet().members(key);
    }

    // set hashSet
    public boolean sSet(String key, Object... values) {
        try {
            redisTemplate.opsForSet().add(key, values);
            return true;
        } catch (Exception e) {
            log.error("RedisUtils sSet key {} values {} error:{}", key, values, e.getMessage());
            return false;
        }
    }

    // set hashSet expire
    public boolean sSet(String key, long time, TimeUnit timeUnit, Object... values) {
        try {
            redisTemplate.opsForSet().add(key, values);
            if (time > 0) {
                expire(key, time, timeUnit);
            }
            return true;
        } catch (Exception e) {
            log.error("RedisUtils sSet key {} time {} values {} error:{}", key, time, values, e.getMessage());
            return false;
        }
    }

    // get hashSet size
    public long sGetSize(String key) {
        return redisTemplate.opsForSet().size(key);
    }

    // del hashSet item
    public long sDel(String key, Object... values) {
        try {
            return redisTemplate.opsForSet().remove(key, values);
        } catch (Exception e) {
            log.error("RedisUtils setRemove key {} values {} error:{}", key, values, e.getMessage());
            return 0;
        }
    }

    // set zSet
    public boolean zSet(String key, Object value, long score) {
        try {
            redisTemplate.opsForZSet().add(key, value, score);
            return true;
        } catch (Exception e) {
            log.error("RedisUtil zSet key {}  value {} error:{}", key, value, e.getMessage());
            return false;
        }
    }

    /**
     * @param key
     * @param value
     * @param score
     * @param time
     * @param timeUnit
     * @return boolean
     */// set zSet expire
    public boolean zSet(String key, Object value, long score, long time, TimeUnit timeUnit) {
        try {
            redisTemplate.opsForZSet().add(key, value, score);
            if (time > 0) {
                expire(key, time, timeUnit);
            }
            return true;
        } catch (Exception e) {
            log.error("RedisUtils zSet key {} value {} time {} error:{}", key, value, time, e.getMessage());
            return false;
        }
    }

    // get zSet
    public Set<Object> zGet(String key) {
        return redisTemplate.opsForZSet().range(key, Long.MIN_VALUE, Long.MAX_VALUE);
    }

    // del zSet
    public boolean zDel(String key, Object... items) {
        try {
            redisTemplate.opsForZSet().remove(key, items);
            return true;
        } catch (Exception e) {
            log.error("RedisUtil zDel key {} member {} error:{}", key, items, e.getMessage());
            return false;
        }
    }


}
