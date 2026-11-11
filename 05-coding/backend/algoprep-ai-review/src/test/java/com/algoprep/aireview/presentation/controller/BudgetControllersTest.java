package com.algoprep.aireview.presentation.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.algoprep.aireview.application.dto.BudgetStatus;
import com.algoprep.aireview.application.ports.in.CreateModelPriceUseCase;
import com.algoprep.aireview.application.ports.in.DeleteModelPriceUseCase;
import com.algoprep.aireview.application.ports.in.GetAiLimitsConfigUseCase;
import com.algoprep.aireview.application.ports.in.GetAiTokenBudgetUseCase;
import com.algoprep.aireview.application.ports.in.ListModelPricesUseCase;
import com.algoprep.aireview.application.ports.in.SetAiTokenLimitUseCase;
import com.algoprep.aireview.application.ports.in.UnlockAiTokenBudgetUseCase;
import com.algoprep.aireview.application.ports.in.UpdateAiLimitsConfigUseCase;
import com.algoprep.aireview.application.ports.in.UpdateModelPriceUseCase;
import com.algoprep.aireview.domain.model.FeatureCode;
import com.algoprep.aireview.domain.model.LimitsConfig;
import com.algoprep.aireview.domain.model.LimitsPatch;
import com.algoprep.aireview.domain.model.ModelPrice;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import tools.jackson.databind.json.JsonMapper;

class BudgetControllersTest {
  private static final JsonMapper JSON = JsonMapper.builder().build();
  private final GetAiTokenBudgetUseCase status = mock(GetAiTokenBudgetUseCase.class);
  private final SetAiTokenLimitUseCase setLimit = mock(SetAiTokenLimitUseCase.class);
  private final UnlockAiTokenBudgetUseCase unlock = mock(UnlockAiTokenBudgetUseCase.class);
  private final GetAiLimitsConfigUseCase readLimits = mock(GetAiLimitsConfigUseCase.class);
  private final UpdateAiLimitsConfigUseCase updateLimits = mock(UpdateAiLimitsConfigUseCase.class);
  private final TokenBudgetController budgets =
      new TokenBudgetController(status, setLimit, unlock, readLimits, updateLimits);
  private final ListModelPricesUseCase list = mock(ListModelPricesUseCase.class);
  private final CreateModelPriceUseCase create = mock(CreateModelPriceUseCase.class);
  private final UpdateModelPriceUseCase update = mock(UpdateModelPriceUseCase.class);
  private final DeleteModelPriceUseCase delete = mock(DeleteModelPriceUseCase.class);
  private final ModelPriceController prices =
      new ModelPriceController(list, create, update, delete);
  private final UUID admin = UUID.randomUUID();

  private JwtAuthenticationToken auth() {
    Jwt jwt = Jwt.withTokenValue("t").header("alg", "none").subject(admin.toString()).build();
    return new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
  }

  private static String rule(Class<?> type, String method) {
    for (Method m : type.getDeclaredMethods()) {
      if (m.getName().equals(method) && m.isAnnotationPresent(PreAuthorize.class)) {
        return m.getAnnotation(PreAuthorize.class).value();
      }
    }
    throw new AssertionError("no rule for " + method);
  }

  @Test
  void budgetLimitAndUnlockNeedTheBudgetUpdateCellNeverTheConfigOne() {
    for (String method : new String[] {"setLimit", "unlock"}) {
      assertThat(rule(TokenBudgetController.class, method))
          .contains("ROLE_ADMIN", "AI_TOKEN_BUDGET:UPDATE")
          .doesNotContain("AI_CONFIG");
    }
  }

  @Test
  void theTwoBudgetReadsAcceptEitherReadCellAndLimitsWriteNeedsConfigUpdate() {
    for (String method : new String[] {"budget", "limits"}) {
      assertThat(rule(TokenBudgetController.class, method))
          .contains("ROLE_ADMIN", "AI_TOKEN_BUDGET:READ", "AI_CONFIG:READ", " or ")
          .doesNotContain("UPDATE");
    }
    assertThat(rule(TokenBudgetController.class, "updateLimits"))
        .contains("ROLE_ADMIN", "AI_CONFIG:UPDATE")
        .doesNotContain("AI_TOKEN_BUDGET");
  }

  @Test
  void priceEndpointsUseTheConfigCells() {
    assertThat(rule(ModelPriceController.class, "list")).contains("ROLE_ADMIN", "AI_CONFIG:READ");
    for (String method : new String[] {"create", "update", "delete"}) {
      assertThat(rule(ModelPriceController.class, method))
          .contains("ROLE_ADMIN", "AI_CONFIG:UPDATE");
    }
  }

  @Test
  void budgetJsonCarriesIsLockedAndAllFourFeaturesInOrder() throws Exception {
    Map<FeatureCode, Long> breakdown = new EnumMap<>(FeatureCode.class);
    for (FeatureCode code : FeatureCode.values()) {
      breakdown.put(code, 0L);
    }
    when(status.status())
        .thenReturn(
            new BudgetStatus(
                5000,
                10,
                LocalDate.of(2026, 11, 1),
                null,
                false,
                LocalDate.of(2026, 12, 1),
                breakdown,
                70));
    String json = JSON.writeValueAsString(budgets.budget());
    assertThat(JSON.readTree(json).get("isLocked").asBoolean(true)).isFalse();
    assertThat(JSON.readTree(json).get("limitTokens").asLong()).isEqualTo(5000);
    assertThat(JSON.readTree(json).get("featureBreakdown")).hasSize(4);
    assertThat(JSON.readTree(json).get("featureBreakdown").get(0).get("featureCode").asString())
        .isEqualTo("SOLUTION_REVIEW");
    assertThat(JSON.readTree(json).get("lockedAt").isNull()).isTrue();
  }

  @Test
  void limitUnlockAndLimitsPassTheCallerAndParsedValues() throws Exception {
    BudgetStatus ok =
        new BudgetStatus(1, 0, LocalDate.EPOCH, Instant.EPOCH, true, null, Map.of(), 70);
    when(setLimit.setLimit(admin, 7)).thenReturn(ok);
    when(unlock.unlock(admin)).thenReturn(ok);
    LimitsConfig stored = new LimitsConfig(5, 3, 20, 70);
    when(updateLimits.update(any(), any())).thenReturn(stored);
    when(readLimits.read()).thenReturn(stored);

    assertThat(budgets.setLimit(auth(), JSON.readTree("{\"limitTokens\":7}")).isLocked()).isTrue();
    assertThat(budgets.unlock(auth()).limitTokens()).isEqualTo(1);
    assertThat(budgets.updateLimits(auth(), JSON.readTree("{\"reviewPerHour\":5}")).reviewPerHour())
        .isEqualTo(5);
    verify(updateLimits).update(admin, new LimitsPatch(5, null, null, null));
    assertThat(budgets.limits().warnPercent()).isEqualTo(70);
  }

  @Test
  void priceEndpointsMapRowsAndParseIds() throws Exception {
    UUID id = UUID.randomUUID();
    ModelPrice row = new ModelPrice(id, "gpt-x", new BigDecimal("0.1500"));
    when(list.list()).thenReturn(List.of(row));
    when(create.create("gpt-x")).thenReturn(row);
    when(update.update(id, null, new BigDecimal("0.15"))).thenReturn(row);

    assertThat(prices.list().items()).hasSize(1);
    assertThat(prices.create(JSON.readTree("{\"modelName\":\" gpt-x \"}")).modelName())
        .isEqualTo("gpt-x");
    assertThat(
            prices
                .update(id.toString(), JSON.readTree("{\"usdPerMillionTokens\":0.15}"))
                .usdPerMillionTokens())
        .isEqualByComparingTo("0.15");
    prices.delete(id.toString());
    verify(delete).delete(id);
  }
}
