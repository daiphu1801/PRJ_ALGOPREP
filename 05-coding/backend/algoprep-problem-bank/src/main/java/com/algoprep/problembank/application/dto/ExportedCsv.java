package com.algoprep.problembank.application.dto;

/** A finished export: the download name and the UTF-8 bytes, BOM included. */
public record ExportedCsv(String filename, byte[] content) {}
