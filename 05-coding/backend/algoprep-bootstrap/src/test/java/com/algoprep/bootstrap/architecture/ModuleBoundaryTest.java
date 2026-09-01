package com.algoprep.bootstrap.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Ba luật ranh giới MODULE của {@code 01-rd/system/backend_architecture.md} mục 2.A, cộng luật
 * package ở mục 2 phần cuối.
 *
 * <p>Vi phạm phải làm FAIL BUILD, không phải cảnh báo ({@code 01-rd/system/environment.md} mục
 * 3.A).
 */
class ModuleBoundaryTest {

  private static final String BASE = "com.algoprep";

  /** Sáu Bounded Context. Tên package không có dấu gạch — Java không cho. */
  private static final List<String> CONTEXTS =
      List.of("identity", "problembank", "harness", "judge", "aireview", "interviewbank");

  private static JavaClasses classes;

  @BeforeAll
  static void importClasses() {
    classes =
        new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(BASE);
  }

  @Test
  @DisplayName("Luật 1 — module nghiệp vụ chỉ phụ thuộc algoprep-common, không phụ thuộc nhau")
  void luat1ModuleNghiepVuKhongPhuThuocNhau() {
    for (String source : CONTEXTS) {
      String[] others =
          CONTEXTS.stream()
              .filter(c -> !c.equals(source))
              .map(c -> BASE + "." + c + "..")
              .toArray(String[]::new);

      noClasses()
          .that()
          .resideInAPackage(BASE + "." + source + "..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage(others)
          .because(
              "module nghiệp vụ chỉ được phụ thuộc algoprep-common; hai module cần nói chuyện thì đi"
                  + " bằng sự kiện miền hoặc bằng cổng ra do module gọi tự khai"
                  + " (backend_architecture.md mục 5), không bằng import thẳng")
          .check(classes);
    }
  }

  @Test
  @DisplayName("Luật 2 — không ai được phụ thuộc algoprep-bootstrap")
  void luat2KhongAiPhuThuocBootstrap() {
    noClasses()
        .that()
        .resideOutsideOfPackage(BASE + ".bootstrap..")
        .should()
        .dependOnClassesThat()
        .resideInAPackage(BASE + ".bootstrap..")
        .because(
            "bootstrap phụ thuộc tất cả và không ai phụ thuộc nó; một phụ thuộc ngược sẽ tạo vòng"
                + " và biến tầng khởi chạy thành nơi chứa nghiệp vụ")
        .check(classes);
  }

  @Test
  @DisplayName("Luật 3 — algoprep-common không phụ thuộc ai")
  void luat3CommonKhongPhuThuocAi() {
    String[] everyoneElse =
        java.util.stream.Stream.concat(CONTEXTS.stream(), java.util.stream.Stream.of("bootstrap"))
            .map(m -> BASE + "." + m + "..")
            .toArray(String[]::new);

    noClasses()
        .that()
        .resideInAPackage(BASE + ".common..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(everyoneElse)
        .because(
            "nhân dùng chung phải là lá của đồ hình phụ thuộc; một phụ thuộc ra ngoài biến nó thành"
                + " nơi chứa nghiệp vụ của context khác")
        .check(classes);
  }

  @Test
  @DisplayName("Luật 4 — presentation/infrastructure của module A không xuất hiện trong module B")
  void luat4KhongChamVaoPresentationHayInfrastructureCuaModuleKhac() {
    List<String> allModules =
        java.util.stream.Stream.concat(CONTEXTS.stream(), java.util.stream.Stream.of("bootstrap"))
            .toList();

    for (String source : allModules) {
      String[] forbidden =
          allModules.stream()
              .filter(m -> !m.equals(source))
              .flatMap(
                  m ->
                      java.util.stream.Stream.of(
                          BASE + "." + m + ".presentation..", BASE + "." + m + ".infrastructure.."))
              .toArray(String[]::new);

      noClasses()
          .that()
          .resideInAPackage(BASE + "." + source + "..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage(forbidden)
          .because(
              "ràng buộc chặt hơn pom.xml và không diễn đạt được bằng pom.xml"
                  + " (backend_architecture.md mục 2.A phần cuối). Áp cho CẢ bootstrap: bootstrap"
                  + " lắp bean bằng component scan, không cần import adapter của module khác — nới"
                  + " luật này về sau dễ hơn siết nó lại")
          .check(classes);
    }
  }
}
