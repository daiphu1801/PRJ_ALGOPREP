package com.algoprep.problembank.application.query;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.algoprep.problembank.application.dto.CatalogQuery;
import com.algoprep.problembank.domain.model.CatalogKind;
import com.algoprep.problembank.domain.ports.out.CatalogRepository;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class CatalogQueryHandlerTest {
  @ParameterizedTest
  @CsvSource({"true,true", "true,false", "false,true", "false,false"})
  void onlyAdminExplicitlyRequestingCountsReceivesThem(boolean admin, boolean counts) {
    var repository = mock(CatalogRepository.class);
    var query = new CatalogQuery(admin, counts);
    new ListProblemLevelsQueryHandler(repository).list(query);
    new ListProblemTopicsQueryHandler(repository).list(query);
    verify(repository).list(CatalogKind.LEVEL, admin && counts);
    verify(repository).list(CatalogKind.TOPIC, admin && counts);
  }
}
