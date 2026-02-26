package com.bitcoin.merchant.app.currency;

import java.util.Currency;
import java.util.Map;
import java.util.TreeMap;

public class ExchangeRate {
    public String ccy;
    public Double rate;
    public String symbol; // not in json
    public String name; // not in json

    public ExchangeRate(String ccy, Double rate, String symbol, String name) {
        this.ccy = ccy.toUpperCase();
        this.rate = rate;
        this.symbol = symbol;
        this.name = name;
    }

    public static Map<String, ExchangeRate> convertToMap(ExchangeRate[] bchRates, Map<String, String> tickerToSymbol) {
        Map<String, ExchangeRate> tickerToRate = new TreeMap<>();
        for (ExchangeRate er : bchRates) {
            String ccy = er.ccy.toUpperCase();
            String symbol = tickerToSymbol.get(ccy);
            String name = Currency.getInstance(ccy).getDisplayName();
            ExchangeRate erWithSymbol = new ExchangeRate(ccy, er.rate, symbol, name);
            tickerToRate.put(ccy, erWithSymbol);
        }
        return tickerToRate;
    }

    @Override
    public String toString() {
        String value = symbol == null ? "" : symbol + " - ";
        return ccy + " - " + value + name;
    }
}
