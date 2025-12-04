package com.boot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.filter.CharacterEncodingFilter;

@EnableCaching
@SpringBootApplication
public class BootSearchProjectApplication {

	public static void main(String[] args) {
		SpringApplication.run(BootSearchProjectApplication.class, args);
	}
	/**
     * ★★★ 한글 깨짐 방지 UTF-8 인코딩 필터 ★★★
     * - 모든 요청(Request) / 응답(Response)을 강제로 UTF-8 로 변환
     * - JSON 파싱 시 깨지는 문제 완전 해결
     */
    @Bean
    public FilterRegistrationBean<CharacterEncodingFilter> encodingFilter() {
        CharacterEncodingFilter filter = new CharacterEncodingFilter();
        filter.setEncoding("UTF-8");
        filter.setForceEncoding(true);

        FilterRegistrationBean<CharacterEncodingFilter> bean =
                new FilterRegistrationBean<>(filter);

        bean.addUrlPatterns("/*");
        return bean;
    }
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

}
