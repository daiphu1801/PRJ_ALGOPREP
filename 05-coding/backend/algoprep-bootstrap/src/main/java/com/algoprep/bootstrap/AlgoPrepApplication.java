package com.algoprep.bootstrap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Tiến trình duy nhất của backend AlgoPrep.
 *
 * <p>Modular Monolith: một tiến trình, một PostgreSQL instance, tám module Maven ({@code
 * DEC-2026-0820-architecture-baseline}). Quét component từ {@code com.algoprep} để mọi module được
 * nạp, còn ranh giới giữa chúng do ArchUnit giữ chứ không do phạm vi quét giữ.
 */
@SpringBootApplication(scanBasePackages = "com.algoprep")
public class AlgoPrepApplication {

  public static void main(String[] args) {
    SpringApplication.run(AlgoPrepApplication.class, args);
  }
}
