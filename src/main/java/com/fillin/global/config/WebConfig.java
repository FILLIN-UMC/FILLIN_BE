package com.fillin.global.config;


import com.fillin.global.security.jwt.AuthUserArgumentResolver;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.ArrayList;
import java.util.List;


@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AuthUserArgumentResolver userIdArgumentResolver;

    public WebConfig(AuthUserArgumentResolver userIdArgumentResolver) {
        this.userIdArgumentResolver = userIdArgumentResolver;
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addRedirectViewController("/swagger", "/swagger-ui/index.html");
        registry.addRedirectViewController("/swagger/", "/swagger-ui/index.html");
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(userIdArgumentResolver);
    }

    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        for (HttpMessageConverter<?> converter : converters) {
            if (converter instanceof MappingJackson2HttpMessageConverter jacksonConverter) {
                // 기존에 지원하는 타입 리스트를 가져와서 application/octet-stream을 추가함
                List<MediaType> supportedMediaTypes = new ArrayList<>(jacksonConverter.getSupportedMediaTypes());
                supportedMediaTypes.add(MediaType.APPLICATION_OCTET_STREAM);
                jacksonConverter.setSupportedMediaTypes(supportedMediaTypes);
            }
        }
    }
}