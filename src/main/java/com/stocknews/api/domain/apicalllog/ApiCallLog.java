package com.stocknews.api.domain.apicalllog;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "api_call_log",
    indexes = @Index(name = "idx_api_call_log_provider_called", columnList = "provider, called_at")
)
@Getter
@NoArgsConstructor
public class ApiCallLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String provider;   // finnhub / fred / gemini

    @Column(nullable = false, length = 255)
    private String endpoint;

    @Column(name = "called_at", nullable = false)
    private LocalDateTime calledAt;

    @Column(nullable = false, length = 20)
    private String status;     // SUCCESS / RATE_LIMITED / ERROR

    @Builder
    public ApiCallLog(String provider, String endpoint, String status) {
        this.provider = provider;
        this.endpoint = endpoint;
        this.status = status;
        this.calledAt = LocalDateTime.now();
    }
}
