package com.john.mvcframework.servlet;

import com.john.mvcframework.annotations.JohnAutowired;
import com.john.mvcframework.annotations.JohnController;
import com.john.mvcframework.annotations.JohnRequestMapping;
import com.john.mvcframework.annotations.JohnSercurity;
import com.john.mvcframework.annotations.JohnService;
import com.john.mvcframework.pojo.Handler;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author:wenwei
 * @date:2020/03/08
 * @description:
 */
public class JohnDispatchServlet extends HttpServlet {
    private Properties properties = new Properties();

    private List<String> classNames = new ArrayList<>();

    //ioc 容器
    private Map<String, Object> iocMap = new HashMap<>();

    //存储method 和 URL之间的映射关系
//    private Map<String, Method> handleMapping = new HashMap<>();

    private List<Handler> handlerList = new ArrayList<>();

    public JohnDispatchServlet() {
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        //1:加载配置文件，springmvc.properties
        String configContextLocation = config.getInitParameter("configContextLocation");

        doLoadConfig(configContextLocation);
        //2： 扫描相关的类，扫描注解
        doScan(properties.getProperty("scanPackage"));
        //3: 初始化bean对象，
        doInstance();
        //4: 实现依赖注入
        doAutoWired();
        //5： 构造一个handlemapping处理器映射器，，将配置好的URL和method构件映射关系
        initHandleMapping();
        //6: 等待请求，处理请求
//        super.init(config);
        System.out.println("********init ok");
    }


    private void initHandleMapping() {
        if (iocMap.isEmpty()) {
            return;
        }
        for (Map.Entry<String, Object> entry : iocMap.entrySet()) {
            //获取ioc中的class类型
            Class<?> aClass = entry.getValue().getClass();

            if (!aClass.isAnnotationPresent(JohnController.class)) {
                continue;
            }
            String baseUrl = "";
            if (aClass.isAnnotationPresent(JohnRequestMapping.class)) {
                JohnRequestMapping annotation = aClass.getAnnotation(JohnRequestMapping.class);
                baseUrl = annotation.value(); //demo
            }

            Method[] methods = aClass.getMethods();
            for (int i = 0; i < methods.length; i++) {
                Method method = methods[i];
                if (!method.isAnnotationPresent(JohnRequestMapping.class)) {
                    continue;
                }



                JohnRequestMapping annotation = method.getAnnotation(JohnRequestMapping.class);
                String value = annotation.value();
                baseUrl += value;

                //把method相关信息封装成handele
                Handler handler = new Handler(entry.getValue(), method, Pattern.compile(baseUrl));

                Parameter[] parameters = method.getParameters();
                for (int i1 = 0; i1 < parameters.length; i1++) {
                    if (parameters[i].getType() == HttpServletResponse.class || parameters[i].getType().equals(HttpServletRequest.class)) {
                        handler.getParamIndexMapping().put(parameters[i].getType().getSimpleName(), i);
                    } else {
                        handler.getParamIndexMapping().put(parameters[i].getName(), i);
                    }
                    //处理security的参数

                    //处理security
                    if(method.isAnnotationPresent(JohnSercurity.class)){
                        JohnSercurity sucurityAnnotion= method.getAnnotation(JohnSercurity.class);
                        String name = sucurityAnnotion.value();
                        if(parameters[i].getName().equals("userName")){

                            handler.getSecurityIndexMapping().put(parameters[i].getName(),name);
                        }
                    }
                }
                handlerList.add(handler);


//                handleMapping.put(baseUrl,method);
            }


        }


    }

    //实现依赖注入
    private void doAutoWired() {
        if (iocMap.isEmpty()) {
            return;
        }

        //分析容器中 类中是否有注解
        for (Map.Entry<String, Object> entry : iocMap.entrySet()) {
            //获取bean对象的字断信息
            Field[] declaredFields = entry.getValue().getClass().getDeclaredFields();
            for (int i = 0; i < declaredFields.length; i++) {
                Field declaredField = declaredFields[i];
                if (!declaredField.isAnnotationPresent(JohnAutowired.class)) {
                    continue;

                }

                JohnAutowired annotation = declaredField.getAnnotation(JohnAutowired.class);
                String beanName = annotation.value();
                if ("".equals(beanName)) {
                    beanName = declaredField.getType().getName();
                }
                declaredField.setAccessible(true);
                try {
                    declaredField.set(entry.getValue(), iocMap.get(beanName));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }

            }
        }

    }

    /**
     * 基于className缓存中的全限定类名
     * 反射技术
     * 对象的创建和管理
     */
    private void doInstance() {
        if (classNames.size() == 0) {
            return;
        }
        try {
            for (int i = 0; i < classNames.size(); i++) {
                String className = classNames.get(i);
                //反射获取类
                Class<?> aClass = Class.forName(className);
                //区分controller和service；
                if (aClass.isAnnotationPresent(JohnController.class)) {
                    //controller 没有id,直接首字母小写
                    String aClassSimpleName = aClass.getSimpleName();
                    String lowerFirstSimpleName = lowerFirst(aClassSimpleName);
                    Object object = aClass.newInstance();
                    iocMap.put(lowerFirstSimpleName, object);

                } else if (aClass.isAnnotationPresent(JohnService.class)) {
                    JohnService annotion = aClass.getAnnotation(JohnService.class);
                    //获取注解的值
                    String beanName = annotion.value();


                    //获取注解value值
                    if (!"".equals(beanName)) {
                        iocMap.put(beanName, aClass.newInstance());
                    } else {
                        //如果没有值，就是用默认的方式
                        beanName = lowerFirst(aClass.getSimpleName());
                        iocMap.put(beanName, aClass.newInstance());
                    }
                    //service 往往是有接口的，面向接口的开发；此时放入一份对象到IOC容器当中;便于后期跟接口类型注入
                    Class<?>[] interfaces = aClass.getInterfaces();
                    for (int i1 = 0; i1 < interfaces.length; i1++) {
                        Class<?> anInterface = interfaces[i];
                        //以接口的全限定类名作为ID放入
                        iocMap.put(anInterface.getName(), aClass.newInstance());
                    }
                } else {
                    continue;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    public String lowerFirst(String string) {
        char[] chars = string.toCharArray();
        if ('A' <= chars[0] && chars[0] <= 'Z') {
            chars[0] += 32;
        }
        return String.valueOf(chars);
    }

    private void doScan(String packagePath) {
        String scanPath = Thread.currentThread().getContextClassLoader().getResource("").getPath() + packagePath.replaceAll("\\.", "/");
        System.out.println(scanPath);

        File file = new File(scanPath);

        File[] files = file.listFiles();
        for (File scanfile : files) {
            if (!scanfile.exists()) {
                return;
            }
            if (scanfile.isDirectory()) {
                doScan(packagePath + "." + scanfile.getName());
            } else if (file.getName().endsWith(".class")) {
                String className = scanPath + "." + file.getName().replaceAll(".class", "");
                classNames.add(className);
            }
        }
    }

    private void doLoadConfig(String configContextLocation) {
        InputStream resourceAsStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(configContextLocation);
        try {
            properties.load(resourceAsStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
//        super.doGet(req, resp);
        String requestURI = req.getRequestURI();
//        Method method = handleMapping.get(requestURI);


        Handler handler = getHandleMapping(req);

        if (handler == null) {
            resp.getWriter().write("404 not found");
            return;
        }

        //参数绑定
        //后去所有参数类型数组。数组长度就是要处理的args的长度
        Class<?>[] parameterTypes = handler.getMethod().getParameterTypes();

        int length = parameterTypes.length;
        Object[] paravalues = new Object[length];

        //注意处理的顺序
        Map<String, String[]> parameterMap = req.getParameterMap();


        for (Map.Entry<String, String[]> parms : parameterMap.entrySet()) {

            if(handler.getSecurityIndexMapping().get(parms.getKey())!=null){

                if(handler.getSecurityIndexMapping().get(parms.getKey()).equals(parms.getValue())){
                    System.out.println("you do not have right,{}");
                    break;
                }
            }

            //name=1&name=2
            String value = StringUtils.join(parms.getValue(), ",");//1,2

            //如果参数和方法中参数匹配上了，填充数据
            if (!handler.getParamIndexMapping().containsKey(parms.getKey())) {
                continue;
            }
            //方法行残中确实有，找到索引位置；
            Integer integer = handler.getParamIndexMapping().get(parms.getKey());
            paravalues[integer] = value;

        }

        int reqindex = handler.getParamIndexMapping().get(HttpServletRequest.class.getSimpleName());
        paravalues[reqindex] = req;
        int respindex = handler.getParamIndexMapping().get(HttpServletResponse.class.getSimpleName());
        paravalues[respindex] = req;




        //最终调用的过程
        try {
            handler.getMethod().invoke(handler.getController(), paravalues);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }

    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
//        super.doPost(req, resp);

    }

    private Handler getHandleMapping(HttpServletRequest req) {
        if (handlerList.isEmpty()) {
            return null;
        }

        String url = req.getRequestURI();
        for (Handler han : handlerList) {
            Matcher matcher = han.getPattern().matcher(url);
            if (!matcher.matches()) {
                continue;
            }
            return han;
        }
        return null;

    }


}
