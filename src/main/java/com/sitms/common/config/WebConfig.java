package com.sitms.common.config;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Web MVC configuration: CORS origins are driven by the {@code CORS_ALLOWED_ORIGINS} env var. */
@Configuration
public class WebConfig implements WebMvcConfigurer {

  private final List<String> allowedOrigins;

  public WebConfig(
      @Value("${app.cors.allowed-origins:http://localhost:4200}") List<String> allowedOrigins) {
    this.allowedOrigins = allowedOrigins;
  }

  @Override
  public void addCorsMappings(CorsRegistry registry) {
    registry
        .addMapping("/**")
        .allowedOrigins(allowedOrigins.toArray(String[]::new))
        .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
        .allowedHeaders("*");
  }
}
