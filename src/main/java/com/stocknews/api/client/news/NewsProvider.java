package com.stocknews.api.client.news;

import java.time.LocalDate;
import java.util.List;

public interface NewsProvider {

    List<RawNews> fetchCompanyNews(String ticker, LocalDate from, LocalDate to);
}
