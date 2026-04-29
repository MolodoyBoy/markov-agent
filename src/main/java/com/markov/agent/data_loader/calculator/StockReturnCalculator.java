package com.markov.agent.data_loader.calculator;

import com.markov.agent.domain.value_object.DailyStock;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

import static java.util.Comparator.comparing;

@Service
public class StockReturnCalculator {

    public Map<Integer, List<DailyStock>> calculate(Map<Integer, List<DailyStock>> dailyStocks) {
        Map<Integer, List<DailyStock>> stockReturns = new HashMap<>();

        dailyStocks.forEach((companyId, dailyStocksForCompany) -> {
            List<DailyStock> sortedDailyStocks = dailyStocksForCompany.stream()
                .sorted(comparing(DailyStock::stockDate))
                .toList();

            BigDecimal previousPrice = null;
            List<DailyStock> returnsForCompany = new ArrayList<>();
            for (DailyStock dailyStock : sortedDailyStocks) {
                if (previousPrice != null) {
                    BigDecimal closeValue = dailyStock.closeValue();
                    BigDecimal returnValue = closeValue.subtract(previousPrice)
                        .divide(previousPrice, 4, RoundingMode.HALF_UP);

                    DailyStock result = new DailyStock(companyId, returnValue, dailyStock.stockDate());
                    returnsForCompany.add(result);
                }

                previousPrice = dailyStock.closeValue();
            }

            stockReturns.put(companyId, returnsForCompany);
        });

        return stockReturns;
    }
}