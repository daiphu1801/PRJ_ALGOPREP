package com.algoprep.bootstrap.problembank;

import com.algoprep.harness.application.ports.in.GetStarterCodeUseCase;
import com.algoprep.harness.application.ports.in.PrepareSubmissionUseCase;
import com.algoprep.harness.application.ports.out.ProblemSpecReaderPort;
import com.algoprep.harness.application.ports.out.StarterCodeAccessPort;
import com.algoprep.harness.application.prepare.PrepareSubmissionCommandHandler;
import com.algoprep.harness.application.prepare.ProgramPackaging;
import com.algoprep.harness.application.query.CurrentProblemSpecification;
import com.algoprep.harness.application.query.GetStarterCodeQueryHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Explicit composition keeps existing pure harness component contexts independent of F2. */
@Configuration(proxyBeanMethods = false)
public class HarnessProblemReadConfiguration {
  @Bean
  CurrentProblemSpecification currentProblemSpecification(ProblemSpecReaderPort reader) {
    return new CurrentProblemSpecification(reader);
  }

  @Bean
  GetStarterCodeUseCase getStarterCodeUseCase(
      StarterCodeAccessPort access, CurrentProblemSpecification specs) {
    return new GetStarterCodeQueryHandler(access, specs);
  }

  @Bean
  PrepareSubmissionUseCase prepareSubmissionUseCase(
      CurrentProblemSpecification specs, ProgramPackaging packaging) {
    return new PrepareSubmissionCommandHandler(specs, packaging);
  }
}
