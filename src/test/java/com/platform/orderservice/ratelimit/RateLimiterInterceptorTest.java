package com.platform.orderservice.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import com.platform.orderservice.config.RateLimiterProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RateLimiterInterceptorTest {

  private MockHttpServletRequest request;
  private MockHttpServletResponse response;
  private Object handler;

  @BeforeEach
  void setUp() {
    request = new MockHttpServletRequest();
    response = new MockHttpServletResponse();
    handler = new Object();
  }

  private RateLimiterInterceptor interceptor(boolean enabled, int capacity, double refillRate) {
    return new RateLimiterInterceptor(new RateLimiterProperties(enabled, capacity, refillRate));
  }

  @Test
  void allowsRequest_whenDisabled() throws Exception {
    var interceptor = interceptor(false, 0, 0.0);
    request.setRemoteAddr("10.0.0.1");

    assertThat(interceptor.preHandle(request, response, handler)).isTrue();
    assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
  }

  @Test
  void allowsRequests_withinCapacity() throws Exception {
    var interceptor = interceptor(true, 3, 1.0);
    request.setRemoteAddr("10.0.0.1");

    for (int i = 0; i < 3; i++) {
      assertThat(interceptor.preHandle(request, response, handler)).isTrue();
    }
  }

  @Test
  void returns429_whenCapacityExhausted() throws Exception {
    var interceptor = interceptor(true, 2, 1.0);
    request.setRemoteAddr("10.0.0.1");

    interceptor.preHandle(request, response, handler);
    interceptor.preHandle(request, response, handler);

    assertThat(interceptor.preHandle(request, response, handler)).isFalse();
    assertThat(response.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
  }

  @Test
  void responseHasJsonContentType_whenRejected() throws Exception {
    var interceptor = interceptor(true, 1, 1.0);
    request.setRemoteAddr("10.0.0.1");

    interceptor.preHandle(request, response, handler);
    interceptor.preHandle(request, response, handler);

    assertThat(response.getContentType()).isEqualTo(MediaType.APPLICATION_JSON_VALUE);
  }

  @Test
  void responseBodyContainsErrorMessage_whenRejected() throws Exception {
    var interceptor = interceptor(true, 1, 1.0);
    request.setRemoteAddr("10.0.0.1");

    interceptor.preHandle(request, response, handler);
    interceptor.preHandle(request, response, handler);

    assertThat(response.getContentAsString()).contains("Too Many Requests");
    assertThat(response.getContentAsString()).contains("Rate limit exceeded");
  }

  @Test
  void extractsIp_fromRemoteAddr_whenNoForwardedHeader() throws Exception {
    var interceptor = interceptor(true, 1, 1.0);
    request.setRemoteAddr("192.168.1.10");

    interceptor.preHandle(request, response, handler);
    assertThat(interceptor.preHandle(request, response, handler)).isFalse();
  }

  @Test
  void extractsIp_fromXForwardedFor_header() throws Exception {
    var interceptor = interceptor(true, 1, 1.0);
    request.setRemoteAddr("10.0.0.1");
    request.addHeader("X-Forwarded-For", "203.0.113.5");

    interceptor.preHandle(request, response, handler);
    assertThat(interceptor.preHandle(request, response, handler)).isFalse();
  }

  @Test
  void extractsFirstIp_fromXForwardedFor_whenMultiplePresent() throws Exception {
    var interceptorA = interceptor(true, 1, 1.0);

    var reqA = new MockHttpServletRequest();
    reqA.addHeader("X-Forwarded-For", "203.0.113.5, 10.0.0.1, 172.16.0.1");

    var reqB = new MockHttpServletRequest();
    reqB.addHeader("X-Forwarded-For", "203.0.113.5");

    // exhaust bucket for 203.0.113.5
    interceptorA.preHandle(reqA, response, handler);

    // same first IP → rejected
    assertThat(interceptorA.preHandle(reqB, response, handler)).isFalse();
  }

  @Test
  void tracksCapacity_perIpIndependently() throws Exception {
    var interceptor = interceptor(true, 1, 1.0);

    var reqA = new MockHttpServletRequest();
    reqA.setRemoteAddr("10.0.0.1");

    var reqB = new MockHttpServletRequest();
    reqB.setRemoteAddr("10.0.0.2");

    interceptor.preHandle(reqA, response, handler); // exhausts 10.0.0.1

    // 10.0.0.1 rejected
    assertThat(interceptor.preHandle(reqA, response, handler)).isFalse();
    // 10.0.0.2 still has its own full bucket
    assertThat(interceptor.preHandle(reqB, response, handler)).isTrue();
  }
}
