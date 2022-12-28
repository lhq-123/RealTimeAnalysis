package com.alex.web.service;

import com.alex.web.bean.RepBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @Description 统计数据处理业务类
 */
@Service
public class CountService {
    @Resource
    private RedisTemplate redisTemplate;

    /**
     * 获取k/v缓存值
     * @param t
     * @param key
     * @param <T>
     * @return
     */
    public <T> Object getDataStreamCountValue(Class<T> t, final String key){
        String value = (String) redisTemplate.opsForValue().get(key);
        if (Integer.TYPE == t){
            return Integer.parseInt(value);
        } else if(Double.TYPE == t) {
            //在redis缓存结构中double类型只要小于18位便不会出现精度损失问题,但大于7位则会转为科学计数，因此需要转换
            return BigDecimal.valueOf(Double.valueOf(value));
        }
        return value;
    }

    /**
     * 获取k/hash/缓存值
     * @param t
     * @param key
     * @param <T>
     * @return
     */
    public <T> List<RepBean> getDataStreamCountMap(Class<T> t, final String key){
        Map<String, String> value = (Map<String, String>)redisTemplate.opsForHash().entries(key);
        List<RepBean> dataList = new ArrayList<>(value.size());
        if (Integer.TYPE == t){
            value.forEach((k,v)-> dataList.add(new RepBean(k, Integer.parseInt(v))));
        } else if(Double.TYPE == t) {
            //在redis缓存结构中double类型只要小于18位便不会出现精度损失问题,但大于7位则会转为科学计数，因此需要转换
            value.forEach((k,v)-> dataList.add(new RepBean(k, BigDecimal.valueOf(Double.valueOf(v)).doubleValue())));
        } else {
            //在redis缓存结构中double类型只要小于18位便不会出现精度损失问题,但大于7位则会转为科学计数，因此需要转换
            value.forEach((k,v)-> dataList.add(new RepBean(k, v)));
        }
        return  dataList;
    }
}
