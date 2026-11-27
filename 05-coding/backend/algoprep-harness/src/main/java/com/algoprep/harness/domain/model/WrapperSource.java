package com.algoprep.harness.domain.model;

/** Trusted, already rendered framework fragments. Each nonempty fragment ends in LF. */
public record WrapperSource(String prefix, String beforeCall, String callSite, String afterCall) {}
