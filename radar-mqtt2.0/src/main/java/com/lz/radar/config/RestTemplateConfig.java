package com.lz.radar.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;

/**
 * RestTemplateConfig: HTTP 请求工具
 */
@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate getRestTemplate(){
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        // ConnectTimeout只有在网络正常的情况下才有效，因此两个一般都设置
        requestFactory.setConnectTimeout(5000);	//建立连接的超时时间  5秒
        requestFactory.setReadTimeout(5000);	//传递数据的超时时间（在网络抖动的情况下，这个参数很有用）
        
        HttpHeaders headers = new HttpHeaders();
        headers.add("user-agent","Mozilla/5.0(Windows NT 10.0; Win64; x64) AppleWebKit/537.36" +
                "(KHTML, like Gecko) Chrome/54.0.2840.99 Safari/537.36");
        HttpEntity<Resource> httpEntity = new HttpEntity<>(headers);
        RestTemplate restTemplate = new RestTemplate(requestFactory);
        return restTemplate;
    }
}
