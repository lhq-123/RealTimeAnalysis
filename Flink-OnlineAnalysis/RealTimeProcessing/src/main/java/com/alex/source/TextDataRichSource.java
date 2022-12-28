package com.alex.source;

import com.alex.bean.Order;
import com.google.gson.Gson;
import org.apache.commons.io.FileUtils;
import org.apache.flink.streaming.api.functions.source.RichSourceFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;

/**
 * @author Alex_liu
 * @create 2022-12-27 10:19
 * @Description 读取订单数据，模拟继续输出流
 */
public class TextDataRichSource extends RichSourceFunction<Order> {
    static Logger logger = LoggerFactory.getLogger(TextDataRichSource.class);
    String filePath = "D://test/order.txt";
    int size = 0;

    @Override
    public void run(SourceContext<Order> ctx) throws Exception {
        System.out.println("out:Flink开始加载text文本订单数据，fliePath="+ filePath);
        logger.info("Flink开始加载text文本订单数据，fliePath="+ filePath);
        List<String> orderList = FileUtils.readLines(new File(filePath), "UTF-8");
        Gson gson = new Gson();
        for (String str : orderList){
            System.out.println("out:订单：" + str);
            logger.info("订单：" + str);
            //System.out.println(str);
            Order order = gson.fromJson(str, Order.class);
            ctx.collect(order);
            size ++ ;
            //Thread.sleep(RandomUtils.nextInt(10, 200));
            Thread.sleep(10);
        }
    }

    @Override
    public void cancel() {
        System.out.println("out:Flink停止加载text文本订单数据，类计加载["+ size +  "]条.");
        logger.info("Flink停止加载text文本订单数据，类计加载["+ size +  "]条.");
    }
}
