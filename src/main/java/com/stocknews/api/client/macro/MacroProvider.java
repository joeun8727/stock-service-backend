package com.stocknews.api.client.macro;

public interface MacroProvider {

    RawMacroData fetchSeries(String seriesId);
}
