package com.stocknews.api.client.financial;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RawStockProfile(
        String ticker,
        String companyName,
        String exchange,
        String industry,
        BigDecimal marketCapMillions, // 백만 USD 단위 (서비스 계층에서 *1_000_000 변환)
        String website,
        String logo,
        LocalDate ipoDate             // nullable
) {}
