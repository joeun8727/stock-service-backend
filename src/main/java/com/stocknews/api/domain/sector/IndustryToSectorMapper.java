package com.stocknews.api.domain.sector;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

/**
 * Finnhub profile2.finnhubIndustry → 내부 Sector.code 매핑.
 * 매핑 없는 산업은 Optional.empty() 반환 → 수집 제외 + 로그.
 */
@Component
public class IndustryToSectorMapper {

    private static final Map<String, String> MAP = Map.ofEntries(
            // 반도체
            Map.entry("Semiconductors",                               "SEMICONDUCTOR"),
            Map.entry("Semiconductor Equipment & Materials",          "SEMICONDUCTOR"),
            Map.entry("Semiconductor Equipment",                      "SEMICONDUCTOR"),
            // 우주/항공
            Map.entry("Aerospace & Defense",                          "AEROSPACE"),
            Map.entry("Space",                                        "AEROSPACE"),
            // AI/소프트웨어
            Map.entry("Software",                                     "AI_SOFTWARE"),
            Map.entry("Software-Application",                         "AI_SOFTWARE"),
            Map.entry("Software-Infrastructure",                      "AI_SOFTWARE"),
            Map.entry("Technology",                                   "AI_SOFTWARE"),
            Map.entry("Internet Content & Information",               "AI_SOFTWARE"),
            Map.entry("Internet & Catalog Retail",                    "AI_SOFTWARE"),
            Map.entry("IT Services",                                  "AI_SOFTWARE"),
            Map.entry("Data Processing & Outsourced Services",        "AI_SOFTWARE"),
            // 전기차/배터리
            Map.entry("Electric Vehicles",                            "EV_BATTERY"),
            Map.entry("Automobiles",                                  "EV_BATTERY"),
            Map.entry("Auto Manufacturers",                           "EV_BATTERY"),
            Map.entry("Lithium & Battery Materials",                  "EV_BATTERY"),
            Map.entry("Battery Technology",                           "EV_BATTERY"),
            Map.entry("EV Charging",                                  "EV_BATTERY"),
            // 헬스케어/바이오
            Map.entry("Biotechnology",                                "HEALTHCARE_BIO"),
            Map.entry("Pharmaceuticals",                              "HEALTHCARE_BIO"),
            Map.entry("Drug Manufacturers-General",                   "HEALTHCARE_BIO"),
            Map.entry("Drug Manufacturers-Specialty & Generic",       "HEALTHCARE_BIO"),
            Map.entry("Medical Devices",                              "HEALTHCARE_BIO"),
            Map.entry("Medical Instruments & Supplies",               "HEALTHCARE_BIO"),
            Map.entry("Health Care Equipment & Supplies",             "HEALTHCARE_BIO"),
            Map.entry("Health Care Providers & Services",             "HEALTHCARE_BIO"),
            // 에너지
            Map.entry("Oil & Gas Integrated",                         "ENERGY"),
            Map.entry("Oil & Gas E&P",                                "ENERGY"),
            Map.entry("Oil & Gas Refining & Marketing",               "ENERGY"),
            Map.entry("Oil & Gas Equipment & Services",               "ENERGY"),
            Map.entry("Oil, Gas & Consumable Fuels",                  "ENERGY"),
            Map.entry("Utilities-Renewable",                          "ENERGY"),
            Map.entry("Renewable Energy",                             "ENERGY"),
            Map.entry("Solar",                                        "ENERGY"),
            Map.entry("Electric Utilities",                           "ENERGY"),
            // 금융
            Map.entry("Banks",                                        "FINANCE"),
            Map.entry("Banks-Diversified",                            "FINANCE"),
            Map.entry("Banks-Regional",                               "FINANCE"),
            Map.entry("Capital Markets",                              "FINANCE"),
            Map.entry("Investment Banking & Brokerage",               "FINANCE"),
            Map.entry("Credit Services",                              "FINANCE"),
            Map.entry("Financial Data & Stock Exchanges",             "FINANCE"),
            Map.entry("Asset Management",                             "FINANCE"),
            Map.entry("Insurance-Diversified",                        "FINANCE"),
            Map.entry("Cryptocurrency",                               "FINANCE"),
            Map.entry("Fintech",                                      "FINANCE"),
            // 소비재
            Map.entry("Consumer Discretionary",                       "CONSUMER_GOODS"),
            Map.entry("Consumer Defensive",                           "CONSUMER_GOODS"),
            Map.entry("Consumer Staples",                             "CONSUMER_GOODS"),
            Map.entry("Specialty Retail",                             "CONSUMER_GOODS"),
            Map.entry("Discount Stores",                              "CONSUMER_GOODS"),
            Map.entry("Home Improvement Retail",                      "CONSUMER_GOODS"),
            Map.entry("Restaurants",                                  "CONSUMER_GOODS"),
            Map.entry("Beverages-Non-Alcoholic",                      "CONSUMER_GOODS"),
            Map.entry("Apparel Retail",                               "CONSUMER_GOODS"),
            Map.entry("Apparel Manufacturing",                        "CONSUMER_GOODS"),
            Map.entry("Footwear & Accessories",                       "CONSUMER_GOODS"),
            Map.entry("Packaged Foods",                               "CONSUMER_GOODS"),
            // 로봇/자동화
            Map.entry("Medical Robotics",                             "ROBOTICS"),
            Map.entry("Industrial Automation",                        "ROBOTICS"),
            Map.entry("Industrial Machinery",                         "ROBOTICS"),
            Map.entry("Scientific & Technical Instruments",           "ROBOTICS"),
            Map.entry("Electronic Components",                        "ROBOTICS"),
            Map.entry("Electronic Equipment & Instruments",           "ROBOTICS"),
            Map.entry("Connected Devices & AI",                       "ROBOTICS"),
            Map.entry("Warehouse Robotics",                           "ROBOTICS"),
            Map.entry("Collaborative Robots",                         "ROBOTICS"),
            Map.entry("Machine Vision",                               "ROBOTICS"),
            Map.entry("Autonomous Driving AI",                        "ROBOTICS"),
            Map.entry("eVTOL / Air Mobility",                         "ROBOTICS"),
            // 사이버보안
            Map.entry("Cybersecurity",                                "CYBERSECURITY"),
            Map.entry("Security Software",                            "CYBERSECURITY"),
            Map.entry("Network Security",                             "CYBERSECURITY"),
            Map.entry("Endpoint Security",                            "CYBERSECURITY"),
            Map.entry("Cloud Security",                               "CYBERSECURITY"),
            Map.entry("Identity & Access Management",                 "CYBERSECURITY"),
            Map.entry("Data Security",                                "CYBERSECURITY"),
            Map.entry("Vulnerability Management",                     "CYBERSECURITY"),
            Map.entry("Security Analytics",                           "CYBERSECURITY")
    );

    public Optional<String> toSectorCode(String finnhubIndustry) {
        if (finnhubIndustry == null || finnhubIndustry.isBlank()) return Optional.empty();
        return Optional.ofNullable(MAP.get(finnhubIndustry));
    }
}
