package com.alex.mock;

import com.alex.source.TextDataRichSource;
import com.alex.bean.Order;
import org.apache.flink.api.common.functions.FoldFunction;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.functions.ReduceFunction;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.api.java.ExecutionEnvironment;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.tuple.Tuple3;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.windowing.AllWindowFunction;
import org.apache.flink.streaming.api.windowing.assigners.TumblingProcessingTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.streaming.connectors.redis.RedisSink;
import org.apache.flink.streaming.connectors.redis.common.config.FlinkJedisPoolConfig;
import org.apache.flink.streaming.connectors.redis.common.mapper.RedisCommand;
import org.apache.flink.streaming.connectors.redis.common.mapper.RedisCommandDescription;
import org.apache.flink.streaming.connectors.redis.common.mapper.RedisMapper;
import org.apache.flink.table.api.bridge.java.BatchTableEnvironment;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;
import org.apache.flink.util.Collector;

import java.math.BigDecimal;

/**
 * @author Alex_liu
 * @create 2022-12-27 10:19
 * @Description
 */
public class AllStartMain {

    public static void main(String[] args) throws Exception {
        //无界数据流
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        StreamTableEnvironment sTableEnv = StreamTableEnvironment.create(env);
        env.setParallelism(1);

        ExecutionEnvironment dEnv = ExecutionEnvironment.getExecutionEnvironment();
        BatchTableEnvironment tEnv = BatchTableEnvironment.create(dEnv);
        //读取数据源
        DataStream<Order> source = env.addSource(new TextDataRichSource());
//        sTableEnv.fromValues(orderDataStream, $("userName"));
//        sTableEnv.createTemporaryView("Orders", source,
//                $("userName"),
//                $("gender"),
//                $("goods"),
//                $("goodsType"),
//                $("brand"),
//                $("orderTime"),
//                $("orderTimeSeries"),
//                $("price"),
//                $("num"),
//                $("totalPrice"),
//                $("status"),
//                $("address"),
//                $("phone")
//        );
//        Table orderTable = sTableEnv.sqlQuery("select userName,gender,goods,goodsType,brand,orderTime,orderTimeSeries,price,num,totalPrice,status,address,phone from Orders");
//        DataStream<Order> orderDataStream = sTableEnv.toAppendStream(orderTable, Order.class);

        //按默认全局窗口，获取订单数据流，在基于订单数据流的基础上进行算子统计
        DataStream<Order> orderDataStream = source.windowAll(TumblingProcessingTimeWindows.of(Time.seconds(5))).apply(new AllWindowFunction<Order, Order, TimeWindow>() {
            @Override
            public void apply(TimeWindow window, Iterable<Order> values, Collector<Order> out) throws Exception {
                for (Order order : values){
                    out.collect(order);
                }
            }
        });

        //加载redis配置与连接池
        FlinkJedisPoolConfig conf = new FlinkJedisPoolConfig.Builder().setHost("127.0.0.1").setPort(6379).build();

//        countOrderNum(orderDataStream, conf);
//        countGenderNum(orderDataStream, conf);

//        timeGenderShoppingNum(orderDataStream);
//        timeGoodsTypeNum(orderDataStream);
//
        countGenderShoppingAmount(orderDataStream);
//        timeGoodsTypeAmount(orderDataStream);

//        countUserRanking(source);

//        timeBrandNum(orderDataStream);
//        timeBrandAmount(orderDataStream);

        env.execute("执行统计job");
    }

    //每5秒统计一次销量和累计额
    private static void countOrderNum(DataStream<Order> orderDataStream,FlinkJedisPoolConfig conf){
        DataStream<Tuple2<Integer,Double>> output = orderDataStream
                .map(new MapFunction<Order, Tuple3<String,Integer,Double>>() {
                    @Override
                    public Tuple3<String,Integer,Double> map(Order order) throws Exception {
                        return Tuple3.of("order", order.getNum(), order.getTotalPrice());
                    }
                })
                .returns(Types.TUPLE(Types.STRING, Types.INT, Types.DOUBLE))
                .keyBy((KeySelector<Tuple3<String,Integer,Double>, String>) k ->k.f0)
                .fold(Tuple2.of(0,0.0) , new FoldFunction<Tuple3<String,Integer,Double>, Tuple2<Integer,Double>>() {
                    @Override
                    public Tuple2<Integer,Double> fold(Tuple2<Integer,Double> tuple2, Tuple3<String,Integer,Double> tuple3) throws Exception {
                        BigDecimal totalPrice = new BigDecimal(tuple2.f1 + tuple3.f2);
                        return Tuple2.of(tuple2.f0 + tuple3.f1, totalPrice.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
                    }
                });
        output.print();

        //保存总销量与累计额到redis中
        output.addSink(new RedisSink<>(conf, new RedisMapper<Tuple2<Integer, Double>>() {
            String key = "FLINK_TOTAL_NUM_AND_TOTAL_PRICE";
            @Override
            public RedisCommandDescription getCommandDescription() {
                return new RedisCommandDescription(RedisCommand.SET, key);
            }
            @Override
            public String getKeyFromData(Tuple2<Integer, Double> tuple2) {
                return key;
            }
            @Override
            public String getValueFromData(Tuple2<Integer, Double> tuple2) {
                return tuple2.f0.toString() + "," + tuple2.f1.toString();
            }
        }));
    }

    //每5秒统计一次性别下单人次
    private static void countGenderNum(DataStream<Order> orderDataStream, FlinkJedisPoolConfig conf){
        DataStream<Tuple2<String, Integer>> output = orderDataStream
                .map(new MapFunction<Order, Tuple2<String, Integer>>() {
                    @Override
                    public Tuple2<String, Integer> map(Order order) throws Exception {
                        return Tuple2.of(order.getGender(), 1);
                    }
                })
                .returns(Types.TUPLE(Types.STRING, Types.INT))
                .keyBy((KeySelector<Tuple2<String, Integer>, String>) k ->k.f0)
                .reduce(new ReduceFunction<Tuple2<String, Integer>>() {
                    @Override
                    public Tuple2<String, Integer> reduce(Tuple2<String, Integer> t0, Tuple2<String, Integer> t1) throws Exception {
                        int total = t0.f1 + t1.f1;
                        return new Tuple2<>(t0.f0, total);
                    }
                });
        output.print();

        //保存总销量与累计额到redis中
        output.addSink(new RedisSink<>(conf, new RedisMapper<Tuple2<String, Integer>>() {
            String key = "FLINK_GENDER_TOTAL_NUM";

            @Override
            public RedisCommandDescription getCommandDescription() {
                return new RedisCommandDescription(RedisCommand.HSET, key);
            }

            @Override
            public String getKeyFromData(Tuple2<String, Integer> tuple2) {
                return tuple2.f0;
            }

            @Override
            public String getValueFromData(Tuple2<String, Integer> tuple2) {
                return tuple2.f1.toString();
            }
        }));
    }

    //每5秒统计一次性别销量
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
                .sum(1);
        output.print();
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
                .sum(1);
        output.print();
    }

    //每5秒统计一次商品分类销量
    private static void timeGoodsTypeNum(DataStream<Order> orderDataStream){
        DataStream<Tuple2<String, Integer>> output = orderDataStream
                .map(new MapFunction<Order, Tuple2<String, Integer>>() {
                    @Override
                    public Tuple2<String, Integer> map(Order order) throws Exception {
                        return Tuple2.of(order.getGoodsType(), order.getNum());
                    }
                })
                .returns(Types.TUPLE(Types.STRING, Types.INT))
                .keyBy((KeySelector<Tuple2<String, Integer>, String>) k ->k.f0)
                .sum(1);
        output.print();
    }

    //每5秒统计一次商品分类累计额
    private static void timeGoodsTypeAmount(DataStream<Order> orderDataStream){
        DataStream<Tuple2<String, Double>> output = orderDataStream
                .map(new MapFunction<Order, Tuple2<String, Double>>() {
                    @Override
                    public Tuple2<String, Double> map(Order order) throws Exception {
                        return Tuple2.of(order.getGoodsType(), order.getTotalPrice());
                    }
                })
                .returns(Types.TUPLE(Types.STRING, Types.DOUBLE))
                .keyBy((KeySelector<Tuple2<String, Double>, String>) k ->k.f0)
                .sum(1);
        output.print();
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
                .sum(1);
        output.print();
    }

    //每5秒统计品牌销量
    private static void timeBrandNum(DataStream<Order> orderDataStream){
        DataStream<Tuple2<String, Integer>> output = orderDataStream
                .map(new MapFunction<Order, Tuple2<String, Integer>>() {
                    @Override
                    public Tuple2<String, Integer> map(Order order) throws Exception {
                        return Tuple2.of(order.getBrand(), order.getNum());
                    }
                })
                .returns(Types.TUPLE(Types.STRING, Types.INT))
                .keyBy((KeySelector<Tuple2<String, Integer>, String>) k ->k.f0)
                .sum(1);
        output.print();
    }

    //每5秒统计品牌累计额
    private static void timeBrandAmount(DataStream<Order> orderDataStream){
        DataStream<Tuple2<String, Double>> output = orderDataStream
                .map(new MapFunction<Order, Tuple2<String, Double>>() {
                    @Override
                    public Tuple2<String, Double> map(Order order) throws Exception {
                        return Tuple2.of(order.getBrand(), order.getTotalPrice());
                    }
                })
                .returns(Types.TUPLE(Types.STRING, Types.DOUBLE))
                .keyBy((KeySelector<Tuple2<String, Double>, String>) k ->k.f0)
                .sum(1);
        output.print();
    }

}
