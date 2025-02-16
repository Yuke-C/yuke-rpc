package com.yuke.examplespringbootprovider;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

@SpringBootTest
class ExampleSpringbootProviderApplicationTests {

    @Autowired
    private ApplicationContext applicationContext;
    @Test
    public void listBeans() {
        String[] beanNames = applicationContext.getBeanDefinitionNames();
        for (String beanName : beanNames) {
            System.out.println(beanName);
        }
    }
    @Test
    void contextLoads() {
    }

}
