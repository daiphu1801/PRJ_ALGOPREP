package com.algoprep.harness.infrastructure.template;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.algoprep.common.exception.AlgoPrepException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/** Tests eager P1 construction against the real classes with one isolated damaged jar asset. */
class HarnessStartupAssetsTest {
  private static final String HANDLER =
      "com.algoprep.harness.application.query.GetStarterCodeQueryHandler";

  @Test
  void completePackagedAssetsAllowStartupWithoutAnyProblemRead() throws Exception {
    try (var loader = new AssetLoader("", "healthy");
        var context = context(loader)) {
      context.refresh();
      assertThat(context.isActive()).isTrue();
      assertThat(context.getBean("starter").getClass().getName()).isEqualTo(HANDLER);
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"java", "cpp", "python"})
  void missingLanguageAssetStopsStartupBeforeAnyRequest(String language) throws Exception {
    rejects("harness-templates/" + language + "/starter-wrapper.txt", "missing");
  }

  @ParameterizedTest
  @ValueSource(strings = {"missing-anchor", "duplicate-anchor"})
  void malformedTemplateStopsStartupBeforeAnyRequest(String damage) throws Exception {
    rejects("harness-templates/java/starter-wrapper.txt", damage);
  }

  private void rejects(String resource, String damage) throws Exception {
    try (var loader = new AssetLoader(resource, damage);
        var context = context(loader)) {
      assertThatThrownBy(context::refresh)
          .isInstanceOf(BeanCreationException.class)
          .satisfies(
              failure -> {
                Throwable cause = failure;
                while (cause.getCause() != null) {
                  cause = cause.getCause();
                }
                assertThat(cause)
                    .isInstanceOfSatisfying(
                        AlgoPrepException.class,
                        error -> assertThat(error.errorCode()).isEqualTo("HRN-030"));
              });
      assertThat(context.isActive()).isFalse();
    }
  }

  private AnnotationConfigApplicationContext context(ClassLoader loader) {
    var context = new AnnotationConfigApplicationContext();
    context.setClassLoader(loader);
    context.registerBean("starter", Object.class, () -> handler(loader));
    return context;
  }

  private Object handler(ClassLoader loader) {
    try {
      String base = "com.algoprep.harness.application.";
      Class<?> reader = loader.loadClass(base + "ports.out.ProblemSpecReaderPort");
      Class<?> access = loader.loadClass(base + "ports.out.StarterCodeAccessPort");
      Class<?> current = loader.loadClass(base + "query.CurrentProblemSpecification");
      Object specs = current.getConstructor(reader).newInstance(unusedPort(loader, reader));
      return loader
          .loadClass(HANDLER)
          .getConstructor(access, current)
          .newInstance(unusedPort(loader, access), specs);
    } catch (InvocationTargetException failure) {
      throw new IllegalStateException("P1 construction failed", failure.getCause());
    } catch (ReflectiveOperationException failure) {
      throw new IllegalStateException("Invalid startup test wiring", failure);
    }
  }

  private Object unusedPort(ClassLoader loader, Class<?> type) {
    return Proxy.newProxyInstance(
        loader,
        new Class<?>[] {type},
        (proxy, method, arguments) -> {
          throw new AssertionError("Startup must not read problem data");
        });
  }

  private static final class AssetLoader extends URLClassLoader {
    private final String resource;
    private final String damage;

    AssetLoader(String resource, String damage) {
      super(
          new URL[] {
            ClasspathStarterCodeTemplates.class.getProtectionDomain().getCodeSource().getLocation()
          },
          HarnessStartupAssetsTest.class.getClassLoader());
      this.resource = resource;
      this.damage = damage;
    }

    @Override
    protected synchronized Class<?> loadClass(String name, boolean resolve)
        throws ClassNotFoundException {
      if (!name.startsWith("com.algoprep.harness.")) {
        return super.loadClass(name, resolve);
      }
      Class<?> result = findLoadedClass(name);
      if (result == null) {
        result = findClass(name);
      }
      if (resolve) {
        resolveClass(result);
      }
      return result;
    }

    @Override
    public InputStream getResourceAsStream(String name) {
      if (!name.equals(resource)) {
        return super.getResourceAsStream(name);
      }
      if (damage.equals("missing")) {
        return null;
      }
      try (var source = super.getResourceAsStream(name)) {
        if (source == null) {
          throw new AssertionError("The original asset must exist");
        }
        String text = new String(source.readAllBytes(), StandardCharsets.UTF_8);
        String modified =
            damage.equals("missing-anchor")
                ? text.replace("{{DECLARATION}}", "")
                : text + "{{DECLARATION}}";
        return new ByteArrayInputStream(modified.getBytes(StandardCharsets.UTF_8));
      } catch (IOException failure) {
        throw new AssertionError("Unable to read startup fixture", failure);
      }
    }
  }
}
