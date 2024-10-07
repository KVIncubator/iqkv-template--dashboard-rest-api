package com.iqkv.sample.webmvc.dashboard.config;

import jakarta.servlet.ServletContext;

import com.iqkv.boot.security.SecurityProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.util.CollectionUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * Configuration of web application with Servlet 3.0 APIs.
 */
@Configuration
public class WebConfigurer implements ServletContextInitializer {

  private static final Logger LOG = LoggerFactory.getLogger(WebConfigurer.class);

  private final Environment env;

  private final SecurityProperties securityProperties;

  public WebConfigurer(Environment env, SecurityProperties securityProperties) {
    this.env = env;
    this.securityProperties = securityProperties;
  }

  @Override
  public void onStartup(ServletContext servletContext) {
    if (env.getActiveProfiles().length != 0) {
      LOG.info("Web application configuration, using profiles: {}", (Object[]) env.getActiveProfiles());
    }

    LOG.info("Web application fully configured");
  }

  @Bean
  public CorsFilter corsFilter() {
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    CorsConfiguration config = securityProperties.getCors();
    if (!CollectionUtils.isEmpty(config.getAllowedOrigins()) || !CollectionUtils.isEmpty(config.getAllowedOriginPatterns())) {
      LOG.debug("Registering CORS filter");
      source.registerCorsConfiguration("/api/**", config);
      source.registerCorsConfiguration("/management/**", config);
      source.registerCorsConfiguration("/v3/api-docs", config);
      source.registerCorsConfiguration("/swagger-ui/**", config);
    }
    return new CorsFilter(source);
  }
}