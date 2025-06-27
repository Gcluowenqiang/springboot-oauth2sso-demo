package com.example.oauth2sso.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.HttpURLConnection;

/**
 * HTTP客户端配置类
 * 优化OAuth2相关的网络请求超时和连接设置
 * 防止GitHub API调用时出现连接超时问题
 * 
 * @author Luowenqiang
 * @version 1.0.0
 * @since 2025-06-27
 */
@Configuration
public class HttpClientConfig {
    
    /**
     * 配置优化的RestTemplate
     * 设置合理的超时时间和连接参数
     * 
     * @return 配置优化的RestTemplate实例
     */
    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setRequestFactory(clientHttpRequestFactory());
        return restTemplate;
    }
    
    /**
     * 配置HTTP客户端请求工厂
     * 设置连接超时、读取超时等参数
     * 
     * @return 配置的ClientHttpRequestFactory
     */
    @Bean
    public ClientHttpRequestFactory clientHttpRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory() {
            @Override
            protected void prepareConnection(HttpURLConnection connection, String httpMethod) throws IOException {
                super.prepareConnection(connection, httpMethod);
                
                // 设置连接属性以优化OAuth2相关请求
                connection.setUseCaches(false);
                connection.setDoInput(true);
                connection.setDoOutput(true);
                
                // 设置请求头以避免某些网络问题
                connection.setRequestProperty("Connection", "close");
                connection.setRequestProperty("Accept-Encoding", "gzip, deflate");
                connection.setRequestProperty("Cache-Control", "no-cache");
            }
        };
        
        // 连接超时时间：15秒
        factory.setConnectTimeout(15000);
        
        // 读取超时时间：30秒
        factory.setReadTimeout(30000);
        
        return factory;
    }
} 