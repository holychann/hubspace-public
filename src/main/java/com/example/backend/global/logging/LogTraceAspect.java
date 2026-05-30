package com.example.backend.global.logging;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class LogTraceAspect {

    private final ThreadLocal<Integer> depthHolder = ThreadLocal.withInitial(() -> 0);

    @Around("within(com.example.backend.domain..controller..*) || " +
            "within(com.example.backend.domain..service..*) || " +
            "within(com.example.backend.infra.google.drive.GoogleDriveServiceImpl)")
    public Object trace(ProceedingJoinPoint joinPoint) throws Throwable {
        Logger targetLogger = getTargetLogger(joinPoint);
        int depth = depthHolder.get();
        String methodName = getMethodName(joinPoint);
        String prefix = createPrefix(depth);

        long startTime = System.currentTimeMillis();

        targetLogger.info("{}--> {}", prefix, methodName);

        depthHolder.set(depth + 1);

        try {
            Object result = joinPoint.proceed();

            long elapsedTime = System.currentTimeMillis() - startTime;

            depthHolder.set(depth);

            targetLogger.info("{}<-- {} {}ms", prefix, methodName, elapsedTime);

            return result;
        } catch (Throwable e) {
            long elapsedTime = System.currentTimeMillis() - startTime;

            depthHolder.set(depth);

            targetLogger.error("{}<X-- {} {}ms ex={}", prefix, methodName, elapsedTime, e.getClass().getSimpleName());

            throw e;
        } finally {
            if (depth == 0) {
                depthHolder.remove();
            }
        }
    }

    private Logger getTargetLogger(ProceedingJoinPoint joinPoint) {
        Class<?> targetClass = AopUtils.getTargetClass(joinPoint.getTarget());
        return LoggerFactory.getLogger(targetClass);
    }

    private String getMethodName(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();

        return signature.getName();
    }

    private String createPrefix(int depth) {
        if (depth == 0) {
            return "";
        }

        return "|   ".repeat(depth) + "|";
    }
}