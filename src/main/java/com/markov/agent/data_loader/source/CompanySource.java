package com.markov.agent.data_loader.source;

import com.markov.agent.data_loader.value_object.Company;
import com.markov.agent.data_loader.value_object.SaveCompanyCmd;

import java.util.Collection;
import java.util.List;

public interface CompanySource {

    Integer getFirstCompanyIdWithDailyStock();

    List<Company> saveCompanies(Collection<SaveCompanyCmd> companies);

    List<Company> getCompaniesWithDailyStock(Integer start, int batchSize);
}