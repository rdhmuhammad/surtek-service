package com.sabarceria.surtek.restexecutor;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;

@Component
public class RestExecutor {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    // POST METHOD
    public <T> ResponseWrapper<T> post(String uri, Object request, Class<T> type) throws IOException {
        return exchange(uri, HttpMethod.POST, request, type);
    }

    public <T> ResponseWrapper<T> post(String uri, HttpEntity<?> request, Class<T> type) throws IOException {
        return exchange(uri, HttpMethod.POST, request, type);
    }

    public ResponseWrapper<Object> post(String uri, HttpEntity<?> request) throws IOException {
        return exchange(uri, HttpMethod.POST, request, Object.class);
    }

    public ResponseWrapper<Object> post(String uri, Object request) throws IOException {
        return exchange(uri, HttpMethod.POST, request, Object.class);
    }

    // PUT METHOD
    public <T> ResponseWrapper<T> put(String uri, Object request, Class<T> type) throws IOException {
        return exchange(uri, HttpMethod.PUT, request, type);
    }

    public <T> ResponseWrapper<T> put(String uri, HttpEntity<?> request, Class<T> type) throws IOException {
        return exchange(uri, HttpMethod.PUT, request, type);
    }

    public ResponseWrapper<Object> put(String uri, HttpEntity<?> request) throws IOException {
        return exchange(uri, HttpMethod.PUT, request, Object.class);
    }

    public ResponseWrapper<Object> put(String uri, Object request) throws IOException {
        return exchange(uri, HttpMethod.PUT, request, Object.class);
    }

    // GET METHOD
    public <T> ResponseWrapper<T> get(String uri, Class<T> type) throws IOException {
        return exchange(uri, HttpMethod.GET, type);
    }

    public <T> ResponseWrapper<T> get(String uri, HttpEntity<?> request, Class<T> type) throws IOException {
        return exchange(uri, HttpMethod.GET, request, type);
    }

    public ResponseWrapper<Object> get(String uri, HttpEntity<?> request) throws IOException {
        return exchange(uri, HttpMethod.GET, request, Object.class);
    }

    public ResponseWrapper<Object> get(String uri) throws IOException {
        return exchange(uri, HttpMethod.GET, Object.class);
    }


    // DELETE METHOD
    public <T> ResponseWrapper<T> delete(String uri, Class<T> type) throws IOException {
        return exchange(uri, HttpMethod.DELETE, type);
    }

    public <T> ResponseWrapper<T> delete(String uri, HttpEntity<?> request, Class<T> type) throws IOException {
        return exchange(uri, HttpMethod.DELETE, request, type);
    }

    public ResponseWrapper<Object> delete(String uri, HttpEntity<?> request) throws IOException {
        return exchange(uri, HttpMethod.DELETE, request, Object.class);
    }

    public ResponseWrapper<Object> delete(String uri) throws IOException {
        return exchange(uri, HttpMethod.DELETE, Object.class);
    }

    private  <T> ResponseWrapper<T> exchange(String uri, HttpMethod method, HttpEntity<?> request, Class<T> type) throws IOException {
        try {
            ResponseEntity<T> exchange = restTemplate.exchange(uri, method, request, ParameterizedTypeReference.forType(type));
            return new ResponseWrapper<>(exchange.getBody(), exchange.getStatusCode(), exchange.getStatusCode().getReasonPhrase());
        } catch (HttpClientErrorException ex) {
            TypeReference<T> tTypeReference = new TypeReference<>() {
            };
            T resData = objectMapper.readValue(ex.getResponseBodyAsString(), tTypeReference);
            return new ResponseWrapper<>(resData, ex.getStatusCode(), ex.getMessage());
        }
    }

    private  <T> ResponseWrapper<T> exchange(String uri, HttpMethod method, Class<T> type) throws IOException {
        try {
            ResponseEntity<T> exchange = restTemplate.exchange(uri, method, new HttpEntity<>(null), ParameterizedTypeReference.forType(type));
            return new ResponseWrapper<>(exchange.getBody(), exchange.getStatusCode(), exchange.getStatusCode().getReasonPhrase());
        } catch (HttpClientErrorException ex) {
            TypeReference<T> tTypeReference = new TypeReference<>() {
            };
            T resData = objectMapper.readValue(ex.getResponseBodyAsString(), tTypeReference);
            return new ResponseWrapper<>(resData, ex.getStatusCode(), ex.getMessage());
        }
    }

    private  <T> ResponseWrapper<T> exchange(String uri, HttpMethod method, Object request, Class<T> type) throws IOException {
        try {
            ResponseEntity<T> exchange = restTemplate.exchange(uri, method, new HttpEntity<>(request), ParameterizedTypeReference.forType(type));
            return new ResponseWrapper<>(exchange.getBody(), exchange.getStatusCode(), exchange.getStatusCode().getReasonPhrase());
        } catch (HttpClientErrorException ex) {
            TypeReference<T> tTypeReference = new TypeReference<>() {
            };
            T resData = objectMapper.readValue(ex.getResponseBodyAsString(), tTypeReference);
            return new ResponseWrapper<>(resData, ex.getStatusCode(), ex.getMessage());
        }
    }
}
