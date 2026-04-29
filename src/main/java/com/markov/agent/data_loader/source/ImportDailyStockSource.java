package com.markov.agent.data_loader.source;

import com.markov.agent.data_loader.client.ExternalClientException;
import com.markov.agent.data_loader.value_object.Company;
import com.markov.agent.domain.value_object.DailyStock;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface ImportDailyStockSource {

    Map<Integer, List<DailyStock>> getDailyStock(List<Company> companies, LocalDate startDate) throws ExternalClientException;
}