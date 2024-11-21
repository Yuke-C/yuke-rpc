package com.yuke.yukerpc.server;

import io.vertx.core.Vertx;

public class VertxHttpServer implements HttpServer{

    @Override
    public void doStart(int port) {
        //创建Vert.x实例
        Vertx vertx = Vertx.vertx();

        //创建http服务器
        io.vertx.core.http.HttpServer server = vertx.createHttpServer();

        //监听端口并处理请求
        server.requestHandler(new HttpServerHandler());

        //启动HTTP服务器并监听指定端口
        server.listen(port,result ->{
           if (result.succeeded()){
               System.out.println("Server is now listening on port:"+port);
           }else {
               System.err.println("Failed to start server:"+result.cause());
           }
        });
    }
}
