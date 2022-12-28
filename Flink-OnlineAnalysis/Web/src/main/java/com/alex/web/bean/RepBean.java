package com.alex.web.bean;

import lombok.Data;

/**
 * @Description
 * @Author JL
 * @Date 2020/10/09
 * @Version V1.0
 */
@Data
public class RepBean {
    private String name;
    private Object value;

    public RepBean(){}

    public RepBean(String name, Object value){
        this.name = name;
        this.value = value;
    }
}
