package com.markov.agent.domain.source;

import com.markov.agent.domain.value_object.MarkovChain;
import com.markov.agent.domain.value_object.MarkovChainPrediction;
import com.markov.agent.domain.value_object.MathIndex;
import com.markov.agent.domain.value_object.State;
import com.markov.agent.domain.value_object.DailyStock;

import java.time.LocalDateTime;

public interface MarkovChainSource {

    MarkovChain getMarkovChain();

    void updateMathIndex(LocalDateTime from, MathIndex mathIndex);

    void updateMarkovChain(State from, State to, LocalDateTime fromDate);

    MarkovChainPrediction getMarkovChainPrediction(DailyStock dailyStock);
}