package com.yuke.yukerpc.proxy;


import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * Mock服务动态代理（JDK动态代理）
 */
@Slf4j
public class MockServiceProxy implements InvocationHandler {


    /**
     * 调用代理
     * @param proxy
     * @param method
     * @param args
     * @return
     * @throws Throwable
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        //根据方法的返回值类型，生成特定的默认值对象
        Class<?> methodReturnType = method.getReturnType();
        log.info("mock invoke {}",method.getName());
        return getDefaultObject(methodReturnType);
    }

    /**
     * 生成指定类型的默认值对象（可自行完善默认值逻辑）
     * @param type
     * @return
     */
    private Object getDefaultObject(Class<?> type) {
        if (type.isPrimitive()){
            if(type==boolean.class){
                return false;
            }else if(type==short.class){
                return (short) 0;
            }else if(type==int.class){
                return 0;
            }else if(type==long.class){
                return 0L;
            }
        }
        //对象类型
        return null;
    }

    /** TODO copilot优化
     * 生成指定类型的默认值对象
     *
     * @param type
     * @return
     */
 /*   private Object getDefaultObject(Class<?> type) {
        // 基本类型
        if (type.isPrimitive()) {
            if (type == boolean.class) {
                return false;
            } else if (type == short.class) {
                return (short) 0;
            } else if (type == int.class) {
                return 0;
            } else if (type == long.class) {
                return 0L;
            } else if (type == float.class) {
                return 0.0f;
            } else if (type == double.class) {
                return 0.0;
            } else if (type == byte.class) {
                return (byte) 0;
            } else if (type == char.class) {
                return '\u0000';
            }
        }

        // 对象类型
        try {
            Object instance = type.getDeclaredConstructor().newInstance();
            for (Field field : type.getDeclaredFields()) {
                if (!Modifier.isStatic(field.getModifiers())) {
                    field.setAccessible(true);
                    field.set(instance, getDefaultObject(field.getType()));
                }
            }
            return instance;
        } catch (Exception e) {
            log.error("Error creating mock object for type: {}", type, e);
        }

        return null;
    }*/
}
