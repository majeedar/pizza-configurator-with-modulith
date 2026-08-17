package com.example.pizzaconfigurator;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

/**
 * Fails the build on module cycles or a module reaching into another
 * module's internal package (agent.md §1.2).
 */
class ModularityTests {

    static final ApplicationModules MODULES = ApplicationModules.of(PizzaConfiguratorApplication.class);

    @Test
    void moduleStructureIsRespected() {
        MODULES.verify();
    }

    @Test
    void printModuleStructure() {
        System.out.println(MODULES);
    }
}
