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

    private static double findBchRate(CurrencyRate[] rates) {
        double bchRate = 0;
        for (CurrencyRate rate : rates) {
            if ("BCH".equals(rate.code)) {
                bchRate = rate.rate;
                break;
            }
        }
        return bchRate;
    }

    public static Map<String, CurrencyRate> convertFromBtcToBch(CurrencyRate[] btcRates, Map<String, String> tickerToSymbol) {
        Map<String, CurrencyRate> tickerToRate = new TreeMap<>();
        double bchRate = findBchRate(btcRates);
        for (CurrencyRate cr : btcRates) {
            if (!cr.name.toLowerCase().contains("coin")) {
                BigDecimal bchValue = new BigDecimal(cr.rate * bchRate).setScale(2, BigDecimal.ROUND_CEILING);
                double price = bchValue.doubleValue();
                String ticker = cr.code;
                String symbol = tickerToSymbol.get(ticker);
                CurrencyRate crBch = new CurrencyRate(ticker, cr.name, price, symbol);
                tickerToRate.put(ticker, crBch);
                // System.out.println(tickerToSymbol.get(ticker) + " " + currency.name + " => " + bchValue.toPlainString());
            }
        }
        return tickerToRate;
    }

    @Override
    public String toString() {
        String value = symbol == null ? "" : symbol + " - ";
        return code + " - " + value + name;
    }
}
