package cc.ivera.gray.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;

import cc.ivera.gray.common.VersionCompare;
import org.junit.jupiter.api.Test;

class GrayRuleServiceTest {
    @Test
    void appVersionCompareShouldSupportSemanticVersion() {
        assertEquals(1, VersionCompare.compare("2.1.0", "2.0.9"));
        assertEquals(0, VersionCompare.compare("2.1", "2.1.0"));
        assertEquals(-1, VersionCompare.compare("1.9.9", "2.0.0"));
    }
}

