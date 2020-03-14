package com.john.mvcframework.demo.service.impl;

import com.john.mvcframework.annotations.JohnService;
import com.john.mvcframework.demo.service.Iservice;

/**
 * @author:wenwei
 * @date:2020/03/09
 * @description:
 */
@JohnService
public class ServiceImpl implements Iservice {
    @Override
    public String  query(String name) {
        System.out.println("query:name");
        return name;
    }
}
