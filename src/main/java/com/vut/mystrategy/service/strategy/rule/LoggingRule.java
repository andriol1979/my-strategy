package com.vut.mystrategy.service.strategy.rule;

import com.vut.mystrategy.helper.LogMessage;
import lombok.Getter;
import org.slf4j.Logger;
import org.ta4j.core.Rule;
import org.ta4j.core.TradingRecord;
import org.ta4j.core.rules.AbstractRule;

public class LoggingRule extends AbstractRule {
    private final Rule rule;
    private final String name;
    private final Logger log;
    @Getter
    private final double weightNumber;

    public LoggingRule(Rule rule, String name, Logger log) {
        this.rule = rule;
        this.name = name;
        this.log = log;
        this.weightNumber = 1.0;
    }

    public LoggingRule(Rule rule, String name, Logger log, double weightNumber) {
        this.rule = rule;
        this.name = name;
        this.log = log;
        this.weightNumber = weightNumber;
    }

    @Override
    public boolean isSatisfied(int index, TradingRecord tradingRecord) {
        boolean satisfied = rule.isSatisfied(index, tradingRecord);
        if (satisfied) {
            LogMessage.printRuleMatchedMessage(log, index,  name);
        }
        return satisfied;
    }
}
