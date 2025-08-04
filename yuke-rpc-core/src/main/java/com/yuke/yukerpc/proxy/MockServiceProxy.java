package com.yuke.yukerpc.proxy;

import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mock服务动态代理（JDK动态代理）
 */
@Slf4j
public class MockServiceProxy implements InvocationHandler {


    private static Map<Class<?>, Object> DEFAULT_VALUES_CACHE;

    private static Map<Class<?>, Object> EMPTY_ARRAYS_CACHE;

    public MockServiceProxy() {
        DEFAULT_VALUES_CACHE = new ConcurrentHashMap<>();
        EMPTY_ARRAYS_CACHE = new ConcurrentHashMap<>();
        // 预填充基本类型和常用包装类型的默认值
        DEFAULT_VALUES_CACHE.put(boolean.class, false);
        DEFAULT_VALUES_CACHE.put(short.class, (short) 0);
        DEFAULT_VALUES_CACHE.put(int.class, 0);
        DEFAULT_VALUES_CACHE.put(long.class, 0L);
        DEFAULT_VALUES_CACHE.put(float.class, 0.0f);
        DEFAULT_VALUES_CACHE.put(double.class, 0.0);
        DEFAULT_VALUES_CACHE.put(byte.class, (byte) 0);
        DEFAULT_VALUES_CACHE.put(char.class, '\u0000');

        DEFAULT_VALUES_CACHE.put(Boolean.class, false);
        DEFAULT_VALUES_CACHE.put(Short.class, (short) 0);
        DEFAULT_VALUES_CACHE.put(Integer.class, 0);
        DEFAULT_VALUES_CACHE.put(Long.class, 0L);
        DEFAULT_VALUES_CACHE.put(Float.class, 0.0f);
        DEFAULT_VALUES_CACHE.put(Double.class, 0.0);
        DEFAULT_VALUES_CACHE.put(Byte.class, (byte) 0);
        DEFAULT_VALUES_CACHE.put(Character.class, '\u0000');
        DEFAULT_VALUES_CACHE.put(String.class, "");

        // 预填充空数组
        EMPTY_ARRAYS_CACHE.put(boolean[].class, new boolean[0]);
        EMPTY_ARRAYS_CACHE.put(short[].class, new short[0]);
        EMPTY_ARRAYS_CACHE.put(int[].class, new int[0]);
        EMPTY_ARRAYS_CACHE.put(long[].class, new long[0]);
        EMPTY_ARRAYS_CACHE.put(float[].class, new float[0]);
        EMPTY_ARRAYS_CACHE.put(double[].class, new double[0]);
        EMPTY_ARRAYS_CACHE.put(byte[].class, new byte[0]);
        EMPTY_ARRAYS_CACHE.put(char[].class, new char[0]);
        EMPTY_ARRAYS_CACHE.put(Object[].class, new Object[0]);
    }

    /**
     * 调用代理
     *
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
        log.info("mock invoke {}", method.getName());
        return getDefaultObject(methodReturnType);
    }

    /**
     * 生成指定类型的默认值对象
     *
     * @param type
     * @return
     */
    private Object getDefaultObject(Class<?> type) {
        return getDefaultObject(type, 0);
    }

    private Object getDefaultObject(Class<?> type, int layers) {
        if (layers >= 3) return null;
        // 1. 处理缓存中已有的类型
        Object cached = DEFAULT_VALUES_CACHE.get(type);
        if (cached != null) return cached;

        // 2. 处理数组类型（包括多维数组）
        if (type.isArray()) {
            return EMPTY_ARRAYS_CACHE.computeIfAbsent(type, k ->
                    Array.newInstance(type.getComponentType(), 0)
            );
        }

        // 3. 处理枚举类型
        if (type.isEnum()) {
            Object[] enumConstants = type.getEnumConstants();
            return enumConstants.length > 0 ? enumConstants[0] : null;
        }

        // 4. 处理集合和Map类型
        if (Collection.class.isAssignableFrom(type)) {
            if (type == Set.class || type == HashSet.class) {
                return Collections.emptySet();
            }
            return Collections.emptyList();
        }

        if (Map.class.isAssignableFrom(type)) {
            return Collections.emptyMap();
        }

        // 5. 处理对象类型（带循环引用检测）
        try {
            // 尝试直接创建实例
            Object instance = type.getDeclaredConstructor().newInstance();

            // 递归初始化带循环引用检测
            return initializeWithCycleDetection(instance, new IdentityHashMap<>(), layers);
        } catch (Exception e) {
            log.warn("Cannot create default instance for type: {}", type, e);
            return null;
        }
    }

    private Object initializeWithCycleDetection(Object instance,
                                                IdentityHashMap<Object, Boolean> visited,
                                                int layers) {
        // 如果已经访问过该对象，直接返回
        if (visited.put(instance, Boolean.TRUE) != null) {
            return instance;
        }

        Class<?> type = instance.getClass();

        try {
            for (Field field : type.getDeclaredFields()) {
                if (!Modifier.isStatic(field.getModifiers())) {
                    field.setAccessible(true);

                    // 获取字段当前值（可能已被其他初始化逻辑设置）
                    Object currentValue = field.get(instance);

                    // 只有未初始化字段才设置默认值
                    if (currentValue == null) {
                        Object defaultValue = getDefaultObject(field.getType(), layers + 1);
                        field.set(instance, defaultValue);

                        // 递归初始化新创建的对象（如果是引用类型）
                        if (defaultValue != null && !field.getType().isPrimitive()) {
                            // 跳过自引用字段
                            if (defaultValue != instance) {
                                initializeWithCycleDetection(defaultValue, visited, layers + 1);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Error initializing fields for type: {}", type, e);
        }

        return instance;
    }

}