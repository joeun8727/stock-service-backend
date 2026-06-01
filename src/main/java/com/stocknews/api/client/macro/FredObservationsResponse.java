package com.stocknews.api.client.macro;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FredObservationsResponse(List<FredObservation> observations) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record FredObservation(String date, String value) {}
}
