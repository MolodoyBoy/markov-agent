package com.markov.agent.data_loader.importer;

import com.markov.agent.data_loader.calculator.StockReturnCalculator;
import com.markov.agent.data_loader.client.ExternalClientException;
import com.markov.agent.data_loader.source.CompanySource;
import com.markov.agent.data_loader.source.ImportDailyStockSource;
import com.markov.agent.data_loader.source.DailyStockReturnSource;
import com.markov.agent.data_loader.source.DailyStockSource;
import com.markov.agent.data_loader.value_object.Company;
import com.markov.agent.domain.value_object.DailyStock;
import org.slf4j.Logger;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

import static java.util.stream.Collectors.*;
import static org.slf4j.LoggerFactory.getLogger;
import static com.markov.agent.data_loader.common.Utils.sleep;
import static com.markov.agent.data_loader.importer.ImportResult.empty;
import static com.markov.agent.data_loader.importer.ImportResult.error;
import static com.markov.agent.data_loader.importer.ImportResult.ImportStatus.*;

@Component
public class DailyStockImporter implements Importer {

    private static final Logger logger = getLogger(DailyStockImporter.class);

    private static final int BATCH_SIZE = 50;

    private final DailyStockImporter self;
    private final CompanySource companySource;
    private final DailyStockSource dailyStockSource;
    private final MarkovChainImporter markovChainImporter;
    private final StockReturnCalculator stockReturnCalculator;
    private final DailyStockReturnSource dailyStockReturnSource;
    private final ImportDailyStockSource importDailyStockSource;

    public DailyStockImporter(CompanySource companySource,
                              @Lazy DailyStockImporter self,
                              DailyStockSource dailyStockSource,
                              MarkovChainImporter markovChainImporter,
                              StockReturnCalculator stockReturnCalculator,
                              DailyStockReturnSource dailyStockReturnSource,
                              ImportDailyStockSource importDailyStockSource) {
        this.self = self;
        this.companySource = companySource;
        this.dailyStockSource = dailyStockSource;
        this.markovChainImporter = markovChainImporter;
        this.stockReturnCalculator = stockReturnCalculator;
        this.dailyStockReturnSource = dailyStockReturnSource;
        this.importDailyStockSource = importDailyStockSource;
    }

    @Override
    @Scheduled(cron = "0 0 0 * * *")
    public void startImport() {
        Integer companyId = companySource.getFirstCompanyIdWithDailyStock();

        logger.info("Start company new daily stock returns import.");

        List<Company> companies = null;

        while (companies == null || !companies.isEmpty()) {
            var importResult = self.importDailyReturns(companyId);
            if (importResult.isEmpty()) {
                break;
            }

            sleep(30_000);

            if (importResult.isError()) {
                continue;
            }

            companies = importResult.companies();
            companyId = importResult.lastCompanyId();
        }

        logger.info("Companies new daily stock returns import completed.");

        markovChainImporter.startImport();
    }

    @Transactional
    public ImportResult importDailyReturns(Integer companyId) {
        List<Company> companies = companySource.getCompaniesWithDailyStock(companyId, BATCH_SIZE);
        if (companies.isEmpty()) {
            return empty();
        }

        try {
            LocalDate previousDay = LocalDate.now().minusDays(1);

            var dailyStocks = importDailyStockSource.getDailyStock(companies, previousDay);

            if (!dailyStocks.isEmpty()) {
                dailyStocks = dailyStocks.values()
                    .stream()
                    .flatMap(List::stream)
                    .collect(groupingBy(DailyStock::companyId));

                dailyStockSource.saveDailyStock(dailyStocks);

                Map<Integer, DailyStock> previousDaysStocks = getPreviousDayStocks(companies);

                dailyStocks = merge(previousDaysStocks, dailyStocks);

                var dailyStockReturns = stockReturnCalculator.calculate(dailyStocks);
                dailyStockReturnSource.saveDailyReturnStock(dailyStockReturns);

                logger.info("Batch {} companies daily stock returns imported.", dailyStocks.size());
            }

            companyId = getLastCompanyId(companies);

            return new ImportResult(companies, companyId, SUCCESS);
        } catch (ExternalClientException e) {
            logger.error("Failed to import daily stock data for new companies. Will retry later.");
            return error();
        }
    }

    private Map<Integer, DailyStock> getPreviousDayStocks(List<Company> companies) {
        LocalDate previous = previous();
        Set<Integer> companyIds = companies.stream()
            .map(Company::id)
            .collect(toSet());

        return dailyStockSource.getDailyStock(companyIds, previous);
    }

    private LocalDate previous() {
        return LocalDate.now().minusDays(2);
    }

    private Map<Integer, List<DailyStock>> merge(Map<Integer, DailyStock> var1, Map<Integer, List<DailyStock>> var2) {
        Map<Integer, List<DailyStock>> result = new HashMap<>();

        var2.forEach((companyId, stocks) -> {
            List<DailyStock> merged = new ArrayList<>();
            DailyStock previous = var1.get(companyId);
            if (previous != null) {
                merged.add(previous);
            }
            merged.addAll(stocks);
            result.put(companyId, merged);
        });

        return result;
    }

    protected Integer getLastCompanyId(List<Company> companies) {
        try {
            Company last = companies.getLast();
            return last.id();
        } catch (NoSuchElementException e) {
            return null;
        }
    }
}