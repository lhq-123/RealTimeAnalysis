package com.alex.bean;

import lombok.AllArgsConstructor;
import lombok.Data;


/**
 * @author Alex_liu
 * @create 2022-12-27 10:19
 * @Description
 */
@Data
@AllArgsConstructor
public class Order implements java.io.Serializable{
    private String orderId;
    private String userName;
    private String gender;
    private String goods;
    private String goodsType;
    private String brand;
    private String orderTime;
    private Long orderTimeSeries;
    private Double price;
    private Integer num;
    private Double totalPrice;
    private String status;
    private String address;
    private Long phone;
}
