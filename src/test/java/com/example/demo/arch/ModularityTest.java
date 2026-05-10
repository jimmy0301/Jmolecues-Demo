package com.example.demo.arch;

import com.example.demo.DemoApplication;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

class ModularityTest {

    ApplicationModules modules = ApplicationModules.of(DemoApplication.class);

    @Test
    void verifiesModularStructure() {
        modules.verify();
    }

    @Test
    void documentModules() {
        new Documenter(modules).writeDocumentation();
    }
}
