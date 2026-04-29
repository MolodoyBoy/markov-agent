package com.markov.agent.data_loader.source;

import com.markov.agent.data_loader.value_object.SaveCompanyCmd;

import java.util.Collection;
import java.util.Map;

public interface NewCompaniesQueueSource {

    void delete(Collection<Integer> ids);

    Map<Integer, SaveCompanyCmd> poll(int limit);

    void save(Collection<SaveCompanyCmd> companyCmds);
}
