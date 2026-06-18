package com.modelmesh.gateway;

import com.modelmesh.gateway.config.ModelMeshProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(ModelMeshProperties.class)
public class ModelMeshGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ModelMeshGatewayApplication.class, args);
    }
}
