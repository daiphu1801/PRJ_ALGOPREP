package com.algoprep.aireview.presentation.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.algoprep.aireview.domain.exception.AiInvalidRequestException;
import com.algoprep.aireview.domain.exception.AiModelPriceNotFoundException;
import com.algoprep.aireview.domain.model.LimitsPatch;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class BudgetRequestsTest {
  private static final JsonMapper JSON = JsonMapper.builder().build();

  private static JsonNode json(String text) {
    return JSON.readTree(text);
  }

  private static void rejects(Runnable call, String code) {
    assertThatThrownBy(call::run)
        .isInstanceOf(AiInvalidRequestException.class)
        .extracting(e -> ((AiInvalidRequestException) e).errorCode())
        .isEqualTo(code);
  }

  @Test
  void limitTokensAcceptsOneAndTheCeilingOnly() {
    assertThat(BudgetRequests.limitTokens(json("{\"limitTokens\":1}"))).isEqualTo(1);
    assertThat(BudgetRequests.limitTokens(json("{\"limitTokens\":9000000000000}")))
        .isEqualTo(9_000_000_000_000L);
    for (String bad :
        new String[] {
          "{\"limitTokens\":0}",
          "{\"limitTokens\":-1}",
          "{\"limitTokens\":1.5}",
          "{\"limitTokens\":\"5\"}",
          "{\"limitTokens\":null}",
          "{\"limitTokens\":9000000000001}",
          "{\"limitTokens\":99999999999999999999}",
          "{}",
          "[]"
        }) {
      rejects(() -> BudgetRequests.limitTokens(json(bad)), "AIR-V15");
    }
  }

  @Test
  void limitsAcceptRangesOneToNineNineNineAndOneToOneHundred() {
    assertThat(BudgetRequests.limits(json("{\"cooldownSeconds\":999,\"warnPercent\":100}")))
        .isEqualTo(new LimitsPatch(null, null, 999, 100));
    assertThat(BudgetRequests.limits(json("{\"reviewPerHour\":1,\"interviewPerHour\":5,\"x\":0}")))
        .isEqualTo(new LimitsPatch(1, 5, null, null));
    for (String bad :
        new String[] {
          "{}",
          "{\"unknown\":1}",
          "{\"cooldownSeconds\":1000}",
          "{\"cooldownSeconds\":0}",
          "{\"reviewPerHour\":-1}",
          "{\"interviewPerHour\":1000}",
          "{\"warnPercent\":0}",
          "{\"warnPercent\":101}",
          "{\"warnPercent\":2.5}",
          "{\"warnPercent\":\"7\"}",
          "{\"warnPercent\":null}"
        }) {
      rejects(() -> BudgetRequests.limits(json(bad)), "AIR-V16");
    }
  }

  @Test
  void modelNameIsTrimmedAndLimitedToOneHundredCharacters() {
    assertThat(BudgetRequests.createName(json("{\"modelName\":\"  gpt-x \"}"))).isEqualTo("gpt-x");
    String hundred = "a".repeat(100);
    assertThat(BudgetRequests.createName(json("{\"modelName\":\"" + hundred + "\"}"))).hasSize(100);
    for (String bad :
        new String[] {
          "{\"modelName\":\"   \"}",
          "{\"modelName\":\"\"}",
          "{\"modelName\":\"" + hundred + "a\"}",
          "{\"modelName\":5}",
          "{\"modelName\":null}",
          "{}"
        }) {
      rejects(() -> BudgetRequests.createName(json(bad)), "AIR-V17");
    }
  }

  @Test
  void priceRulesAreNonNegativeFourDecimalsAndTheNumericCeiling() {
    assertThat(BudgetRequests.pricePatch(json("{\"usdPerMillionTokens\":0.15}")))
        .isEqualTo(new BudgetRequests.PricePatch(null, new BigDecimal("0.15")));
    assertThat(BudgetRequests.pricePatch(json("{\"usdPerMillionTokens\":999999.9999}")))
        .isNotNull();
    assertThat(
            BudgetRequests.pricePatch(json("{\"usdPerMillionTokens\":1.50000}"))
                .usdPerMillionTokens())
        .isEqualByComparingTo("1.5");
    for (String bad :
        new String[] {
          "{\"usdPerMillionTokens\":-1}",
          "{\"usdPerMillionTokens\":\"1\"}",
          "{\"usdPerMillionTokens\":null}",
          "{\"usdPerMillionTokens\":0.00001}",
          "{\"usdPerMillionTokens\":1000000}"
        }) {
      rejects(() -> BudgetRequests.pricePatch(json(bad)), "AIR-V18");
    }
  }

  @Test
  void aPriceEditNeedsAtLeastOneFieldAndValidatesTheNameToo() {
    rejects(() -> BudgetRequests.pricePatch(json("{}")), "AIR-V17");
    rejects(() -> BudgetRequests.pricePatch(json("{\"modelName\":\" \"}")), "AIR-V17");
    assertThat(BudgetRequests.pricePatch(json("{\"modelName\":\"m\",\"usdPerMillionTokens\":1}")))
        .isEqualTo(new BudgetRequests.PricePatch("m", BigDecimal.ONE));
  }

  @Test
  void aPathIdThatIsNotAUuidIsNotFound() {
    UUID id = UUID.randomUUID();
    assertThat(BudgetRequests.priceId(id.toString())).isEqualTo(id);
    assertThatThrownBy(() -> BudgetRequests.priceId("not-a-uuid"))
        .isInstanceOf(AiModelPriceNotFoundException.class);
  }
}
