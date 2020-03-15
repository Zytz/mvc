package com.john.mvcframework.demo.service.impl;

import com.john.mvcframework.annotations.JohnService;
import com.john.mvcframework.demo.service.IService;

/**
 * @author:wenwei
 * @date:2020/03/09
 * @description:
 */
@JohnService
public class ServiceImpl implements IService {
    @Override
    public String  query(String name) {
        System.out.println("you have the query right: "+name);
        return name;
    }
}
