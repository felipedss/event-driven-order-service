package com.platform.orderservice.ratelimit;

import com.platform.orderservice.config.RateLimiterProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class RateLimiterInterceptor implements HandlerInterceptor {

  private static final String RATE_LIMIT_RESPONSE =
      """
      {"error":"Too Many Requests","message":"Rate limit exceeded. Please slow down."}
      """;

  private final RateLimiterProperties properties;
  private final ConcurrentHashMap<String, TokenBucket> buckets = new ConcurrentHashMap<>();

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
      throws Exception {
    if (!properties.enabled()) {
      return true;
    }

    String clientIp = extractClientIp(request);
    TokenBucket bucket =
        buckets.computeIfAbsent(
            clientIp,
            ip -> new TokenBucket(properties.capacity(), properties.refillRatePerSecond()));

    if (bucket.tryConsume()) {
      return true;
    }

    response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.getWriter().write(RATE_LIMIT_RESPONSE.strip());
    return false;
  }

  private String extractClientIp(HttpServletRequest request) {
    String forwarded = request.getHeader("X-Forwarded-For");
    if (forwarded != null && !forwarded.isBlank()) {
      return forwarded.split(",")[0].trim();
    }
    return request.getRemoteAddr();
  }
}
