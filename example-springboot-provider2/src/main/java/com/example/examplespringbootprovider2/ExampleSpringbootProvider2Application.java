package com.example.examplespringbootprovider2;

import com.yuke.yukerpc.springboot.starter.annotation.EnableRpc;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableRpc
public class ExampleSpringbootProvider2Application {

    public static void main(String[] args) {
        SpringApplication.run(ExampleSpringbootProvider2Application.class, args);
    }

}
