package com.algoprep.aireview.domain.prompt;

/**
 * Layer 2 input from problem-bank. {@code aiBrief} is written by the problem author, so it is
 * untrusted data; the two booleans are server-printed switches.
 */
public record ProblemGuidance(String aiBrief, boolean noFullCode, boolean socraticOnly) {}
