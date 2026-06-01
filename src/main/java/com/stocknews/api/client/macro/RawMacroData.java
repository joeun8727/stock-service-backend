package com.stocknews.api.client.macro;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RawMacroData(
        String seriesId,
        LocalDate date,
        BigDecimal value,
        String units
) {}
