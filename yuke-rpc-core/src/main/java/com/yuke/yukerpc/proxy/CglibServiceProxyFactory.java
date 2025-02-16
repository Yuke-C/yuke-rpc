package com.yuke.yukerpc.proxy;

import com.yuke.yukerpc.RpcApplication;
import net.sf.cglib.proxy.Enhancer;

import java.lang.reflect.Proxy;

public class CglibServiceProxyFactory {

    public static <T> T getProxy(Class<T> serviceClass) {
        if (RpcApplication.getRpcConfig().isMock()) {
            return getMock(serviceClass);
        }
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(serviceClass);
        enhancer.setCallback(new CglibServiceProxy());
        return (T) enhancer.create();
    }

    private static <T> T getMock(Class<T> serviceClass) {
        return (T) Proxy.newProxyInstance(
                serviceClass.getClassLoader(),
                new Class[] {serviceClass},
                new MockServiceProxy()
        );
    }

}
