package com.tenantcomplaint;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

/**
 * Main application class.
 * Extends SpringBootServletInitializer to support WAR deployment on Tomcat.
 */
@SpringBootApplication
public class TenantComplaintApplication extends SpringBootServletInitializer {

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        return builder.sources(TenantComplaintApplication.class);
    }

    public static void main(String[] args) {
        SpringApplication.run(TenantComplaintApplication.class, args);
    }
}
