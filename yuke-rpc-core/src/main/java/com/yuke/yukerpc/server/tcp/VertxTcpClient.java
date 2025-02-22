package com.yuke.yukerpc.server.tcp;

import cn.hutool.core.util.IdUtil;
import com.yuke.yukerpc.RpcApplication;
import com.yuke.yukerpc.model.RpcRequest;
import com.yuke.yukerpc.model.RpcResponse;
import com.yuke.yukerpc.model.ServiceMetaInfo;
import com.yuke.yukerpc.protocol.*;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetClientOptions;
import io.vertx.core.net.NetSocket;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * Vertx TCP 请求客户端
 */
public class VertxTcpClient {

    private static final Vertx VERTX  = Vertx.vertx();

    // 共享 NetClient 连接池配置
    private static final NetClient NET_CLIENT = VERTX.createNetClient(
            new NetClientOptions()
                    .setConnectTimeout(5000) // 5秒连接超时
                    .setReconnectInterval(1000) // 重试间隔
                    .setReconnectAttempts(3) // 最大重试次数
    );

    // 全局关闭钩子
    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down TCP client...");
            NET_CLIENT.close();
            VERTX.close().onSuccess(v -> System.out.println("Vertx closed successfully"));
        }));
    }
    /**
     * 发送请求
     *
     * @param rpcRequest
     * @param serviceMetaInfo
     * @return
     * @throws InterruptedException
     * @throws ExecutionException
     */
    public static RpcResponse doRequest(RpcRequest rpcRequest, ServiceMetaInfo serviceMetaInfo) throws InterruptedException, ExecutionException {
        // 发送 TCP 请求
        CompletableFuture<RpcResponse> responseFuture  = new CompletableFuture<>();
        NET_CLIENT.connect(serviceMetaInfo.getServicePort(), serviceMetaInfo.getServiceHost(),
                result -> {
                    if (!result.succeeded()){
                        System.err.println("Failed to connect to TCP server");
                        responseFuture.completeExceptionally(new RuntimeException("Failed to connect to TCP server"));
                        return;
                    }
                    NetSocket socket = result.result();
                    //构造消息
                    ProtocolMessage<RpcRequest> protocolMessage = buildMessage(rpcRequest);
                    //编码请求
                    try{
                        Buffer encodeBuffer = ProtocolMessageEncoder.encode(protocolMessage);
                        socket.write(encodeBuffer);
                    }catch (IOException e){
                        responseFuture.completeExceptionally(new RuntimeException("协议消息编码错误", e));
                        return;
                    }

                    //接受响应
                    TcpBufferHandlerWrapper bufferHandlerWrapper = new TcpBufferHandlerWrapper(buffer -> {
                        try {
                            ProtocolMessage<RpcResponse> rpcResponseProtocolMessage = (ProtocolMessage<RpcResponse>) ProtocolMessageDecoder.decode(buffer);
                            responseFuture.complete(rpcResponseProtocolMessage.getBody());
                        } catch (IOException e) {
                            responseFuture.completeExceptionally(new RuntimeException("协议消息解码错误", e));
                        }
                    });
                    socket.handler(bufferHandlerWrapper);
                });
        RpcResponse rpcResponse = responseFuture.get();
        return rpcResponse;
    }

    private static ProtocolMessage<RpcRequest> buildMessage(RpcRequest rpcRequest) {
        ProtocolMessage<RpcRequest> protocolMessage = new ProtocolMessage<>();
        ProtocolMessage.Header header = new ProtocolMessage.Header();
        header.setMagic(ProtocolConstant.PROTOCOL_MAGIC);
        header.setVersion(ProtocolConstant.PROTOCOL_VERSION);
        header.setSerializer((byte) ProtocolMessageSerializerEnum.getEnumByValue(RpcApplication.getRpcConfig().getSerializer()).getKey());
        header.setType((byte) ProtocolMessageTypeEnum.REQUEST.getKey());
        // 生成全局请求 ID
        header.setRequestId(IdUtil.getSnowflakeNextId());
        protocolMessage.setHeader(header);
        protocolMessage.setBody(rpcRequest);
        return protocolMessage;
    }
}
