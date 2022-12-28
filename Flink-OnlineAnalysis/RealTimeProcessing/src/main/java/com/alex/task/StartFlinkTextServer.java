package com.alex.task;

import com.alex.service.FlinkCountService;
import com.alex.service.QuotaEnum;
import com.alex.sink.RedisDataRichSink;
import com.alex.source.TextDataRichSource;
import com.alex.bean.Order;
import com.google.gson.Gson;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.flink.api.common.functions.FilterFunction;
import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.tuple.Tuple3;
import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.windowing.ProcessWindowFunction;
import org.apache.flink.streaming.api.windowing.windows.GlobalWindow;
import org.apache.flink.streaming.connectors.redis.common.mapper.RedisCommand;
import org.apache.flink.util.Collector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.*;
/**
 * @author Alex_liu
 * @create 2022-12-27 10:19
 * @Description Flink数据流实时计算示例：启动对接kafka数据流处理flink业务服务job,模拟对用户消费订单信息多维度聚合统计;
 */
public class StartFlinkTextServer {
    static Logger logger = LoggerFactory.getLogger(StartFlinkTextServer.class);
    /**
     * 窗口事件时间
     */
    static final int EVENT_COUNT = 100;

    /**
     * 主进程方法
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) {
        System.out.println("out:开始启动StartFlinkTextServer服务");
        logger.info("开始启动StartFlinkTextServer服务");
        //无界数据流
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        //设置并行度
        env.setParallelism(4);
        //每隔5000ms进行启动一个检查点
        env.enableCheckpointing(5000);
        //设置模式为exactly-once
        env.getCheckpointConfig().setCheckpointingMode(CheckpointingMode.EXACTLY_ONCE);
        // 确保检查点之间有进行500 ms的进度
        env.getCheckpointConfig().setMinPauseBetweenCheckpoints(500);
        //读取数据源
        DataStream<Order> source = env.addSource(new TextDataRichSource());
        //统计订单总量和总额、最新订单窗口总量
        countOrderNumAndAmount(source);
        //按性别统计订单总量和总额
        countGenderShoppingNumAndAmount(source);
        //按商品类型统计订单总量和总额
        countGoodsTypeNumAndAmount(source);
        //按品牌统计订单总量和总额
        countBrandNumAndAmount(source);
        //统计用户消费总额
        countUserAmount(source);
        //按性别统计每10分钟消费订单总量
        countGenderTimeToNum(source);
        System.out.println("out:执行job任务");
        logger.info("执行job任务");
        //执行JOB
        try {
            env.execute("聚合统计JOB");
        }catch(Exception e){
            logger.error("聚合统计JOB,执行异常！", e);
        }
    }

    /**
     * 统计销量和销售额、最新订单窗口总量
     * @param orderDataStream
     */
    private static void countOrderNumAndAmount(DataStream<Order> orderDataStream){
        DataStream<Tuple3<String, Integer, BigDecimal>> output =
                FlinkCountService.commonCount("countOrderNumAndAmount", QuotaEnum.DEFAULT, orderDataStream, RedisCommand.SET,"FLINK_ORDER_TOTAL_NUM", "FLINK_ORDER_TOTAL_PRICE", true);
        //保存每次窗口统计总销量结果到redis中，注意此数据只取窗口最新值，将会复盖db存储中的值
        output.map(new MapFunction<Tuple3<String, Integer, BigDecimal>, Tuple2<String,String>>() {
            @Override
            public Tuple2<String,String> map(Tuple3<String, Integer, BigDecimal> t3) throws Exception {
                return Tuple2.of(null, System.currentTimeMillis() + ":" + t3.f1);
            }
        }).addSink(new RedisDataRichSink("FLINK_ORDER_TIME_NUM", RedisCommand.SET, false));
    }

    /**
     * 按性别统计每10分钟下单总量,并计录下单时间
     * @param orderDataStream
     */
    private static void countGenderTimeToNum(DataStream<Order> orderDataStream){
        DataStream<Map<String, Map<String,Integer>>> output = orderDataStream
                .map(new MapFunction<Order, Tuple3<String, Integer, String>>() {
                    @Override
                    public Tuple3<String, Integer, String> map(Order order) throws Exception {
                        return Tuple3.of(order.getGender(), order.getNum(), order.getOrderTime());
                    }
                })
                .returns(Types.TUPLE(Types.STRING, Types.INT, Types.STRING))
                .keyBy((KeySelector<Tuple3<String, Integer, String>, String>) k ->k.f0)
                //因测试是模拟一天的全量数据不停的输出，未按实际业务发生时间流入到数据流队列中，所以无法用timeWindows时间滑动或时间滚动窗口来实现，只有通过数量滚动窗口批量处理数据；
                //数量窗口限定每100个订单，触发一次计算
                .countWindow(EVENT_COUNT)
                //处理窗口下的所有数据
                .process(new ProcessWindowFunction<Tuple3<String,Integer,String>, Map<String, Map<String,Integer>>, String, GlobalWindow>() {
                    /**
                     * 按每10分钟时间拆分统计，计算一次性别下各自订单总量
                     * @param k         keyBy分区字段
                     * @param context   上下文对象
                     * @param input     窗口输入数据集合
                     * @param out       输出的数据集合
                     * @throws Exception
                     */
                    @Override
                    public void process(String k, Context context, Iterable<Tuple3<String, Integer, String>> input, Collector<Map<String, Map<String,Integer>>> out) throws Exception {
                        Iterator<Tuple3<String, Integer, String>> iterator = input.iterator();
                        Map<String, Map<String,Integer>> map= new HashMap<>();
                        while (iterator.hasNext()){
                            Tuple3<String, Integer, String> tuple3 = iterator.next();
                            Integer num = tuple3.f1;
                            String orderTimeStr = tuple3.f2;
                            Date orderTime = DateUtils.parseDate(orderTimeStr, "yyyy-MM-dd HH:mm:ss");
                            long h = DateUtils.getFragmentInHours(orderTime, Calendar.DAY_OF_YEAR);
                            long m = DateUtils.getFragmentInMinutes(orderTime, Calendar.HOUR_OF_DAY);
                            //key = 1.2h
                            String key = h+"."+(m>9?(m/10+1):1) + "h";
                            Map<String,Integer> genderMap = map.get(key) ;
                            if (genderMap == null){
                                genderMap = new HashMap<>();
                                genderMap.put(k, num);
                            }else {
                                Integer val = genderMap.get(k);
                                val = (val == null)? new Integer(0) : val;
                                genderMap.put(k, val.intValue() + num.intValue());
                            }
                            //key = 1.2h, value = {男：11，女：22}
                            map.put(key, genderMap);
                        }
                        out.collect(map);
                    }
                })
                .name("countGenderTimeToNum");
        output.print();
        //数据结构：key,1h,{男：11，女：22}
        //保存到redis中
        output.flatMap(new FlatMapFunction<Map<String,Map<String,Integer>>, Tuple2<String,String>>() {
            @Override
            public void flatMap(Map<String, Map<String, Integer>> input, Collector<Tuple2<String, String>> out) throws Exception {
                Gson gson = new Gson();
                input.forEach((k,v) -> out.collect(Tuple2.of(k, gson.toJson(v))));
            }
        }).addSink(new RedisDataRichSink("FLINK_ORDER_GENDER_TIME_NUM", RedisCommand.HSET,  true));
    }

    /**
     * 按性别统计总量和累计额
     * @param orderDataStream
     */
    private static void countGenderShoppingNumAndAmount(DataStream<Order> orderDataStream){
        FlinkCountService.commonCount("countGenderShoppingNumAndAmount", QuotaEnum.GENDER, orderDataStream, RedisCommand.HSET,"FLINK_ORDER_GENDER_TOTAL_NUM", "FLINK_ORDER_GENDER_TOTAL_PRICE", true);
    }

    /**
     * 按商品分类统计总量和累计额
     * @param orderDataStream
     */
    private static void countGoodsTypeNumAndAmount(DataStream<Order> orderDataStream){
        FlinkCountService.commonCount("countGoodsTypeNumAndAmount", QuotaEnum.GOODS_TYPE, orderDataStream, RedisCommand.HSET,"FLINK_ORDER_GOODS_TYPE_TOTAL_NUM", "FLINK_ORDER_GOODS_TYPE_TOTAL_PRICE", true);
    }

    /**
     * 按品牌统计总量和累计额
     * @param orderDataStream
     */
    private static void countBrandNumAndAmount(DataStream<Order> orderDataStream){
        FlinkCountService.commonCount("countBrandNumAndAmount", QuotaEnum.BRAND, orderDataStream, RedisCommand.HSET, "FLINK_ORDER_BRAND_TOTAL_NUM", "FLINK_ORDER_BRAND_TOTAL_PRICE", true);
    }

    /**
     * 统计用户消费累计额
     * @param orderDataStream
     */
    private static void countUserAmount(DataStream<Order> orderDataStream){
        //单次消费低于3000的不入库（有可能会存在，用户持续消费，但每次小于1000，实际累计额较大的情况，可根据需要调整）
        orderDataStream = orderDataStream.filter((FilterFunction<Order>) value -> value.getTotalPrice() > 3000.0);
        FlinkCountService.commonCount("countUserAmount", QuotaEnum.USER, orderDataStream, RedisCommand.HSET, null, "FLINK_ORDER_USER_RANKING", true);
    }

}
