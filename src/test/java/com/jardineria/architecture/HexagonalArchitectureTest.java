package com.jardineria.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

/**
 * Verifica automáticamente las fronteras de la Arquitectura Hexagonal (Ports &amp; Adapters).
 * Si algún cambio futuro rompe la regla de dependencias, {@code mvn test} falla.
 */
@AnalyzeClasses(packages = "com.jardineria", importOptions = ImportOption.DoNotIncludeTests.class)
class HexagonalArchitectureTest {

    @ArchTest
    static final ArchRule capas_respetan_la_regla_de_dependencias = layeredArchitecture()
            .consideringOnlyDependenciesInLayers()
            .layer("Dominio").definedBy("com.jardineria.domain..")
            .layer("Aplicacion").definedBy("com.jardineria.application..")
            .layer("Infraestructura").definedBy("com.jardineria.infrastructure..")
            .whereLayer("Infraestructura").mayNotBeAccessedByAnyLayer()
            .whereLayer("Aplicacion").mayOnlyBeAccessedByLayers("Infraestructura")
            .whereLayer("Dominio").mayOnlyBeAccessedByLayers("Aplicacion", "Infraestructura");

    @ArchTest
    static final ArchRule dominio_agnostico_de_frameworks = noClasses()
            .that().resideInAPackage("com.jardineria.domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "java.sql..", "javax.sql..", "java.awt..", "javax.swing..", "javafx..",
                    "com.zaxxer..", "com.mysql..", "org.slf4j..")
            .because("el núcleo del dominio no debe conocer persistencia, UI ni librerías externas");

    @ArchTest
    static final ArchRule aplicacion_sin_tecnologia = noClasses()
            .that().resideInAPackage("com.jardineria.application..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "java.sql..", "javax.sql..", "java.awt..", "javax.swing..", "javafx..", "com.zaxxer..")
            .because("los casos de uso orquestan puertos, no tecnologías concretas");

    @ArchTest
    static final ArchRule adaptadores_de_entrada_no_usan_persistencia = noClasses()
            .that().resideInAPackage("com.jardineria.infrastructure.adapter.in..")
            .should().dependOnClassesThat().resideInAPackage("com.jardineria.infrastructure.adapter.out..")
            .because("la GUI y la CLI hablan con los puertos de entrada, nunca con los adaptadores JDBC");

    @ArchTest
    static final ArchRule adaptadores_de_entrada_dependen_de_puertos = noClasses()
            .that().resideInAPackage("com.jardineria.infrastructure.adapter.in..")
            .should().dependOnClassesThat().resideInAPackage("com.jardineria.application.service..")
            .because("los adaptadores de entrada dependen de abstracciones (*UseCase), no de servicios concretos (DIP)");
}
