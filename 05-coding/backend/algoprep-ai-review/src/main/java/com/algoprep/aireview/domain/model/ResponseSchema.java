package com.algoprep.aireview.domain.model;

/** Name, version and JSON Schema text the provider output must satisfy. */
public record ResponseSchema(String name, String version, String jsonSchema) {}
