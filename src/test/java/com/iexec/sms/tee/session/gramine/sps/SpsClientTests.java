package com.iexec.sms.tee.session.gramine.sps;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import static org.mockito.Mockito.*;

class SpsClientTests {
    private static final String SPS_URL = "localhost:8080";
    private static final String SPS_LOGIN = "admin";
    private static final String SPS_PASSWORD = "admin";

    @Captor
    ArgumentCaptor<String> sessionPostUrlCaptor;
    @Captor
    ArgumentCaptor<HttpEntity<byte[]>> sessionPostRequestCaptor;

    @Mock
    private SpsConfiguration spsConfiguration;

    @InjectMocks
    @Spy
    private SpsClient spsClient;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void shouldPostSession() {
        byte[] session = new byte[10];

        final RestTemplate restTemplate = mock(RestTemplate.class);
        when(spsClient.createRestTemplate()).thenReturn(restTemplate);
        when(restTemplate.postForEntity(any(), any(), eq(String.class))).thenReturn(ResponseEntity.ok("OK"));

        when(spsConfiguration.getWebUrl()).thenReturn(SPS_URL);
        when(spsConfiguration.getWebLogin()).thenReturn(SPS_LOGIN);
        when(spsConfiguration.getWebPassword()).thenReturn(SPS_PASSWORD);

        spsClient.postSession(session);

        verify(restTemplate).postForEntity(sessionPostUrlCaptor.capture(), sessionPostRequestCaptor.capture(), eq(String.class));

        Assertions.assertEquals(SPS_URL + "/api/session", sessionPostUrlCaptor.getValue());
        Assertions.assertEquals("Basic YWRtaW46YWRtaW4=", sessionPostRequestCaptor.getValue()
                .getHeaders()
                .get(HttpHeaders.AUTHORIZATION)
                .get(0));
    }
}