package com.example.sha256.api.upload;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(MultipartUploadProperties.class)
public class MultipartUploadConfiguration { }
