package com.alex.task;

import com.alex.service.FlinkCountService;
import com.alex.service.QuotaEnum;
import com.alex.sink.RedisDataRichSink;
import com.alex.source.KafkaSourceFunction;
import com.alex.bean.Order;
import com.google.gson.Gson;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.FilterFunction;
import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.tuple.Tuple3;
import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.windowing.ProcessWindowFunction;
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.streaming.connectors.redis.common.mapper.RedisCommand;
import org.apache.flink.util.Collector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.*;

/**
 * @author Alex_liu
 * @create 2022-12-27 10:19
 * @Description Flink数据流实时计算示例：启动对接kafka数据流处理flink业务服务job,模拟对用户消费订单信息多维度聚合统计;
 */
public class StartFlinkKafkaServer {
    static Logger logger = LoggerFactory.getLogger(StartFlinkKafkaServer.class);
    /**
     * 窗口事件时间
     */
    static final int EVENT_TIME = 5;

    /**
     * 主进程方法
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) {
        System.out.println("out:开始启动StartFlinkKafkaServer服务");
        logger.info("开始启动StartFlinkKafkaServer服务");
        //无界数据流
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        //设置并行度
        env.setParallelism(2);
        //每隔5000ms进行启动一个检查点
        env.enableCheckpointing(5000);
        //设置模式为exactly-once
        env.getCheckpointConfig().setCheckpointingMode(CheckpointingMode.EXACTLY_ONCE);
        // 确保检查点之间有进行500 ms的进度
        env.getCheckpointConfig().setMinPauseBetweenCheckpoints(500);
        //注意此处，必需设为TimeCharacteristic.EventTime，表示采用数据流元素事件时间（可以是元素时间字段、也可以自定义系统时间）
        env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime);
        //读取数据源
        KafkaSourceFunction kafkaSourceFunction = new KafkaSourceFunction();
        DataStream<Order> source = kafkaSourceFunction.run(env);
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
        output.print("销量和销售额：");
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

    /**
     * 按性别统计每5分钟下单总量,并计录下单时间(单独流处理)
     * @param orderDataStream
     */
    private static void countGenderTimeToNum(DataStream<Order> orderDataStream){
        DataStream<Map<String, Map<String,Integer>>> output = orderDataStream
                .assignTimestampsAndWatermarks(
                        WatermarkStrategy.<Order>forBoundedOutOfOrderness(Duration.ofSeconds(5))
                                .withTimestampAssigner((element, timestamp) -> {
                                    return element.getOrderTimeSeries();
                                })
                )
                .map(new MapFunction<Order, Tuple3<String, Integer, Long>>() {
                    @Override
                    public Tuple3<String, Integer, Long> map(Order order) throws Exception {
                        return Tuple3.of(order.getGender(), order.getNum(), order.getOrderTimeSeries());
                    }
                })
                .returns(Types.TUPLE(Types.STRING, Types.INT, Types.LONG))
                .keyBy((KeySelector<Tuple3<String, Integer, Long>, String>) k ->k.f0)
                //按5分钟为一个滚动窗口
                .window(TumblingEventTimeWindows.of(Time.minutes(EVENT_TIME)))
                //处理窗口下的所有数据
                .process(new ProcessWindowFunction<Tuple3<String,Integer,Long>, Map<String, Map<String,Integer>>, String, TimeWindow>() {
                    /**
                     * 按每10分钟时间分区统计，计算一次性别下各自订单总量
                     * @param k         keyBy分区字段
                     * @param context   上下文对象
                     * @param input     窗口输入数据集合
                     * @param out       输出的数据集合
                     * @throws Exception
                     */
                    @Override
                    public void process(String k, Context context, Iterable<Tuple3<String, Integer, Long>> input, Collector<Map<String, Map<String,Integer>>> out) throws Exception {
                        long start = context.window().getStart();
                        long end = context.window().getEnd();
                        System.err.println("计算窗口时间周期，startTime:" + DateFormatUtils.format(start, "yyyy-MM-dd HH:mm:ss") + ", endTime:" + DateFormatUtils.format(end, "yyyy-MM-dd HH:mm:ss"));

                        Iterator<Tuple3<String, Integer, Long>> iterator = input.iterator();
                        Map<String, Map<String,Integer>> map= new HashMap<>();
                        Tuple3<String, Integer, Long> tuple3;
                        String key;
                        Integer val;
                        Integer num;
                        Date orderTime;
                        Map<String,Integer> genderMap;
                        while (iterator.hasNext()){
                            tuple3 = iterator.next();
                            val = null;
                            num = tuple3.f1;
                            orderTime = new Date();
                            long h = DateUtils.getFragmentInHours(orderTime, Calendar.DAY_OF_YEAR);
                            long m = DateUtils.getFragmentInMinutes(orderTime, Calendar.HOUR_OF_DAY);
                            //key = 1.2h
                            key = h +"."+(m>9 ? (m/10+1) : 1) + "h";
                            genderMap = map.get(key) ;
                            if (genderMap == null){
                                genderMap = new HashMap<>(2);
                            }else {
                                val = genderMap.get(k);
                            }
                            val = (val == null) ? 0 : val;
                            genderMap.put(k, val.intValue() + num.intValue());
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
                input.forEach((k,v) -> out.collect(Tuple2.of(k, new Gson().toJson(v))));
            }
        }).addSink(new RedisDataRichSink("FLINK_ORDER_GENDER_TIME_NUM", RedisCommand.HSET,  true));
    }

}
