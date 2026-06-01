package com.stocknews.api.domain.apicalllog;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface ApiCallLogRepository extends JpaRepository<ApiCallLog, Long> {

    // 특정 provider의 특정 시간 이후 호출 횟수 — Rate Limit 모니터링용
    long countByProviderAndCalledAtAfter(String provider, LocalDateTime after);

    // 특정 provider + status 최신 여부 확인
    @Query("SELECT COUNT(a) FROM ApiCallLog a WHERE a.provider = :provider AND a.status = :status AND a.calledAt > :after")
    long countByProviderAndStatusAfter(@Param("provider") String provider,
                                       @Param("status") String status,
                                       @Param("after") LocalDateTime after);
}
