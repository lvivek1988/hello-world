package com.tcs.telecom.tib.configuration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.support.SpringBootServletInitializer;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@EnableZuulProxy
@ComponentScan({"com.tcs.telecom.tib"})
public class ZuulConfiguration extends SpringBootServletInitializer {

	public static void main(String[] args) {
		SpringApplication.run(ZuulConfiguration.class, args);
	}
	
}