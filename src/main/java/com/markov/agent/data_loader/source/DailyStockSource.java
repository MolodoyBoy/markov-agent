package com.markov.agent.data_loader.source;

import com.markov.agent.domain.value_object.DailyStock;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface DailyStockSource {

    void saveDailyStock(Map<Integer, List<DailyStock>> dailyStocks);

    Map<Integer, DailyStock> getDailyStock(Set<Integer> companyIds, LocalDate date);
}