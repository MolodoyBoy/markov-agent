package com.markov.agent.domain.value_object;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailyStock(
    int companyId,
    BigDecimal closeValue,
    LocalDate stockDate
) {
}