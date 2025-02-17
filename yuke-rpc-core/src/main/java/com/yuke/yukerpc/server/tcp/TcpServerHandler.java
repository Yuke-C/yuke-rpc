package com.yuke.yukerpc.server.tcp;

import com.yuke.yukerpc.model.RpcRequest;
import com.yuke.yukerpc.model.RpcResponse;
import com.yuke.yukerpc.protocol.ProtocolMessage;
import com.yuke.yukerpc.protocol.ProtocolMessageDecoder;
import com.yuke.yukerpc.protocol.ProtocolMessageEncoder;
import com.yuke.yukerpc.protocol.ProtocolMessageTypeEnum;
import com.yuke.yukerpc.registry.LocalRegistry;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetSocket;

import java.io.IOException;
import java.lang.reflect.Method;

public class TcpServerHandler implements Handler<NetSocket> {
    @Override
    public void handle(NetSocket socket) {
        //处理链接
        TcpBufferHandlerWrapper bufferHandlerWrapper = new TcpBufferHandlerWrapper(buffer -> {
            //接受请求，解码
            ProtocolMessage<RpcRequest> protocolMessage;
            try {
                protocolMessage = (ProtocolMessage<RpcRequest>) ProtocolMessageDecoder.decode(buffer);
            } catch (IOException e) {
                throw new RuntimeException("协议消息解码错误");
            }
            RpcRequest rpcRequest = protocolMessage.getBody();

            // 处理请求
            // 构造响应结果对象
            RpcResponse rpcResponse = new RpcResponse();
            try{
                //获取要调用的服务实现类，通过反射调用
                Object serviceImpl = LocalRegistry.get(rpcRequest.getServiceName());
                if (serviceImpl == null) {
                    throw new RuntimeException("服务未注册: " + rpcRequest.getServiceName());
                }
                // 通过实例反射调用方法
                Method method = serviceImpl.getClass().getMethod(
                        rpcRequest.getMethodName(),
                        rpcRequest.getParameterTypes()
                );
                Object result = method.invoke(serviceImpl, rpcRequest.getArgs()); // 使用实例调用
//                Class<?> implClass = LocalRegistry.get(rpcRequest.getServiceName());
//                Method method = implClass.getMethod(rpcRequest.getMethodName(), rpcRequest.getParameterTypes());
//                Object result = method.invoke(implClass.getDeclaredConstructor().newInstance(), rpcRequest.getArgs());
                rpcResponse.setData(result);
                rpcResponse.setDataType(method.getReturnType());
                rpcResponse.setMessage("ok");
            }catch (Exception e){
                e.printStackTrace();
                rpcResponse.setMessage(e.getMessage());
                rpcResponse.setException(e);
            }

            //发送响应，编码
            ProtocolMessage.Header header = protocolMessage.getHeader();
            header.setType((byte)ProtocolMessageTypeEnum.RESPONSE.getKey());
            ProtocolMessage<RpcResponse> rpcResponseProtocolMessage = new ProtocolMessage<>(header, rpcResponse);
            try {
                Buffer encode = ProtocolMessageEncoder.encode(rpcResponseProtocolMessage);
                socket.write(encode);
            } catch (IOException e) {
                throw new RuntimeException("协议消息编码错误");
            }
        });
        socket.handler(bufferHandlerWrapper);
    }
}
