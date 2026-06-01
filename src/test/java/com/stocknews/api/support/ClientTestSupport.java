package com.stocknews.api.support;

import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.web.client.RestClient;

import java.io.IOException;

public abstract class ClientTestSupport {

    protected MockWebServer mockWebServer;
    protected RestClient testRestClient;

    @BeforeEach
    void setUpServer() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        testRestClient = RestClient.builder()
                .baseUrl(mockWebServer.url("/").toString())
                .build();
    }

    @AfterEach
    void tearDownServer() throws IOException {
        mockWebServer.shutdown();
    }
}
