package com.markov.agent.domain.source;

import com.markov.agent.domain.value_object.DailyStock;

public interface CompanySearchSource {

    DailyStock searchLatestDailyStockReturn(String query);
}