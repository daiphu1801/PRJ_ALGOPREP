package com.algoprep.bootstrap.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Bốn luật TẦNG của {@code 01-rd/system/backend_architecture.md} mục 3.A, áp cho mọi module.
 *
 * <p>Đây là phần Spring Modulith không diễn đạt được — nó chỉ hiểu ranh giới GIỮA các module theo
 * quy ước package riêng của nó, còn bốn luật dưới đây là luật BÊN TRONG một module. Đó là lý do
 * {@code DEC-2026-0901-backend-base-architecture} điểm 1 chọn ArchUnit.
 */
class LayerRuleTest {

  private static final String BASE = "com.algoprep";

  private static JavaClasses classes;

  @BeforeAll
  static void importClasses() {
    classes =
        new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(BASE);
  }

  @Test
  @DisplayName("Luật tầng 1 — domain là Java thuần: không Spring, không JPA, không Jackson")
  void luatTang1DomainLaJavaThuan() {
    noClasses()
        .that()
        .resideInAPackage(BASE + "..domain..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(
            "org.springframework..",
            "jakarta.persistence..",
            "jakarta.validation..",
            "com.fasterxml.jackson..",
            "org.hibernate..")
        .because(
            "lõi nghiệp vụ phải test được không cần khởi động Spring và không cần database; đây là"
                + " điều kiện để bộ sinh mã F3 test được bằng golden file (environment.md mục 3.A)")
        .check(classes);
  }

  @Test
  @DisplayName("Luật tầng 2 — presentation không gọi thẳng infrastructure")
  void luatTang2PresentationKhongGoiInfrastructure() {
    noClasses()
        .that()
        .resideInAPackage(BASE + "..presentation..")
        .should()
        .dependOnClassesThat()
        .resideInAPackage(BASE + "..infrastructure..")
        .because(
            "controller @Autowired một repository JPA là dấu hiệu kinh điển của việc tầng ứng dụng"
                + " bị bỏ qua, và cùng với nó là kiểm quyền sở hữu dữ liệu và ranh giới transaction")
        .check(classes);
  }

  @Test
  @DisplayName("Luật tầng 3 — application không biết HTTP")
  void luatTang3ApplicationKhongBietHttp() {
    noClasses()
        .that()
        .resideInAPackage(BASE + "..application..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage("jakarta.servlet..", "org.springframework.web..")
        .because(
            "ca sử dụng phải gọi được từ một consumer RabbitMQ và từ một job định kỳ, không chỉ từ"
                + " một request HTTP — luồng chấm bài đi vào bằng cả ba đường")
        .check(classes);
  }

  @Test
  @DisplayName("Luật tầng 4 — cổng đặt đúng chỗ và là interface")
  void luatTang4CongDatDungCho() {
    // Repository là một cổng ra và sống ở domain/ports/out.
    noClasses()
        .that()
        .resideInAPackage(BASE + "..domain.repository..")
        .should()
        .beTopLevelClasses()
        .because(
            "KHÔNG có package domain/repository — repository là một cổng ra, sống trong"
                + " domain/ports/out (backend_architecture.md mục 3.A)")
        .check(classes);

    // Thay cho application/usecase là cặp command/ và query/.
    noClasses()
        .that()
        .resideInAPackage(BASE + "..application.usecase..")
        .should()
        .beTopLevelClasses()
        .because(
            "KHÔNG có package application/usecase — thay bằng cặp command/ và query/, vì hai đường"
                + " có nhu cầu dữ liệu khác nhau (backend_architecture.md mục 3.A)")
        .check(classes);

    // Cổng là hợp đồng, nên phải là interface.
    classes()
        .that()
        .resideInAPackage(BASE + "..ports..")
        .and()
        .areNotAnnotations()
        .should()
        .beInterfaces()
        .because(
            "một cổng là hợp đồng; một class trong ports.. nghĩa là hiện thực đã lẫn vào chỗ khai"
                + " báo, và adapter mất chỗ để thay thế")
        .check(classes);
  }
}
