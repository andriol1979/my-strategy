package com.vut.mystrategy.service.strategy.rule;

import com.vut.mystrategy.helper.LogMessage;
import lombok.Getter;
import org.slf4j.Logger;
import org.ta4j.core.Rule;
import org.ta4j.core.TradingRecord;
import org.ta4j.core.rules.AbstractRule;

@Getter
public class LoggingRule extends AbstractRule {
    private final Rule rule;
    private final String name;
    private final String debugMessage;
    private final Logger log;

    public LoggingRule(Rule rule, String name, Logger log, String debugMessage) {
        this.rule = rule;
        this.name = name;
        this.log = log;
        this.debugMessage = debugMessage;
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
