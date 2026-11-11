package com.algoprep.aireview.application.command;

/** JSON Schema text handed to the provider (DD BD F5 1.3a). Server fields are not asked for. */
final class SolutionReviewSchema {
  static final String JSON =
      """
      {"type":"object","required":["approachTitle","verdict","complexity","strengths","improvements",\
      "edgeCases","followUpTopics","readabilityScore"],"properties":{\
      "approachTitle":{"type":"string","maxLength":60},\
      "verdict":{"enum":["OPTIMAL","IMPROVABLE","NOT_OPTIMAL"]},\
      "complexity":{"type":"object","required":["actualTime","timeExplanation","actualSpace",\
      "spaceExplanation"],"properties":{"actualTime":{"type":"string"},\
      "optimalTime":{"type":["string","null"]},"timeExplanation":{"type":"string"},\
      "actualSpace":{"type":"string"},"optimalSpace":{"type":["string","null"]},\
      "spaceExplanation":{"type":"string"}}},\
      "strengths":{"type":"array","items":{"type":"string"}},\
      "improvements":{"type":"array","items":{"type":"string"}},\
      "edgeCases":{"type":"array","items":{"type":"string"}},\
      "followUpTopics":{"type":"array","items":{"type":"string"}},\
      "readabilityScore":{"type":"integer","minimum":1,"maximum":5},\
      "codeDiff":{"type":["object","null"],"properties":{"suggestedCode":{"type":"string"},\
      "explanation":{"type":"string"}}}}}""";

  private SolutionReviewSchema() {}
}
