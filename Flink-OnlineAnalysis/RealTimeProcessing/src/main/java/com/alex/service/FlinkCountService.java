package com.alex.service;

import com.alex.sink.RedisDataRichSink;
import com.alex.bean.Order;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.tuple.Tuple3;
import org.apache.flink.api.java.tuple.Tuple4;
import org.apache.flink.streaming.api.datastream.DataStream;
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
import java.util.Iterator;
/**
 * @author Alex_liu
 * @create 2022-12-27 10:19
 * @Description 公共算子
 */
public class FlinkCountService {
    static Logger logger = LoggerFactory.getLogger(FlinkCountService.class);
    /**
     * 公共聚合统计方法
     * @param name              设置当前数据流名称，用于可始化和日志查看
     * @param quotaEnum         统计维度
     * @param dataStream        数据源
     * @param command           Redis指令类型k/v|k/hash
     * @param key1              缓存key1
     * @param key2              缓存key2
     * @param append            是否追加数据（累计到之前的统计结果上）
     * @return
     */
    public static DataStream<Tuple3<String, Integer, BigDecimal>> commonCount(String name, QuotaEnum quotaEnum,
                                                                               DataStream<Order> dataStream,
                                                                               RedisCommand command,
                                                                               String key1,
                                                                               String key2,
                                                                               boolean append){
        DataStream<Tuple3<String, Integer, BigDecimal>> output = dataStream
                .map(new MapFunction<Order, Tuple4<String, Integer, BigDecimal,Long>>() {
                    @Override
                    public Tuple4<String, Integer, BigDecimal,Long> map(Order order) throws Exception {
                        String key = "default";
                        if (quotaEnum == QuotaEnum.BRAND){
                            key = order.getBrand();
                        }else if (quotaEnum == QuotaEnum.GOODS_TYPE){
                            key = order.getGoodsType();
                        }else if (quotaEnum == QuotaEnum.GENDER){
                            key = order.getGender();
                        }else if (quotaEnum == QuotaEnum.USER){
                            //演示以姓名为keyBy分区字段，实际会有重名因以业务唯一标识，如用户ID或账号等；
                            key = order.getUserName();
                        }
                        return Tuple4.of(key, order.getNum(), BigDecimal.valueOf(order.getTotalPrice()), order.getOrderTimeSeries());
                    }
                })
                //为一个水位线，这个Watermarks在不断的变化，一旦Watermarks大于了某个window的end_time，就会触发此window的计算，Watermarks就是用来触发window计算的。
                //Duration.ofSeconds(3)，到数据流到达flink后，再水位线中设置延迟时间，也就是在所有数据流的最大的事件时间比window窗口结束时间大或相等时，再延迟多久触发window窗口结束；
                .assignTimestampsAndWatermarks(
                        WatermarkStrategy.<Tuple4<String, Integer, BigDecimal, Long>>forBoundedOutOfOrderness(Duration.ofSeconds(3))
                                .withTimestampAssigner((element, timestamp) -> {
                                    //System.out.println(element.f1 + ","+ element.f0 + "的水位线为：" + DateFormatUtils.format(element.f3, "yyyy-MM-dd HH:mm:ss"));
                                    return element.f3;
                                })
                )
                .keyBy((KeySelector<Tuple4<String, Integer, BigDecimal,Long>, String>) k ->k.f0)
                //按1分钟为一个流窗口，但窗口从第0秒钟开始
                .window(TumblingEventTimeWindows.of(Time.minutes(1), Time.seconds(0)))
                //处理窗口事件下的所有流元素
                .process(new ProcessWindowFunction<Tuple4<String, Integer, BigDecimal,Long>, Tuple3<String, Integer, BigDecimal>, String, TimeWindow>() {
                    @Override
                    public void process(String s, Context context, Iterable<Tuple4<String, Integer, BigDecimal,Long>> elements, Collector<Tuple3<String, Integer, BigDecimal>> out) throws Exception {
                        long start = context.window().getStart();
                        long end = context.window().getEnd();
                        logger.info("计算窗口时间周期，startTime:" + DateFormatUtils.format(start, "yyyy-MM-dd HH:mm:ss") + ", endTime:" + DateFormatUtils.format(end, "yyyy-MM-dd HH:mm:ss"));
                        Iterator<Tuple4<String, Integer, BigDecimal,Long>> iterator = elements.iterator();
                        int totalNum = 0;
                        BigDecimal totalPrice = BigDecimal.valueOf(0.00);
                        while (iterator.hasNext()){
                            Tuple4<String, Integer, BigDecimal,Long> t4 = iterator.next();
                            totalNum += t4.f1;
                            totalPrice = totalPrice.add(t4.f2).setScale(2 , BigDecimal.ROUND_HALF_UP);
                        }
                        out.collect(Tuple3.of(s, totalNum, totalPrice));
                    }
                })
                .returns(Types.TUPLE(Types.STRING, Types.INT, Types.BIG_DEC))
                .name(name);
        output.print();

        //保存总销量到redis中
        if (key1 != null) {
            output.map(new MapFunction<Tuple3<String, Integer, BigDecimal>, Tuple2<String, String>>() {
                @Override
                public Tuple2<String, String> map(Tuple3<String, Integer, BigDecimal> t3) throws Exception {
                    return Tuple2.of(t3.f0, t3.f1.toString());
                }
            }).addSink(new RedisDataRichSink(key1, command, append));
        }

        //保存累计额到redis中
        if (key2 != null){
            output.map(new MapFunction<Tuple3<String, Integer, BigDecimal>, Tuple2<String,String>>() {
                @Override
                public Tuple2<String,String> map(Tuple3<String, Integer, BigDecimal> t3) throws Exception {
                    return Tuple2.of(t3.f0, t3.f2.toPlainString());
                }
            }).addSink(new RedisDataRichSink(key2, command, append));
        }
        return output;
    }

}
