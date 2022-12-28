package com.alex.mock;

import com.alex.sink.RedisDataRichSink;
import com.alex.source.TextDataRichSource;
import com.alex.bean.Order;
import com.google.gson.Gson;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.common.functions.FoldFunction;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.tuple.Tuple3;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.windowing.ProcessWindowFunction;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.api.windowing.windows.GlobalWindow;
import org.apache.flink.streaming.connectors.redis.common.mapper.RedisCommand;
import org.apache.flink.util.Collector;

import java.math.BigDecimal;
import java.util.*;

/**
 * @author Alex_liu
 * @create 2022-12-27 10:19
 * @Description
 */
public class StartMain {


    public static void main(String[] args) throws Exception {
        //无界数据流
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(4);
        //读取数据源
        DataStream<Order> source = env.addSource(new TextDataRichSource());

//        countOrderNum(source);
//        countOrderAmount(source);

//        countGenderShoppingNum(source);
//        countGenderShoppingAmount(source);
//        countGenderTimeToNum(source);

//        countGoodsTypeNum(source);
//        countGoodsTypeAmount(source);

//        countBrandNum(source);
//        countBrandAmount(source);

//        countUserRanking(source);

        env.execute("聚合统计JOB");
    }

    //每5秒统计一次性别下单人次,并计录下单时间
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
                .countWindow(100)
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
                            long h = DateUtils.getFragmentInHours(orderTime, Calendar.HOUR_OF_DAY);
                            long m = DateUtils.getFragmentInHours(orderTime, Calendar.MINUTE);
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
                });
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

    //每5秒统计一次商品分类累计销量
    private static void countGoodsTypeNum(DataStream<Order> orderDataStream){
        DataStream<Tuple2<String, Integer>> output = orderDataStream
                .map(new MapFunction<Order, Tuple2<String, Integer>>() {
                    @Override
                    public Tuple2<String, Integer> map(Order order) throws Exception {
                        return Tuple2.of(order.getGoodsType(), order.getNum());
                    }
                })
                .returns(Types.TUPLE(Types.STRING, Types.INT))
                .keyBy((KeySelector<Tuple2<String, Integer>, String>) k ->k.f0)
                .timeWindow(Time.seconds(5))
                .sum(1);
        output.print();
        //保存到redis中
        output.map(new MapFunction<Tuple2<String,Integer>, Tuple2<String,String>>() {
            @Override
            public Tuple2<String,String> map(Tuple2<String, Integer> t2) throws Exception {
                return Tuple2.of(t2.f0,  t2.f1 + "");
            }
        }).addSink(new RedisDataRichSink("FLINK_ORDER_GOODS_TYPE_TOTAL_NUM", RedisCommand.HSET, true));
    }

    //每5秒统计一次商品分类累计额
    private static void countGoodsTypeAmount(DataStream<Order> orderDataStream){
        DataStream<Tuple2<String, Double>> output = orderDataStream
                .map(new MapFunction<Order, Tuple2<String, Double>>() {
                    @Override
                    public Tuple2<String, Double> map(Order order) throws Exception {
                        return Tuple2.of(order.getGoodsType(), order.getTotalPrice());
                    }
                })
                .returns(Types.TUPLE(Types.STRING, Types.DOUBLE))
                .keyBy((KeySelector<Tuple2<String, Double>, String>) k ->k.f0)
                .timeWindow(Time.seconds(5))
                .sum(1);
        output.print();
        //保存到redis中
        output.map(new MapFunction<Tuple2<String,Double>, Tuple2<String,String>>() {
            @Override
            public Tuple2<String,String> map(Tuple2<String, Double> t2) throws Exception {
                return Tuple2.of(t2.f0, t2.f1.toString());
            }
        }).addSink(new RedisDataRichSink("FLINK_ORDER_GOODS_TYPE_TOTAL_PRICE", RedisCommand.HSET, true));
    }

    //每5秒统计消费者排名
    private static void countUserRanking(DataStream<Order> orderDataStream){
        DataStream<Tuple2<String, Double>> output = orderDataStream
                .map(new MapFunction<Order, Tuple2<String, Double>>() {
                    @Override
                    public Tuple2<String, Double> map(Order order) throws Exception {
                        return Tuple2.of(order.getUserName(), order.getTotalPrice());
                    }
                })
                .returns(Types.TUPLE(Types.STRING, Types.DOUBLE))
                .keyBy((KeySelector<Tuple2<String, Double>, String>) k ->k.f0)
                .timeWindow(Time.seconds(5))
                .sum(1);
        output.print();
        //保存到redis中
        output.map(new MapFunction<Tuple2<String,Double>, Tuple2<String,String>>() {
            @Override
            public Tuple2<String,String> map(Tuple2<String, Double> t2) throws Exception {
                return Tuple2.of(t2.f0, t2.f1.toString());
            }
        }).addSink(new RedisDataRichSink("FLINK_ORDER_USER_RANKING", RedisCommand.HSET, true));
    }

    //每5秒统计品牌销量
    private static void countBrandNum(DataStream<Order> orderDataStream){
        DataStream<Tuple2<String, Integer>> output = orderDataStream
                .map(new MapFunction<Order, Tuple2<String, Integer>>() {
                    @Override
                    public Tuple2<String, Integer> map(Order order) throws Exception {
                        return Tuple2.of(order.getBrand(), order.getNum());
                    }
                })
                .returns(Types.TUPLE(Types.STRING, Types.INT))
                .keyBy((KeySelector<Tuple2<String, Integer>, String>) k ->k.f0)
                .timeWindow(Time.seconds(5))
                .sum(1);
        output.print();
        //保存到redis中
        output.map(new MapFunction<Tuple2<String,Integer>, Tuple2<String,String>>() {
            @Override
            public Tuple2<String,String> map(Tuple2<String, Integer> t2) throws Exception {
                return Tuple2.of(t2.f0,  t2.f1 + "");
            }
        }).addSink(new RedisDataRichSink("FLINK_ORDER_BRAND_TOTAL_NUM", RedisCommand.HSET, true));
    }

    //每5秒统计品牌累计额
    private static void countBrandAmount(DataStream<Order> orderDataStream){
        DataStream<Tuple2<String, Double>> output = orderDataStream
                .map(new MapFunction<Order, Tuple2<String, Double>>() {
                    @Override
                    public Tuple2<String, Double> map(Order order) throws Exception {
                        return Tuple2.of(order.getBrand(), order.getTotalPrice());
                    }
                })
                .returns(Types.TUPLE(Types.STRING, Types.DOUBLE))
                .keyBy((KeySelector<Tuple2<String, Double>, String>) k ->k.f0)
                .timeWindow(Time.seconds(5))
                .sum(1);
        output.print();
        //保存到redis中
        output.map(new MapFunction<Tuple2<String,Double>, Tuple2<String,String>>() {
            @Override
            public Tuple2<String,String> map(Tuple2<String, Double> t2) throws Exception {
                return Tuple2.of(t2.f0, t2.f1.toString());
            }
        }).addSink(new RedisDataRichSink("FLINK_ORDER_BRAND_TOTAL_PRICE", RedisCommand.HSET, true));
    }

    //每5秒统计一次性别累计销量
    private static void countGenderShoppingNum(DataStream<Order> orderDataStream){
        DataStream<Tuple2<String, Integer>> output = orderDataStream
                .map(new MapFunction<Order, Tuple2<String, Integer>>() {
                    @Override
                    public Tuple2<String, Integer> map(Order order) throws Exception {
                        return Tuple2.of(order.getGender(), order.getNum());
                    }
                })
                .returns(Types.TUPLE(Types.STRING, Types.INT))
                .keyBy((KeySelector<Tuple2<String, Integer>, String>) k ->k.f0)
                .timeWindow(Time.seconds(5))
                .sum(1);
        output.print();
        //保存到redis中
        output.map(new MapFunction<Tuple2<String, Integer>, Tuple2<String, String>>() {
            @Override
            public Tuple2<String, String> map(Tuple2<String, Integer> t2) throws Exception {
                return Tuple2.of(t2.f0, t2.f1 + "");
            }
        })
                .addSink(new RedisDataRichSink("FLINK_ORDER_GENDER_TOTAL_NUM", RedisCommand.HSET, true));
    }

    //每5秒统计一次性别累计额
    private static void countGenderShoppingAmount(DataStream<Order> orderDataStream){
        DataStream<Tuple2<String, Double>> output = orderDataStream
                .map(new MapFunction<Order, Tuple2<String, Double>>() {
                    @Override
                    public Tuple2<String, Double> map(Order order) throws Exception {
                        return Tuple2.of(order.getGender(), order.getTotalPrice());
                    }
                })
                .returns(Types.TUPLE(Types.STRING, Types.DOUBLE))
                .keyBy((KeySelector<Tuple2<String, Double>, String>) k ->k.f0)
                .timeWindow(Time.seconds(5))
                .sum(1);
        output.print();
        //保存到redis中
        output.map(new MapFunction<Tuple2<String,Double>, Tuple2<String,String>>() {
            @Override
            public Tuple2<String,String> map(Tuple2<String, Double> t2) throws Exception {
                return Tuple2.of(t2.f0,  t2.f1.toString());
            }
        }).addSink(new RedisDataRichSink("FLINK_ORDER_GENDER_TOTAL_PRICE", RedisCommand.HSET, true));
    }

    //每5秒统计一次销量
    private static void countOrderNum(DataStream<Order> orderDataStream){
        DataStream<Tuple2<String,Integer>> output = orderDataStream
                .map(new MapFunction<Order, Tuple2<String,Integer>>() {
                    @Override
                    public Tuple2<String,Integer> map(Order order) throws Exception {
                        return Tuple2.of("order", order.getNum());
                    }
                })
                .returns(Types.TUPLE(Types.STRING, Types.INT))
//                .keyBy((KeySelector<Tuple2<String,Integer>, String>) k ->k.f0)
                //注意这里窗口是统计所有
                .timeWindowAll(Time.seconds(5))
                .sum(1);
        output.print();
        //保存总销量与累计额到redis中
        output.map(new MapFunction<Tuple2<String,Integer>, Tuple2<String,String>>() {
            @Override
            public Tuple2<String,String> map(Tuple2<String,Integer> t2) throws Exception {
                return Tuple2.of(t2.f0, String.valueOf(t2.f1));
            }
        }).addSink(new RedisDataRichSink("FLINK_ORDER_TOTAL_NUM", RedisCommand.SET, true));

        //保存总销量与累计额到redis中
        output.map(new MapFunction<Tuple2<String,Integer>, Tuple2<String,String>>() {
            @Override
            public Tuple2<String,String> map(Tuple2<String,Integer> t2) throws Exception {
                return Tuple2.of(t2.f0, System.currentTimeMillis() + ":" + t2.f1);
            }
        }).addSink(new RedisDataRichSink("FLINK_ORDER_TIME_NUM", RedisCommand.SET, false));
    }

    //每5秒统计一次销售额
    private static void countOrderAmount(DataStream<Order> orderDataStream){
        DataStream<Tuple2<String,BigDecimal>> output = orderDataStream
                .map(new MapFunction<Order, Tuple2<String,BigDecimal>>() {
                    @Override
                    public Tuple2<String,BigDecimal> map(Order order) throws Exception {
                        return Tuple2.of("order", new BigDecimal(order.getTotalPrice()));
                    }
                })
                .returns(Types.TUPLE(Types.STRING, Types.BIG_DEC))
                .keyBy((KeySelector<Tuple2<String,BigDecimal>, String>) k ->k.f0)
                //注意这里窗口是统计所有
                .timeWindowAll(Time.seconds(5))
                .fold(Tuple2.of("order", new BigDecimal(0.0)), new FoldFunction<Tuple2<String,BigDecimal>, Tuple2<String,BigDecimal>>() {
                    @Override
                    public Tuple2<String,BigDecimal> fold(Tuple2<String,BigDecimal> input1, Tuple2<String,BigDecimal> input2) throws Exception {
                        return Tuple2.of(input1.f0, input1.f1.add(input2.f1).setScale(2, BigDecimal.ROUND_HALF_UP));
                    }
                });
        output.print();
        //保存总销量与累计额到redis中
        output.map(new MapFunction<Tuple2<String,BigDecimal>, Tuple2<String,String>>() {
            @Override
            public Tuple2<String,String> map(Tuple2<String, BigDecimal> t2) throws Exception {
                return Tuple2.of(t2.f0,  t2.f1.toPlainString());
            }
        }).addSink(new RedisDataRichSink("FLINK_ORDER_TOTAL_PRICE", RedisCommand.SET, true));
    }
}
