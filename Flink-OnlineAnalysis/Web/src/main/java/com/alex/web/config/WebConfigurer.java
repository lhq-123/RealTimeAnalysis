package com.alex.web.config;

import com.alex.web.util.Constants;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.format.Formatter;
import org.springframework.format.FormatterRegistry;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.reactive.config.CorsRegistration;
import org.springframework.web.reactive.config.CorsRegistry;
import org.springframework.web.reactive.config.WebFluxConfigurer;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;

/**
 * 配置类：处理跨域
 */
@Configuration
public class WebConfigurer implements WebFluxConfigurer {
    /**
    * 1.webflux已经没有了Interceptor的概念，但是可以通过WebFilter的方式实现
    */
    @Override
    public void configureHttpMessageCodecs(ServerCodecConfigurer configurer) {

    }

    /**
     * 建立redis模板对象
     * @param factory
     * @return
     */
    @Bean(name = {"redisTemplate", "stringRedisTemplate"})
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory factory) {
        StringRedisTemplate redisTemplate = new StringRedisTemplate();
        redisTemplate.setConnectionFactory(factory);
        return redisTemplate;
    }

    /**
     *  fastjson转换格式化加工
     * @param registry
     */
    @Override
    public void addFormatters(FormatterRegistry registry) {
        Formatter<Date> dateFormatter = new Formatter<Date>() {
            SimpleDateFormat sdf = new SimpleDateFormat(Constants.YYYY_MM_DD_HH_MM_SS);
            @Override
            public String print(Date object, Locale locale) {
                return sdf.format(object);
            }
            @Override
            public Date parse(String text, Locale locale) throws ParseException {
                return sdf.parse(text);
            }
        };
        Formatter<Timestamp> timestampFormatter = new Formatter<Timestamp>() {
            SimpleDateFormat sdf = new SimpleDateFormat(Constants.YYYY_MM_DD_HH_MM_SS);
            @Override
            public String print(Timestamp object, Locale locale) {
                return sdf.format(new Date(object.getTime()));
            }
            @Override
            public Timestamp parse(String text, Locale locale) throws ParseException {
                return new Timestamp(sdf.parse(text).getTime());
            }
        };

        Formatter<LocalDate> localDateFormatter = new Formatter<LocalDate>() {
            @Override
            public String print(LocalDate object, Locale locale) {
                return object.format(DateTimeFormatter.ofPattern(Constants.YYYY_MM_DD_HH_MM_SS));
            }
            @Override
            public LocalDate parse(String text, Locale locale) throws ParseException {
                return LocalDate.parse(text ,DateTimeFormatter.ofPattern(Constants.YYYY_MM_DD_HH_MM_SS));
            }
        };

        Formatter<LocalDateTime> localDateTimeFormatter = new Formatter<LocalDateTime>() {
            @Override
            public String print(LocalDateTime object, Locale locale) {
                return object.format(DateTimeFormatter.ofPattern(Constants.YYYY_MM_DD_HH_MM_SS));
            }
            @Override
            public LocalDateTime parse(String text, Locale locale) throws ParseException {
                return LocalDateTime.parse(text ,DateTimeFormatter.ofPattern(Constants.YYYY_MM_DD_HH_MM_SS));
            }
        };

        //对时间，进行格式化
        registry.addFormatterForFieldType(Date.class, dateFormatter);
        registry.addFormatterForFieldType(Timestamp.class, timestampFormatter);
        registry.addFormatterForFieldType(LocalDate.class, localDateFormatter);
        registry.addFormatterForFieldType(LocalDateTime.class, localDateTimeFormatter);

        registry.addFormatter(dateFormatter);
        registry.addFormatter(timestampFormatter);
        registry.addFormatter(localDateFormatter);
        registry.addFormatter(localDateTimeFormatter);
    }

    /**
     * 注册跨域
     * @param registry
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        CorsRegistration corsRegistration = registry.addMapping("/**");
        corsRegistration.allowedOrigins(CorsConfiguration.ALL);
        corsRegistration.allowedHeaders(CorsConfiguration.ALL);
        corsRegistration.allowedMethods(CorsConfiguration.ALL);
        corsRegistration.allowCredentials(true);
        corsRegistration.maxAge(3600L);
    }

}
