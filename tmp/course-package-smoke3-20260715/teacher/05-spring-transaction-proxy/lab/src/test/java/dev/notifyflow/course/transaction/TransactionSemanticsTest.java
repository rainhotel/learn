package dev.notifyflow.course.transaction;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class TransactionSemanticsTest {

    private static AnnotationConfigApplicationContext context;

    @AfterAll
    static void closeContext() {
        if (context != null) {
            context.close();
        }
    }

    @Test
    void transactionalServiceIsCreatedAsAnAopProxy() {
        Class<?> configClass;
        try {
            configClass = Class.forName(
                    "dev.notifyflow.course.transaction.TransactionLabConfig"
            );
        } catch (ClassNotFoundException exception) {
            fail("TransactionLabConfig 尚未实现，先观察 RED，再补最小实现");
            return;
        }

        context = new AnnotationConfigApplicationContext(configClass);
        Object service = context.getBean("proxyProbeService");

        assertTrue(AopUtils.isAopProxy(service), "@Transactional Bean 应由 AOP 代理包装");
    }
}
