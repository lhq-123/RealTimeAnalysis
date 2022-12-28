package com.alex.sink;


import com.google.gson.Gson;
import org.apache.commons.lang3.StringUtils;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.sink.RichSinkFunction;
import org.apache.flink.streaming.connectors.redis.common.mapper.RedisCommand;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.Protocol;

import java.math.BigDecimal;
import java.util.Map;

/**
 * @author Alex_liu
 * @create 2022-12-27 10:19
 * @Description 将Flink的流中数据写入到Redis数据源中
 */
public class RedisDataRichSink extends RichSinkFunction<Tuple2<String,String>>{
    private JedisPool jedisPool;
    private Jedis jedis;
    private String key;
    private RedisCommand command;
    private boolean append;

    public final static String REDIS_HOST="192.168.88.100";
    public final static String REDIS_PASSWORD="";
    public final static int REDIS_PORT=6379;
    public final static String SPLIT_1 = ".";
    public final static String SPLIT_2 = ":";

    public RedisDataRichSink(){}

    /**
     *
     * @param key         redis键
     * @param command     set|hash缓存
     * @param append      append=true表示追加更新
     */
    public RedisDataRichSink(String key, RedisCommand command, boolean append){
        this.key = key;
        this.command = command;
        this.append = append;
    }
    /**
     * job开始执行，调用此方法创建jdbc数据源连接对象
     * @param parameters
     * @throws Exception
     */
    @Override
    public void open(Configuration parameters) throws Exception {
        super.open(parameters);
        //线程池使用内置默认配置
        JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
        //设置20个最大线程池
        jedisPoolConfig.setMaxTotal(20);
        if (REDIS_PASSWORD != null && REDIS_PASSWORD.length() > 0){
            jedisPool = new JedisPool(jedisPoolConfig, REDIS_HOST, REDIS_PORT, Protocol.DEFAULT_TIMEOUT, REDIS_PASSWORD );
        } else {
            jedisPool = new JedisPool(jedisPoolConfig, REDIS_HOST, REDIS_PORT, Protocol.DEFAULT_TIMEOUT);
        }
    }

    /**
     * 此方法实现接口中的invoke，在DataStream数据流中的每一条记录均会调用本方法执行数据同步
     * @param tuple2
     * @param context
     * @throws Exception
     */
    @Override
    public void invoke(Tuple2<String,String> tuple2, Context context) throws Exception {
        jedis = jedisPool.getResource();
        String dbValue ;
        try {
            //判断是k/hash操作，还是普通k/v操作
            if (command == RedisCommand.HSET){
                dbValue = append ? this.getHashDbValue(tuple2.f0, tuple2.f1) : sumValue(null, tuple2.f1);
                jedis.hset(key, tuple2.f0, dbValue);
            } else {
                String dbKey = StringUtils.isNotBlank(key) ? key : tuple2.f0;
                String bufferValue = append ? jedis.get(dbKey) : null;
                dbValue = sumValue(bufferValue, tuple2.f1);
                jedis.set(dbKey, dbValue);
            }
//            System.out.println("合计：" + dbValue);
        }catch (Exception e){
            e.printStackTrace();
        }
        if (jedis != null){
            //用完即关，内部会做判断，如果存在数据源与池，则回滚到池中
            jedis.close();
        }
    }

    /**
     * 对Hash值进行格式识别，并做特殊处理，如果为map对象，则需要额外的比较与拆分后再累计
     * @param f0
     * @param f1
     * @return
     */
    private String getHashDbValue(String f0, String f1){
        String bufferValue = jedis.hget(key, f0);
        if (bufferValue == null){
            return sumValue(null, f1);
        }
        //如果带:号，则表示当前数据是Map对象类型json
        if (f1.indexOf(SPLIT_2) == -1){
            return sumValue(bufferValue, f1);
        }
        Gson gson = new Gson();
        Map<String,String> map1 =  gson.fromJson(bufferValue, Map.class);
        Map<String,String> map2 =  gson.fromJson(f1, Map.class);
        for (Map.Entry<String, String> entry : map1.entrySet()) {
            String key = entry.getKey();
            if (map2.containsKey(key)) {
                //已缓存数据
                String val1 = String.valueOf(entry.getValue());
                //增量数据
                String val2 = String.valueOf(map2.get(key));
                map1.put(key, sumValue(val1, val2));
                //已累加到map1,清理相同key的map2值
                map2.remove(key);
            }
        }
        if (map2 != null && map2.size() > 0) {
            map1.putAll(map2);
        }
        return gson.toJson(map1);
    }

    /**
     * 对两个值进行求和
     * @param value1
     * @param value2
     * @return
     */
    private String sumValue(String value1, String value2){
        if (StringUtils.isBlank(value1)){
            int lastIndex = value2.lastIndexOf(SPLIT_1);
            if (lastIndex != -1  && value2.length() - lastIndex + 1 > 2) {
                return new BigDecimal(value2).setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString();
            }
            return value2;
        }
        if (value1.indexOf(SPLIT_1)!=-1){
            //在redis缓存结构中double类型只要小于18位便不会出现精度损失问题,但大于7位则会转为科学计数，因此读取时需要转换
            BigDecimal b1 = new BigDecimal(value1);
            BigDecimal b2 = new BigDecimal(value2);
            return b1.add(b2).setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString();
        }else {
            long n1 = Long.parseLong(value1);
            long n2 = Long.parseLong(value2);
            return String.valueOf(n1 + n2);
        }
    }

    /**
     * job执行完毕后，调用此方法关闭jdbc连接源
     * @throws Exception
     */
    @Override
    public void close() throws Exception {
        super.close();
        if (jedisPool != null) {
            jedisPool.close();
        }
    }

}
