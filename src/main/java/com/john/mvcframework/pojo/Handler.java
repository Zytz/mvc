package com.john.mvcframework.pojo;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * @author:wenwei
 * @date:2020/03/10
 * @description:
 */

public class Handler {
    private Object controller;//method.invoke

    private Method method;

    private Pattern pattern;//Spring url支持正则

    private Map<String,Integer> paramIndexMapping;//参数顺序，string为可以，后面是参数顺序

    public Handler(Object controller, Method method, Pattern pattern) {
        this.controller = controller;
        this.method = method;
        this.pattern = pattern;
//        this.paramIndexMapping = paramIndexMapping;
    }

    public Object getController() {
        return controller;
    }

    public void setController(Object controller) {
        this.controller = controller;
    }

    public Method getMethod() {
        return method;
    }

    public void setMethod(Method method) {
        this.method = method;
    }

    public Pattern getPattern() {
        return pattern;
    }

    public void setPattern(Pattern pattern) {
        this.pattern = pattern;
    }

    public Map<String, Integer> getParamIndexMapping() {
        return paramIndexMapping;
    }

    public void setParamIndexMapping(Map<String, Integer> paramIndexMapping) {
        this.paramIndexMapping = paramIndexMapping;
    }
}
