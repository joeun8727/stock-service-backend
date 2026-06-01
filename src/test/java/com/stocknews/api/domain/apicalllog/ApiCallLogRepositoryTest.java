package com.stocknews.api.domain.apicalllog;

import com.stocknews.api.support.RepositoryTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ApiCallLogRepositoryTest extends RepositoryTestSupport {

    @Autowired
    ApiCallLogRepository apiCallLogRepository;

    @Autowired
    EntityManager em;

    @Test
    void 저장_후_calledAt_자동_세팅_확인() {
        ApiCallLog log = ApiCallLog.builder()
                .provider("finnhub")
                .endpoint("/stock/metric")
                .status("SUCCESS")
                .build();

        ApiCallLog saved = apiCallLogRepository.saveAndFlush(log);
        em.clear();

        ApiCallLog found = apiCallLogRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getProvider()).isEqualTo("finnhub");
        assertThat(found.getEndpoint()).isEqualTo("/stock/metric");
        assertThat(found.getStatus()).isEqualTo("SUCCESS");
        assertThat(found.getCalledAt()).isNotNull();
    }

    @Test
    void countByProviderAndCalledAtAfter_시간_이후_건수() {
        LocalDateTime base = LocalDateTime.now().minusMinutes(1);

        apiCallLogRepository.saveAndFlush(ApiCallLog.builder().provider("finnhub").endpoint("/ep1").status("SUCCESS").build());
        apiCallLogRepository.saveAndFlush(ApiCallLog.builder().provider("finnhub").endpoint("/ep2").status("SUCCESS").build());
        apiCallLogRepository.saveAndFlush(ApiCallLog.builder().provider("gemini").endpoint("/ep3").status("SUCCESS").build());

        long finnhubCount = apiCallLogRepository.countByProviderAndCalledAtAfter("finnhub", base);
        long geminiCount  = apiCallLogRepository.countByProviderAndCalledAtAfter("gemini", base);
        long fredCount    = apiCallLogRepository.countByProviderAndCalledAtAfter("fred", base);

        assertThat(finnhubCount).isEqualTo(2);
        assertThat(geminiCount).isEqualTo(1);
        assertThat(fredCount).isZero();
    }

    @Test
    void countByProviderAndStatusAfter_상태별_필터() {
        LocalDateTime base = LocalDateTime.now().minusMinutes(1);

        apiCallLogRepository.saveAndFlush(ApiCallLog.builder().provider("gemini").endpoint("/analyze").status("SUCCESS").build());
        apiCallLogRepository.saveAndFlush(ApiCallLog.builder().provider("gemini").endpoint("/analyze").status("RATE_LIMITED").build());
        apiCallLogRepository.saveAndFlush(ApiCallLog.builder().provider("gemini").endpoint("/analyze").status("RATE_LIMITED").build());

        long successCount     = apiCallLogRepository.countByProviderAndStatusAfter("gemini", "SUCCESS", base);
        long rateLimitedCount = apiCallLogRepository.countByProviderAndStatusAfter("gemini", "RATE_LIMITED", base);
        long errorCount       = apiCallLogRepository.countByProviderAndStatusAfter("gemini", "ERROR", base);

        assertThat(successCount).isEqualTo(1);
        assertThat(rateLimitedCount).isEqualTo(2);
        assertThat(errorCount).isZero();
    }

    @Test
    void 미래_시간_이후_조회시_결과_없음() {
        apiCallLogRepository.saveAndFlush(ApiCallLog.builder().provider("fred").endpoint("/series").status("SUCCESS").build());

        LocalDateTime future = LocalDateTime.now().plusHours(1);
        long count = apiCallLogRepository.countByProviderAndCalledAtAfter("fred", future);

        assertThat(count).isZero();
    }
}
