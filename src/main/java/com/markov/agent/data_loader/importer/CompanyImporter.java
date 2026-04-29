package com.markov.agent.data_loader.importer;

import com.markov.agent.data_loader.calculator.StockReturnCalculator;
import com.markov.agent.data_loader.client.ExternalClientException;
import com.markov.agent.data_loader.source.*;
import com.markov.agent.data_loader.value_object.Company;
import com.markov.agent.data_loader.value_object.SaveCompanyCmd;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.time.LocalDate;
import java.util.Map;

import static java.util.concurrent.TimeUnit.*;
import static org.slf4j.LoggerFactory.getLogger;
import static com.markov.agent.data_loader.common.Utils.*;
import static com.markov.agent.data_loader.importer.ImportResult.*;
import static com.markov.agent.data_loader.importer.ImportResult.ImportStatus.*;

@Component
public class CompanyImporter implements Importer {

    private static final Logger logger = getLogger(CompanyImporter.class);

    private static final int BATCH_SIZE = 100;

    private static final LocalDate START_YEAR;

    static {
        START_YEAR = LocalDate.of(2024, 1, 1);
    }

    private final int chunkSize;
    private final CompanyImporter self;
    private final CompanySource companySource;
    private final DailyStockSource dailyStockSource;
    private final MarkovChainImporter markovChainImporter;
    private final StockReturnCalculator stockReturnCalculator;
    private final DailyStockReturnSource dailyStockReturnSource;
    private final ImportDailyStockSource importDailyStockSource;
    private final NewCompaniesQueueSource newCompaniesQueueSource;

    public CompanyImporter(CompanySource companySource,
                           @Lazy CompanyImporter self,
                           DailyStockSource dailyStockSource,
                           MarkovChainImporter markovChainImporter,
                           StockReturnCalculator stockReturnCalculator,
                           DailyStockReturnSource dailyStockReturnSource,
                           ImportDailyStockSource importDailyStockSource,
                           NewCompaniesQueueSource newCompaniesQueueSource,
                           @Value("${external_api.twelve_data.chunk_size}") int chunkSize) {
        this.self = self;
        this.chunkSize = chunkSize;
        this.companySource = companySource;
        this.dailyStockSource = dailyStockSource;
        this.markovChainImporter = markovChainImporter;
        this.stockReturnCalculator = stockReturnCalculator;
        this.dailyStockReturnSource = dailyStockReturnSource;
        this.importDailyStockSource = importDailyStockSource;
        this.newCompaniesQueueSource = newCompaniesQueueSource;
    }

    @Override
    @Scheduled(fixedDelay = 1, timeUnit = MINUTES)
    public void startImport() {
        Map<Integer, SaveCompanyCmd> companiesToSave = newCompaniesQueueSource.poll(BATCH_SIZE);
        if (companiesToSave.isEmpty()) {
            return;
        }

        logger.info("Start new company import.");

        int counter = 0;
        boolean dataWasImporter = false;
        Map<Integer, SaveCompanyCmd> chunk = new HashMap<>();

        for (var entry : companiesToSave.entrySet()) {
            counter += 1;
            chunk.put(entry.getKey(), entry.getValue());

            if (chunk.size() >= chunkSize || counter == companiesToSave.size()) {
                ImportStatus importStatus = importWithHandlingStatus(chunk);
                if (importStatus == SUCCESS) {
                    dataWasImporter = true;
                }

                chunk.clear();
            }
        }

        logger.info("New company import completed.");

        if (dataWasImporter) {
            markovChainImporter.startImport();
        }
    }

    private ImportStatus importWithHandlingStatus(Map<Integer, SaveCompanyCmd> chunk) {
        ImportStatus importStatus = null;
        for (int i = 0; i < 10; i++) {
            importStatus = self.importCompanies(chunk);

            if (importStatus != ERROR) {
                sleep(12_000); // Sleep for 12 seconds to avoid hitting API rate limits before processing the next chunk.
                break;
            } else {
                sleep(20_000); // Sleep for 20 seconds before retry.
            }
        }

        newCompaniesQueueSource.delete(chunk.keySet());

        return importStatus;
    }

    @Transactional
    public ImportStatus importCompanies(Map<Integer, SaveCompanyCmd> newCompanies) {
        List<Company> companies = companySource.saveCompanies(newCompanies.values());

        ImportStatus importStatus;
        try {
            var dailyStocks = importDailyStockSource.getDailyStock(companies, START_YEAR);

            if (!dailyStocks.isEmpty()) {
                dailyStockSource.saveDailyStock(dailyStocks);

                var dailyStockReturns = stockReturnCalculator.calculate(dailyStocks);
                dailyStockReturnSource.saveDailyReturnStock(dailyStockReturns);

                logger.info("Batch {} companies daily stock returns imported.", dailyStocks.size());

                importStatus = SUCCESS;
            } else {
                importStatus = EMPTY;
            }

        } catch (ExternalClientException e) {
            logger.error("Failed to import daily stock data for new companies. Will retry later.");
            importStatus = ERROR;
        }

        return importStatus;
    }
}