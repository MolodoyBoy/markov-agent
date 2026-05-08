package com.markov.agent.rest_api.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.util.StopWatch;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

public class RequestLoggingInterceptor implements HandlerInterceptor {

    private static final String STOPWATCH_ATTR = "logging_startTime";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (handler instanceof HandlerMethod) {
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            request.setAttribute(STOPWATCH_ATTR, stopWatch);
        }

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        if (handler instanceof HandlerMethod handlerMethod) {
            Logger logger = getLogger(handlerMethod);
            Object stopWatchAttr = request.getAttribute(STOPWATCH_ATTR);
            request.removeAttribute(STOPWATCH_ATTR);

            if (stopWatchAttr != null) {
                StopWatch stopWatch = (StopWatch) stopWatchAttr;
                stopWatch.stop();

                HttpStatus httpStatus = HttpStatus.valueOf(response.getStatus());
                String requestQuery = request.getQueryString() != null ? "?" + request.getQueryString() : "";
                String requestString = request.getRequestURI() + requestQuery;

                logger.info(
                    "{} \"{}\" {} ({})",
                    request.getMethod(),
                    requestString,
                    httpStatus,
                    stopWatch.getLastTaskTimeMillis() + " ms"
                );
            }
        }
    }

    private Logger getLogger(HandlerMethod handler) {
        return LoggerFactory.getLogger(handler.getBeanType());
    }
}
