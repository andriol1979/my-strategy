package com.vut.mystrategy.service.strategy;

import com.vut.mystrategy.helper.LogMessage;
import com.vut.mystrategy.model.SymbolConfig;
import com.vut.mystrategy.service.strategy.rule.LoggingRule;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.ta4j.core.BarSeries;
import org.ta4j.core.Rule;
import org.ta4j.core.Strategy;

import java.util.List;
import java.util.Map;

@NoArgsConstructor
public abstract class MyStrategyBase {
    public abstract Strategy buildLongStrategy(BarSeries barSeries, SymbolConfig symbolConfig);
    public abstract Strategy buildShortStrategy(BarSeries barSeries, SymbolConfig symbolConfig);

    protected Rule combineRules(Logger log, int barIndex, Map<String, List<LoggingRule>> ruleMap) {
        LogMessage.printCheckRulesMatchMessage(log, barIndex, ruleMap);
        String key = ruleMap.keySet().iterator().next();
        List<LoggingRule> rules = ruleMap.get(key);
        Rule rule = rules.get(0);
        for (int index = 1; index < rules.size(); index++) {
            Rule loggingRule = rules.get(index);
            rule = rule.and(loggingRule);
        }
        return rule;
    }
}
