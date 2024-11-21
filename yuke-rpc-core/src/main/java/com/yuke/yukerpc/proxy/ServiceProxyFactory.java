package com.yuke.yukerpc.proxy;


import com.yuke.yukerpc.RpcApplication;

import java.lang.reflect.Proxy;

/**
 * 服务代理工厂（用于创建代理对象）
 */
public class ServiceProxyFactory {

    /**
     * 根据服务类获取代理对象
     * @param serviceClass
     * @param <T>
     * @return
     */
    public  static <T> T getProxy(Class<T> serviceClass){
        if (RpcApplication.getRpcConfig().isMock()){
            return getMock(serviceClass);
        }

        return (T) Proxy.newProxyInstance(
                //类加载器，用于定义代理类
                serviceClass.getClassLoader(),
                //代理类需要实现的接口列表，这里只需要代理 serviceClass 指定的接口
                new Class[] {serviceClass},
                //一个调用处理器（InvocationHandler）实例，当代理对象的方法被调用时，将会转发到这个调用处理器
                new ServiceProxy());
    }

    /**
     * 根据服务类获取Mock代理对象
     * @param serviceClass
     * @param <T>
     * @return
     */
    private static <T> T getMock(Class<T> serviceClass) {
        return (T)Proxy.newProxyInstance(
                serviceClass.getClassLoader(),
                new Class[] {serviceClass},
                new MockServiceProxy()
        );
    }
}
