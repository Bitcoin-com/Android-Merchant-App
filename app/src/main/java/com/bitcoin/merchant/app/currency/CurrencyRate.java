package com.bitcoin.merchant.app.currency;

import java.math.BigDecimal;
import java.util.Map;
import java.util.TreeMap;

public class CurrencyRate {
    public String code;
    public String name;
    public Double rate;
    public String symbol; // not in json

    public CurrencyRate(String code, String name, Double rate, String symbol) {
        this.code = code;
        this.name = name;
        this.rate = rate;
        this.symbol = symbol;
    }

    public static Map<String, CurrencyRate> convertToMap(CurrencyRate[] bchRates, Map<String, String> tickerToSymbol) {
        Map<String, CurrencyRate> tickerToRate = new TreeMap<>();
        for (CurrencyRate cr : bchRates) {
            String symbol = tickerToSymbol.get(cr.code);
            CurrencyRate crWithSymbol = new CurrencyRate(cr.code, cr.name, cr.rate, symbol);
            tickerToRate.put(cr.code, crWithSymbol);
        }
        return tickerToRate;
    }

    @Override
    public String toString() {
        String value = symbol == null ? "" : symbol + " - ";
        return code + " - " + value + name;
    }
}
