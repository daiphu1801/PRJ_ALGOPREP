package com.algoprep.problembank.domain.model;

/** Testcase counts of one problem: all rows, approved rows, approved Sample rows. */
public record ApprovalCounts(int total, int approved, int approvedSamples) {}
