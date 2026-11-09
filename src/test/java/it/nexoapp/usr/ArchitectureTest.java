package it.nexoapp.usr;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchTests;
import it.nexoapp.common.testing.ArchitectureRules;

/**
 * Runs the shared architecture rules of common (visibility, layering, native query confinement) against the
 * users production classes.
 */
@AnalyzeClasses(packages = "it.nexoapp.usr", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    /** The shared rule set, owned by common. */
    @ArchTest
    static final ArchTests RULES = ArchTests.in(ArchitectureRules.class);

}
