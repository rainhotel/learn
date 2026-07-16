package dev.notifyflow.course.notifyflow.boot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "dev.notifyflow.course.notifyflow")
public class NotifyFlowApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotifyFlowApplication.class, args);
    }
}
