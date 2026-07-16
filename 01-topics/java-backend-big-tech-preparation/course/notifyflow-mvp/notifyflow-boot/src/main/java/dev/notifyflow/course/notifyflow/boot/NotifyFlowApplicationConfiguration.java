package dev.notifyflow.course.notifyflow.boot;

import java.time.Instant;
import java.util.UUID;

import dev.notifyflow.course.notifyflow.application.Clock;
import dev.notifyflow.course.notifyflow.application.CreateTaskService;
import dev.notifyflow.course.notifyflow.application.IdGenerator;
import dev.notifyflow.course.notifyflow.application.TaskCreationStore;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NotifyFlowApplicationConfiguration {

    @Bean
    Clock notifyFlowClock() {
        return Instant::now;
    }

    @Bean
    IdGenerator idGenerator() {
        return () -> UUID.randomUUID().toString();
    }

    @Bean
    CreateTaskService createTaskService(
            TaskCreationStore taskCreationStore,
            Clock notifyFlowClock,
            IdGenerator idGenerator) {
        return new CreateTaskService(taskCreationStore, notifyFlowClock, idGenerator);
    }
}
