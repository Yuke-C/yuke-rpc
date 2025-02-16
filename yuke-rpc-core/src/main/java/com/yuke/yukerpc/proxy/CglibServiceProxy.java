package com.yuke.yukerpc.proxy;

import cn.hutool.core.collection.CollUtil;
import com.yuke.yukerpc.RpcApplication;
import com.yuke.yukerpc.config.RpcConfig;
import com.yuke.yukerpc.constant.RpcConstant;
import com.yuke.yukerpc.fault.retry.RetryStrategy;
import com.yuke.yukerpc.fault.retry.RetryStrategyFactory;
import com.yuke.yukerpc.fault.tolerant.TolerantStrategy;
import com.yuke.yukerpc.fault.tolerant.TolerantStrategyFactory;
import com.yuke.yukerpc.loadbalancer.LoadBalancer;
import com.yuke.yukerpc.loadbalancer.LoadBalancerFactory;
import com.yuke.yukerpc.model.RpcRequest;
import com.yuke.yukerpc.model.RpcResponse;
import com.yuke.yukerpc.model.ServiceMetaInfo;
import com.yuke.yukerpc.registry.Registry;
import com.yuke.yukerpc.registry.RegistryFactory;
import com.yuke.yukerpc.server.tcp.VertxTcpClient;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;

public class CglibServiceProxy implements MethodInterceptor {
    @Override
    public Object intercept(Object o, Method method, Object[] objects, MethodProxy methodProxy) throws Throwable {

        //构造请求
        String serviceName = method.getDeclaringClass().getName();
        RpcRequest rpcRequest = RpcRequest.builder()
                .serviceName(serviceName)
                .methodName(method.getName())
                .parameterTypes(method.getParameterTypes())
                .args(objects)
                .build();
        try {
            //从注册中心获取服务提供者请求地址
            //双检锁单例模式获取RpcConfig，同时根据配置信息完成注册中心初始化
            RpcConfig rpcConfig = RpcApplication.getRpcConfig();
            //根据配置信息中注册中心类别创建注册中心实例
            Registry registry = RegistryFactory.getInstance(rpcConfig.getRegistryConfig().getRegistry());
            ServiceMetaInfo serviceMetaInfo = new ServiceMetaInfo();
            serviceMetaInfo.setServiceName(serviceName);
            serviceMetaInfo.setServiceVersion(RpcConstant.DEFAULT_SERVICE_VERSION);
            //根据serviceKey(serviceName+serviceVersion)服务发现（获取某服务的所有节点，消费端）
            List<ServiceMetaInfo> serviceMetaInfoList = registry.serviceDiscovery(serviceMetaInfo.getServiceKey());
            if (CollUtil.isEmpty(serviceMetaInfoList)){
                throw new RuntimeException("暂无服务地址");
            }
            // 负载均衡
            LoadBalancer loadBalancer = LoadBalancerFactory.getInstance(rpcConfig.getLoadBalancer());
            // 将调用方法名（请求路径）作为负载均衡参数
            HashMap<String, Object> requestParams = new HashMap<>();
            requestParams.put("methodName",rpcRequest.getMethodName());
            ServiceMetaInfo selectedServiceMetaInfo = loadBalancer.select(requestParams,serviceMetaInfoList);

            //发送 TCP 请求
            // 使用重试机制
            RpcResponse rpcResponse;
            try {
                RetryStrategy retryStrategy = RetryStrategyFactory.getInstance(rpcConfig.getRetryStrategy());
                rpcResponse = retryStrategy.doRetry(() ->
                        VertxTcpClient.doRequest(rpcRequest, selectedServiceMetaInfo)
                );
            }catch (Exception e){
                // 容错机制
                TolerantStrategy tolerantStrategy = TolerantStrategyFactory.getInstance(rpcConfig.getTolerantStrategy());
                rpcResponse = tolerantStrategy.doTolerant(null, e);
            }
            return rpcResponse.getData();

            //发送 HTTP 请求
//            try (HttpResponse httpResponse = HttpRequest.post(selectedServiceMetaInfo.getServiceAddress())
//            .body(bytes)
//            .execute()){
//                byte[] result = httpResponse.bodyBytes();
//                RpcResponse rpcResponse = serializer.deserialize(result, RpcResponse.class);
//                return rpcResponse.getData();
//            }
        }catch (Exception e){
            e.printStackTrace();
            throw new RuntimeException("调用失败");
        }
    }
}
