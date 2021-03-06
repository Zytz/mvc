package com.john.mvcframework.demo.controller;

import com.john.mvcframework.annotations.JohnAutowired;
import com.john.mvcframework.annotations.JohnController;
import com.john.mvcframework.annotations.JohnRequestMapping;
import com.john.mvcframework.annotations.JohnSercurity;
import com.john.mvcframework.demo.service.impl.ServiceImpl;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * @author:wenwei
 * @date:2020/03/09
 * @description:
 */
@JohnController
@JohnRequestMapping("/demo")
public class DemoController {

    @JohnAutowired
    private ServiceImpl service;

    @JohnRequestMapping("/query")
    public String query(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse,String name )
    {
        System.out.println("/demo/query");
        return service.query(name);
    }
    @JohnRequestMapping("/handle01")
    @JohnSercurity({"zhangsan","john"})
    public String queryWithToken(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse,String username ) throws IOException {
//        System.out.println("/demo/handle");
//        httpServletResponse.getWriter().println(username+"have the right");
        return service.query(username);
    }
}
