package com.alex.mock;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.Future;

/**
 * @author Alex_liu
 * @create 2022-12-27 10:19
 * @Description kafka工具类，提供消息发送与监听
 */
public class KafkaUtils {

    /**
     * 获取实始化KafkaStreamServer对象
     * @return
     */
    public static KafkaStreamServer buildServer(){
        return new KafkaStreamServer();
    }

    /**
     * 获取实始化KafkaStreamClient对象
     * @return
     */
    public static KafkaStreamClient buildClient(){
        return new KafkaStreamClient();
    }

    public static class KafkaStreamServer{
        KafkaProducer<String, String> kafkaProducer = null;

        private KafkaStreamServer(){}

        /**
         * 创建配置属性
         * @param host
         * @param port
         * @return
         */
        public KafkaStreamServer createKafkaStreamServer(String host, int port){
            String bootstrapServers = String.format("%s:%d", host, port);
            if (kafkaProducer != null){
                return this;
            }
            Properties properties = new Properties();
            //kafka地址，多个地址用逗号分割
            properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
            properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
            properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
            properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
            kafkaProducer = new KafkaProducer<>(properties);
            return this;
        }

        /**
         * 向kafka服务发送生产者消息
         * @param topic
         * @param msg
         * @return
         */
        public Future<RecordMetadata> sendMsg(String topic, String msg){
            ProducerRecord<String, String> record = new ProducerRecord<String, String>(topic, msg);
            Future<RecordMetadata> future = kafkaProducer.send(record);
            System.out.println("消息发送成功:" + msg + ", isDone:" + future.isDone());
            return future;
        }

        /**
         * 关闭kafka连接
         */
        public void close(){
            if (kafkaProducer != null){
                kafkaProducer.flush();
                kafkaProducer.close();
                kafkaProducer = null;
            }
        }
    }

    public static class KafkaStreamClient {
        KafkaConsumer<String, String> kafkaConsumer = null;
        private KafkaStreamClient(){}

        /**
         * 配置属性,创建消费者
         * @param host
         * @param port
         * @return
         */
        public KafkaStreamClient createKafkaStreamClient(String host, int port, String groupId){
            String bootstrapServers = String.format("%s:%d", host, port);
            if (kafkaConsumer != null){
                return this;
            }
            Properties properties = new Properties();
            //kafka地址，多个地址用逗号分割
            properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,  bootstrapServers);
            properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
            properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
            properties.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
            kafkaConsumer = new KafkaConsumer<String, String>(properties);
            return this;
        }

        /**
         * 客户端消费者拉取消息，并通过回调HeaderInterface实现类传递消息
         * @param topic
         * @param headerInterface
         */
        public void pollMsg(String topic, HeaderInterface headerInterface) {
            kafkaConsumer.subscribe(Collections.singletonList(topic));
            while (true) {
                ConsumerRecords<String, String> records = kafkaConsumer.poll(100);
                for (ConsumerRecord<String, String> record : records) {
                    try{
                        headerInterface.execute(record);
                    }catch(Exception e){
                        e.printStackTrace();
                    }
                }
            }
        }

        /**
         * 关闭kafka连接
         */
        public void close(){
            if (kafkaConsumer != null){
                kafkaConsumer.close();
                kafkaConsumer = null;
            }
        }
    }


    @FunctionalInterface
    interface HeaderInterface{
        void execute(ConsumerRecord<String, String> record);
    }

    /**
     * 测试示例
     * @param args
     * @throws InterruptedException
     */
    public static void main(String[] args) throws InterruptedException {
        //生产者发送消息
//        KafkaStreamServer kafkaStreamServer =  KafkaUtils.bulidServer().createKafkaStreamServer("192.168.110.35", 9092);
//        int i=0;
//        while (i<10) {
//            String msg = "Hello," + new Random().nextInt(100);
//            kafkaStreamServer.sendMsg("test", msg);
//            i++;
//            Thread.sleep(500);
//        }
//        kafkaStreamServer.close();
//        System.out.println("发送结束");

        System.out.println("接收消息");
        KafkaStreamClient kafkaStreamClient =  KafkaUtils.buildClient().createKafkaStreamClient("192.168.88.100", 9092, "consumer-45");
        kafkaStreamClient.pollMsg("mock_orderData", new HeaderInterface() {
            @Override
            public void execute(ConsumerRecord<String, String> record) {
                System.out.println(String.format("topic:%s,offset:%d,消息:%s", record.topic(), record.offset(), record.value()));
            }
        });

    }

}
