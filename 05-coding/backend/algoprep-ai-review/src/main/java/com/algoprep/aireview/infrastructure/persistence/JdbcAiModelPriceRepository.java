package com.algoprep.aireview.infrastructure.persistence;

import com.algoprep.aireview.domain.exception.AiModelPriceConflictException;
import com.algoprep.aireview.domain.model.ModelPrice;
import com.algoprep.aireview.domain.ports.out.AiModelPriceRepository;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class JdbcAiModelPriceRepository implements AiModelPriceRepository {
  private static final String COLUMNS = "id, model_name, usd_per_million_tokens";

  private final ObjectProvider<JdbcClient> jdbc;

  JdbcAiModelPriceRepository(ObjectProvider<JdbcClient> jdbc) {
    this.jdbc = jdbc;
  }

  private static ModelPrice map(ResultSet rs, int row) throws SQLException {
    return new ModelPrice(
        rs.getObject("id", UUID.class),
        rs.getString("model_name"),
        rs.getBigDecimal("usd_per_million_tokens"));
  }

  @Override
  public List<ModelPrice> list() {
    return jdbc.getObject()
        .sql(
            "SELECT "
                + COLUMNS
                + " FROM ai.ai_model_prices ORDER BY lower(model_name), model_name, id")
        .query(JdbcAiModelPriceRepository::map)
        .list();
  }

  @Override
  public ModelPrice create(String modelName) {
    try {
      return jdbc.getObject()
          .sql(
              "INSERT INTO ai.ai_model_prices (id, model_name, usd_per_million_tokens)"
                  + " VALUES (:id, :name, 0) RETURNING "
                  + COLUMNS)
          .param("id", UUID.randomUUID())
          .param("name", modelName)
          .query(JdbcAiModelPriceRepository::map)
          .single();
    } catch (DuplicateKeyException duplicate) {
      throw new AiModelPriceConflictException();
    }
  }

  @Override
  public Optional<ModelPrice> update(UUID id, String modelName, BigDecimal usdPerMillionTokens) {
    try {
      return jdbc.getObject()
          .sql(
              "UPDATE ai.ai_model_prices SET model_name = COALESCE(:name, model_name),"
                  + " usd_per_million_tokens = COALESCE(:price, usd_per_million_tokens),"
                  + " updated_at = now() WHERE id = :id RETURNING "
                  + COLUMNS)
          .param("name", modelName, Types.VARCHAR)
          .param("price", usdPerMillionTokens, Types.NUMERIC)
          .param("id", id)
          .query(JdbcAiModelPriceRepository::map)
          .optional();
    } catch (DuplicateKeyException duplicate) {
      throw new AiModelPriceConflictException();
    }
  }

  @Override
  public void delete(UUID id) {
    jdbc.getObject().sql("DELETE FROM ai.ai_model_prices WHERE id = :id").param("id", id).update();
  }
}
