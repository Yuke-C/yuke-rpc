package com.yuke.yukerpc.registry;

import com.yuke.yukerpc.model.RpcResponse;
import com.yuke.yukerpc.serializer.Serializer;
import com.yuke.yukerpc.serializer.SerializerFactory;

import java.io.IOException;
import java.io.Serializable;

public class RegistryTest {

    public static void main(String[] args) throws IOException {
        Serializer serializer = SerializerFactory.getInstance("hessian");

        RpcResponse rpcResponse=new RpcResponse();
        rpcResponse.setData(getNum());
        byte[] bytes = serializer.serialize(rpcResponse);
        RpcResponse response = serializer.deserialize(bytes, RpcResponse.class);
        System.out.println(response.getData());
    }

    public static Short getNum() {
        short i = Short.parseShort("1");
        return  i;
    }
}



