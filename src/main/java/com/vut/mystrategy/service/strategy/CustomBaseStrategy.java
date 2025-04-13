package com.vut.mystrategy.service.strategy;

import com.vut.mystrategy.service.strategy.rule.LoggingRule;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.util.CollectionUtils;
import org.ta4j.core.Rule;
import org.ta4j.core.Strategy;
import org.ta4j.core.TradingRecord;

import java.util.List;

@Slf4j
public class CustomBaseStrategy implements Strategy {
    private final String className;
    private final String name;
    private final List<LoggingRule> entryRules;
    private final List<LoggingRule> exitRules;
    private int unstableBars;
    private final double entryScoreThreshold;
    private final double exitScoreThreshold;

    public CustomBaseStrategy(List<LoggingRule> entryRules, double entryScoreThreshold,
                              List<LoggingRule> exitRules, double exitScoreThreshold) {
        this((String)null, entryRules, entryScoreThreshold,
                exitRules, exitScoreThreshold, 0);
    }

    public CustomBaseStrategy(List<LoggingRule> entryRules, double entryScoreThreshold,
                              List<LoggingRule> exitRules, double exitScoreThreshold, int unstableBars) {
        this((String)null, entryRules, entryScoreThreshold,
                exitRules, exitScoreThreshold, unstableBars);
    }

    public CustomBaseStrategy(String name, List<LoggingRule> entryRules, double entryScoreThreshold,
                              List<LoggingRule> exitRules, double exitScoreThreshold) {
        this(name, entryRules, entryScoreThreshold,
                exitRules, exitScoreThreshold, 0);
    }

    public CustomBaseStrategy(String name, List<LoggingRule> entryRules, double entryScoreThreshold,
                              List<LoggingRule> exitRules, double exitScoreThreshold, int unstableBars) {
        this.className = this.getClass().getSimpleName();
        if (!CollectionUtils.isEmpty(entryRules) && !CollectionUtils.isEmpty(exitRules)) {
            if (unstableBars < 0) {
                throw new IllegalArgumentException("Unstable bars must be >= 0");
            } else {
                this.name = name;
                this.entryRules = entryRules;
                this.entryScoreThreshold = entryScoreThreshold;
                this.exitScoreThreshold = exitScoreThreshold;
                this.exitRules = exitRules;
                this.unstableBars = unstableBars;
            }
        } else {
            throw new IllegalArgumentException("Rules cannot be null");
        }
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public Rule getEntryRule() {
        return null;
    }

    @Override
    public Rule getExitRule() {
        return null;
    }

    @Override
    public Strategy and(Strategy strategy) {
        return null;
    }

    @Override
    public Strategy or(Strategy strategy) {
        return null;
    }

    @Override
    public Strategy and(String s, Strategy strategy, int i) {
        return null;
    }

    @Override
    public Strategy or(String s, Strategy strategy, int i) {
        return null;
    }

    @Override
    public Strategy opposite() {
        return null;
    }

    @Override
    public void setUnstableBars(int i) {

    }

    @Override
    public int getUnstableBars() {
        return 0;
    }

    @Override
    public boolean isUnstableAt(int i) {
        return false;
    }

    @Override
    public boolean shouldOperate(int index, TradingRecord tradingRecord) {
        return Strategy.super.shouldOperate(index, tradingRecord);
    }

    @Override
    public boolean shouldEnter(int index) {
        double score = calculateEntryScore(index);
        log.info("Index: {} - Entry score: {} - EntryScoreThreshold: {}", index, score, entryScoreThreshold);
        return score >= entryScoreThreshold;
    }

    @Override
    public boolean shouldEnter(int index, TradingRecord tradingRecord) {
        throw new NotImplementedException("Not implemented yet");
    }

    @Override
    public boolean shouldExit(int index) {
        double score = calculateExitScore(index);
        log.info("Index: {} - Exit score: {} - ExitScoreThreshold: {}", index, score, exitScoreThreshold);
        return score >= exitScoreThreshold;
    }

    @Override
    public boolean shouldExit(int index, TradingRecord tradingRecord) {
        throw new NotImplementedException("Not implemented yet");
    }

    private double calculateEntryScore(int index) {
        double score = 0.0;
        for (LoggingRule rule : entryRules) {
            if(rule.isSatisfied(index)) {
                score += 1 * rule.getWeightNumber();
            }
        }
        return score;
    }

    private double calculateExitScore(int index) {
        double score = 0.0;
        for (LoggingRule rule : exitRules) {
            if(rule.isSatisfied(index)) {
                score += 1 * rule.getWeightNumber();
            }
        }
        return score;
    }
}
