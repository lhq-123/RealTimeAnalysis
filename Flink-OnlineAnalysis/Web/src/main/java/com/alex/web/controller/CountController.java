package com.alex.web.controller;

import com.alex.web.bean.RepBean;
import com.alex.web.service.CountService;
import com.alex.web.util.ApiResult;
import com.alex.web.util.Constants;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 统计数据调度控制器
 */
@RestController
@RequestMapping("/count")
public class CountController {

    @Resource
    private CountService countService;

    private static List<Map<String,Object>> dataList = new ArrayList<>(100);

    /**
     * 获取订单总量
     * @return
     */
    @RequestMapping(value = "/totalNum", method = {RequestMethod.GET, RequestMethod.POST})
    public ApiResult totalNum(){
        Object value = countService.getDataStreamCountValue(Integer.TYPE, "FLINK_ORDER_TOTAL_NUM");
        return new ApiResult(value);
    }

    /**
     * 获取订单总额
     * @return
     */
    @RequestMapping(value = "/totalPrice", method = {RequestMethod.GET, RequestMethod.POST})
    public ApiResult totalPrice(){
        Object value = countService.getDataStreamCountValue(Double.TYPE, "FLINK_ORDER_TOTAL_PRICE");
        return new ApiResult(value);
    }

    /**
     * 获取当前1分钟内最新总量
     * @return
     */
    @RequestMapping(value = "/minuteNum", method = {RequestMethod.GET, RequestMethod.POST})
    public ApiResult minuteNum(String flag){
        Map<String,Object> dataMap = new HashMap<>(2);
        String value = (String)countService.getDataStreamCountValue(String.class, "FLINK_ORDER_TIME_NUM");
        if (value.indexOf(":") != -1){
            String [] values = value.split(":");
            Date date = new Date(Long.parseLong(values[0]));
            dataMap.put("name", date);
            dataMap.put("value", new Object[]{DateFormatUtils.format(date, Constants.YYYY_MM_DD_HH_MM_SS2), Integer.parseInt(values[1])});
        }else {
            dataMap.put("name", new Date());
            dataMap.put("value", new Object[]{DateFormatUtils.format(new Date(), Constants.YYYY_MM_DD_HH_MM_SS2), Integer.parseInt(value)});
        }
        //缓存100个演示数据（有弊端，依赖前端轮询次数，正式环境因该考虑其它方案）
        int size = dataList.size();
        if (size == 100) {
            dataList.remove(0);
        }else if (size > 100){
            dataList = dataList.subList(size - 1 - 99, size - 1);
        }
        dataList.add(dataMap);
        //全量
        if (StringUtils.isNotBlank(flag) && flag.trim().equals("full")) {
            return new ApiResult(dataList);
        }else {
            //增量
            return new ApiResult(dataMap);
        }
    }

    /**
     * 获取各品牌总量与总额
     * @return
     */
    @RequestMapping(value = "/brand/sell", method = {RequestMethod.GET, RequestMethod.POST})
    public ApiResult brandSell(){
        List<RepBean> bufferList1 = countService.getDataStreamCountMap(Integer.TYPE, "FLINK_ORDER_BRAND_TOTAL_NUM");
        List<RepBean> bufferList2 = countService.getDataStreamCountMap(Double.TYPE, "FLINK_ORDER_BRAND_TOTAL_PRICE");
        Map<String, Object> dataMap = bufferList2.stream().collect(Collectors.toMap(RepBean::getName, RepBean::getValue));

        List<Object[]> dataList = new ArrayList<>();
        for (RepBean bean : bufferList1){
            Double totalPrice = (Double) dataMap.get(bean.getName());
            Object [] objs  = new Object[]{bean.getName(), bean.getValue(), BigDecimal.valueOf(totalPrice / 10000).setScale(2,BigDecimal.ROUND_HALF_UP)};
            dataList.add(objs);
        }
        return new ApiResult(dataList);
    }

    /**
     * 获取各性别消费总量
     * @return
     */
    @RequestMapping(value = "/gender/totalNum", method = {RequestMethod.GET, RequestMethod.POST})
    public ApiResult genderTotalNum(){
        List<RepBean> dataList = countService.getDataStreamCountMap(Integer.TYPE, "FLINK_ORDER_GENDER_TOTAL_NUM");
        return new ApiResult(dataList);
    }

    /**
     * 获取各性别消费总额
     * @return
     */
    @RequestMapping(value = "/gender/totalPrice", method = {RequestMethod.GET, RequestMethod.POST})
    public ApiResult genderTotalPrice(){
        List<RepBean> dataList = countService.getDataStreamCountMap(Double.TYPE, "FLINK_ORDER_GENDER_TOTAL_PRICE");
        return new ApiResult(dataList);
    }

    /**
     * 获取各性别消费时间与对应总量
     * @return
     */
    @RequestMapping(value = "/gender/shoppingTime", method = {RequestMethod.GET, RequestMethod.POST})
    public ApiResult genderShoppingTime(){
        List<RepBean> bufferList = countService.getDataStreamCountMap(String.class, "FLINK_ORDER_GENDER_TIME_NUM");
        List<Object[]> dataList = new ArrayList<>();
        for (RepBean bean : bufferList){
            String value = (String)bean.getValue();
            Map<String,Object> map = JSONObject.parseObject(value, Map.class);
            Object man = map.get("男");
            Object girl = map.get("女");
            Double d1 = man == null? null: Double.parseDouble(String.valueOf(man));
            Double d2 = girl == null? null: Double.parseDouble(String.valueOf(girl));
            Object [] objs  = new Object[]{bean.getName(), d1 == null?0:d1, d2 == null?0:d2};
            dataList.add(objs);
        }
        return new ApiResult(dataList);
    }

    /**
     * 获取各商品分类总量
     * @return
     */
    @RequestMapping(value = "/goodsType/totalNum", method = {RequestMethod.GET, RequestMethod.POST})
    public ApiResult goodsTotalNum(){
        List<RepBean> dataList = countService.getDataStreamCountMap(Integer.TYPE, "FLINK_ORDER_GOODS_TYPE_TOTAL_NUM");
        //对list对象集合，用lambda排倒序
        Comparator<RepBean> comparator = Comparator.comparingInt(r -> ((Integer) r.getValue()));
        List<RepBean> repList = dataList.stream().sorted(comparator.reversed()).collect(Collectors.toList());
        return new ApiResult(repList);
    }

    /**
     * 获取各商品分类总额
     * @return
     */
    @RequestMapping(value = "/goodsType/totalPrice", method = {RequestMethod.GET, RequestMethod.POST})
    public ApiResult goodsTotalPrice(){
        List<RepBean> dataList = countService.getDataStreamCountMap(Double.TYPE, "FLINK_ORDER_GOODS_TYPE_TOTAL_PRICE");
        return new ApiResult(dataList);
    }

    /**
     * 获取用户消费排名（只取前10）
     * @return
     */
    @RequestMapping(value = "/user/ranking", method = {RequestMethod.GET, RequestMethod.POST})
    public ApiResult userRanking(){
        List<RepBean> dataList = countService.getDataStreamCountMap(Double.TYPE, "FLINK_ORDER_USER_RANKING");
        //对list对象集合，用lambda排倒序
        Comparator<RepBean> comparator = Comparator.comparingInt(r -> (((Double) r.getValue()).intValue()));
        List<RepBean> repList = dataList.stream().sorted(comparator.reversed()).limit(10).collect(Collectors.toList());
        return new ApiResult(repList);
    }
}
