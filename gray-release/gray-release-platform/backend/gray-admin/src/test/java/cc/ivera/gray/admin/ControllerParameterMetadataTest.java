package cc.ivera.gray.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cc.ivera.gray.admin.controller.GrayRuleController;
import cc.ivera.gray.admin.entity.GrayRule;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;

class ControllerParameterMetadataTest {
    @Test
    void controllerParametersShouldExposeNamesForSpringBinding() throws Exception {
        Method publish = GrayRuleController.class.getDeclaredMethod("publish", String.class);
        Method update = GrayRuleController.class.getDeclaredMethod("update", Long.class, GrayRule.class, String.class);

        assertTrue(publish.getParameters()[0].isNamePresent());
        assertEquals("serviceId", publish.getParameters()[0].getName());
        assertTrue(update.getParameters()[0].isNamePresent());
        assertEquals("id", update.getParameters()[0].getName());
    }
}
