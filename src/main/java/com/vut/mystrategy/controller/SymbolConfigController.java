package com.vut.mystrategy.controller;

import com.vut.mystrategy.helper.ApiUrlConstant;
import com.vut.mystrategy.helper.Constant;
import com.vut.mystrategy.component.binance.starter.SymbolConfigManager;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping(ApiUrlConstant.REAL_URL + "/symbol-configs")
@Validated
public class SymbolConfigController {

    private final SymbolConfigManager symbolConfigManager;

    @Autowired
    public SymbolConfigController(SymbolConfigManager symbolConfigManager) {
        this.symbolConfigManager = symbolConfigManager;
    }

    @GetMapping()
    public ResponseEntity<?> getAllSymbolConfigs(@RequestParam(value = "false", required = false) boolean active) {
        if(active) {
            return ResponseEntity.ok(symbolConfigManager.getActiveSymbolConfigsList());
        }
        return ResponseEntity.ok(symbolConfigManager.gettAllSymbolConfigsList());
    }

    @GetMapping("/{symbol}")
    public ResponseEntity<?> getSymbolConfigBySymbol(@RequestParam String exchangeName, @PathVariable String symbol) {
        if(StringUtils.isEmpty(exchangeName)) {
            exchangeName = Constant.EXCHANGE_NAME_BINANCE;
        }
        return ResponseEntity.ok(symbolConfigManager.getSymbolConfig(exchangeName, symbol));
    }

    @GetMapping("/by-exchange")
    public ResponseEntity<?> getSymbolConfigsByExchangeName(@RequestParam(value = "binance", required = false) String exchangeName) {
        return ResponseEntity.ok(symbolConfigManager.getActiveSymbolConfigsListByExchangeName(exchangeName));
    }
}
