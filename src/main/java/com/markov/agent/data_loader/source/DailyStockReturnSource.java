package com.markov.agent.data_loader.source;

import com.markov.agent.domain.value_object.DailyStock;

import java.util.List;
import java.util.Map;

public interface DailyStockReturnSource {

    void saveDailyReturnStock(Map<Integer, List<DailyStock>> dailyStockReturns);
}
