package com.algoprep.bootstrap.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Turns on {@code @Scheduled} for every module's jobs (DD jobs files). */
@Configuration
@EnableScheduling
public class SchedulingConfig {}
