package com.alex.source;

import com.alex.bean.Order;
import com.google.gson.Gson;
import org.apache.commons.lang3.StringUtils;
import org.apache.flink.api.common.functions.FilterFunction;
import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer;
import org.apache.flink.util.Collector;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;
/**
 * @author Alex_liu
 * @create 2022-12-27 10:19
 * @Description 从kafka中获取队列数据
 */
public class KafkaSourceFunction  {
    static Logger logger = LoggerFactory.getLogger(KafkaSourceFunction.class);
    static String DATA_TOPIC = "mock_orderData";
    private Properties initProperties(){
        //1.消费者客户端连接到kafka
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "192.168.88.100:9092");
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 5000);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "flink-consumer");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return props;
    }

    public DataStream<Order> run(StreamExecutionEnvironment env){
        return this.run(env, DATA_TOPIC);
    }

    public DataStream<Order> run(StreamExecutionEnvironment env, String topic){
        FlinkKafkaConsumer<String> consumer = new FlinkKafkaConsumer<>(topic, new SimpleStringSchema(), initProperties());

        //2.在算子中进行处理
        return env.addSource(consumer)
            .filter((FilterFunction<String>) value -> StringUtils.isNotBlank(value))
            .flatMap(new FlatMapFunction<String, Order>() {
                @Override
                public void flatMap(String value, Collector<Order> out) throws Exception {
                    Gson gson = new Gson();
                    logger.info("已消费订单：" + value);
                    //注意，因已开启enableCheckpointing容错定期检查状态机制，当算子出现错误时，会导致数据流恢复到最新 checkpoint 的状态，并从存储在 checkpoint 中的 offset 开始重新消费 Kafka 中的消息
                    //因此会有可能导制数据重复消费，重复错误，陷入死循环。加上try|catch，捕获错误后再正确输出
                    try {
                        Order order = gson.fromJson(value, Order.class);
                        out.collect(order);
                    }catch(Exception e){
                        e.printStackTrace();
                    }
                }
            })
            .returns(Order.class)
            .name("KafkaSource(" + topic +")");
    }
}
