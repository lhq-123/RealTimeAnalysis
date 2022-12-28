package com.alex.mock;

import com.alex.bean.Order;
import com.google.gson.Gson;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author Alex_liu
 * @create 2022-12-27 10:19
 * @Description 向kafka发送测试模拟订单数据
 */
public class CreateKafkaMsg {

    static String upperNames = "赵,钱,孙,李,周,吴,郑,王,冯,陈,褚,卫,蒋,沈,韩,杨,朱,秦,尤,许,何,吕,施,张,孔,曹,严,华,金," +
            "魏,陶,姜,戚,谢,邹,喻,柏,水,窦,章,云,苏,潘,葛,奚,范,彭,郎,鲁,韦,昌,马,苗,凤,花,方,俞,任,袁,柳,酆,鲍,史,唐," +
            "费,廉,岑,薛,雷,贺,倪,汤,滕,殷,罗,毕,郝,邬,安,常,乐,于,时,傅,皮,卞,齐,康,伍,余,元,卜,顾,孟,平,黄,和,穆,萧," +
            "尹,姚,邵,湛,汪,祁,毛,禹,狄,米,贝,明,臧,计,伏,成,戴,谈,宋,茅,庞,熊,纪,舒,屈,项,祝,董,梁,杜,阮,蓝,闵,席,季," +
            "麻,强,贾,路,娄,危,江,童,颜,郭,梅,盛,林,刁,钟,徐,邱,骆,高,夏,蔡,田,樊,胡,凌,霍,虞,万,支,柯,昝,管,卢,莫,经," +
            "房,裘,缪,干,解,应,宗,丁,宣,贲,邓,郁,单,杭,洪,包,诸,左,石,崔,吉,钮,龚,程,嵇,邢,滑,裴,陆,荣,翁,荀,羊,於,惠," +
            "甄,曲,家,封,芮,羿,储,靳,汲,邴,糜,松,井,段,富,巫,乌,焦,巴,弓,牧,隗,山,谷,车,侯,宓,蓬,全,郗,班,仰,秋,仲,伊," +
            "宫,宁,仇,栾,暴,甘,钭,厉,戎,祖,武,符,刘,景,詹,束,龙,叶,幸,司,韶,郜,黎,蓟,薄,印,宿,白,怀,蒲,邰,从,鄂,索,咸," +
            "籍,赖,卓,蔺,屠,蒙,池,乔,阴,胥,能,苍,双,闻,莘,党,翟,谭,贡,劳,逄,姬,申,扶,堵,冉,宰,郦,雍,郤,璩,桑,桂,濮,牛," +
            "寿,通,边,扈,燕,冀,郏,浦,尚,农,温,别,庄,晏,柴,瞿,阎,充,慕,连,茹,习,宦,艾,鱼,容,向,古,易,慎,戈,廖,庾,终,暨," +
            "居,衡,步,都,耿,满,弘,匡,国,文,寇,广,禄,阙,东,欧,殳,沃,利,蔚,越,夔,隆,师,巩,厍,聂,晁,勾,敖,融,冷,訾,辛,阚," +
            "那,简,饶,空,曾,毋,沙,乜,养,鞠,须,丰,巢,关,蒯,相,查,後,荆,红,游,竺,权,逯,盖,益,桓,公";
    static String upperNums = "壹,贰,叁,肆,伍,陆,柒,捌,玖,拾,佰,仟,万,亿,元,角,分,零";
    static String [] genders = new String[]{"男", "女"};
    static String [] goodsTypes = new String[]{"数码", "美食", "时尚", "家居", "运动", "母婴", "大杂烩", "包包"};
    static Map<String,String> brandMap = new HashMap<String,String>(){
        {
            put("数码","苹果,华为,小米,三星,OPPO");
            put("美食","三只松鼠,百草园,周黑鸭");
            put("时尚","韩衣都舍,南极人,冠军");
            put("家居","林氏木业,典美家居,源氏木源");
            put("运动","乔丹,361度,李宁");
            put("母婴","贝佳美,美素佳儿,蓝甄");
            put("大杂烩","洁柔,云南白药,手巾");
            put("包包","LV,老人头,高尔夫,金狐狸,稻草人");
        }
    };
    static String [] statuss = new String [] {"待支付","已支付","配送中","已完成"};
    static String [] addresss = new String [] {"河北省","山西省","辽宁省","吉林省","黑龙江省","江苏省","浙江省","安徽省",
            "福建省","江西省","山东省","河南省","湖北省","湖南省","广东省","海南省","四川省","贵州省","云南省","陕西省",
            "甘肃省","青海省","台湾省","北京市","天津市","上海市","重庆市","广西壮族自治区","内蒙古自治区","西藏自治区",
            "宁夏回族自治区","新疆维吾尔自治区"};

    /**
     * 订单信息：
     * 订单ID、用户名称、用户性别、商品名称、商品类型、生产商、下单时间、单价、数量、总价、订单状态、 收货地址、联系方式
     * @param args
     */
    public static void main(String[] args) throws Exception {
        String [] userNames = StringUtils.split(upperNames, ",");
        String [] nums = StringUtils.split(upperNums, ",");
        Map<String, String> genderMap = new HashMap<>();
        Map<String, String> addressMap = new HashMap<>();

        //生产者发送消息
        KafkaUtils.KafkaStreamServer kafkaStreamServer =  KafkaUtils.buildServer().createKafkaStreamServer("192.168.88.100", 9092);
        String topic = "mock_orderData";

        //模拟不停创建模拟订单
        int i=0;
        while(true){
            String orderId = DateFormatUtils.format(System.currentTimeMillis(), "yyyyMMddHHmmssSSS") + RandomUtils.nextInt(1000 , 9999);
            String userName  = userNames[RandomUtils.nextInt(0, userNames.length)] + nums[RandomUtils.nextInt(0, nums.length)];
            String gender = genderMap.get(userName);
            if (gender == null){
                gender = genders[i%2];
                genderMap.put(userName, gender);
            }

            String address = addressMap.get(userName);
            if (address == null){
                address = addresss[RandomUtils.nextInt(0, addresss.length)];
                addressMap.put(userName, address);
            }

            String goodsType = goodsTypes[RandomUtils.nextInt(0, goodsTypes.length)];
            String goods = goodsType + "商品"+ i;
            String [] brands = brandMap.get(goodsType).split(",");
            String brand = brands[RandomUtils.nextInt(0, brands.length)];

            Double price ;
            Integer num ;
            if (goodsType.equals("数码")){
                price = RandomUtils.nextDouble(400.00, 12000.00);
                num = RandomUtils.nextInt(1, 2);
            }else if (goodsType.equals("家居")){
                price = RandomUtils.nextDouble(300.00, 7000.00);
                num = RandomUtils.nextInt(1, 2);
            }else if (goodsType.equals("运动")){
                price = RandomUtils.nextDouble(200.00, 4000.00);
                num = RandomUtils.nextInt(1, 3);
            }else if (goodsType.equals("包包")){
                price = RandomUtils.nextDouble(100.00, 3000.00);
                num = RandomUtils.nextInt(1, 3);
            }else if (goodsType.equals("母婴")){
                price = RandomUtils.nextDouble(50.00, 2000.00);
                num = RandomUtils.nextInt(1, 4);
            }else {
                price = RandomUtils.nextDouble(20.00, 1000.00);
                num = RandomUtils.nextInt(1, 10);
            }
            BigDecimal priceBig = new BigDecimal(price);
            price = priceBig.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
            //创建总价
            BigDecimal totalPriceBig = new BigDecimal(price * num);
            Double totalPrice = totalPriceBig.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
            //订单生成时间
            Long orderTimeSeries = System.currentTimeMillis();
            String orderTime = DateFormatUtils.format(orderTimeSeries, "yyyy-MM-dd HH:mm:ss");

            String status = statuss[RandomUtils.nextInt(0, statuss.length)];
            String phone = String.format("13%s%09d", (i+1)%9, i);
            //订单ID、用户名称、用户性别、商品名称、商品类型、生产商、下单时间、单价、数量、总价、订单状态、 收货地址、联系方式
            Order order = new Order(orderId, userName, gender, goods, goodsType, brand, orderTime, orderTimeSeries , price, num, totalPrice, status, address, Long.parseLong(phone));
            String orderJson = new Gson().toJson(order);
            System.out.println(orderJson);
            i++;
            //向kafka队列发送数据
            kafkaStreamServer.sendMsg(topic, orderJson);
            //模拟不同时间段的消费量
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(orderTimeSeries);
            int h = calendar.get(Calendar.HOUR_OF_DAY);
            int startInt = 700;
            if (8 > h){
                startInt = 1500;
            }else if (h>=8 && h<18){
                startInt = 300;
            }else if(h >= 18 && h < 22) {
                startInt = 100;
            }
            //线程休眠
            TimeUnit.MILLISECONDS.sleep(RandomUtils.nextInt(startInt, 3000));
        }
    }
}
