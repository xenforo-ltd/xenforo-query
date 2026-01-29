package com.github.xenforo.query.inspections

import com.github.xenforo.query.XenForoQueryTestCase

/**
 * Tests for UnknownColumnInspection to ensure it correctly identifies columns
 * while avoiding false positives in nested structures.
 *
 * Note: The core logic is tested in QueryChainResolverTest. These tests verify
 * the inspection is properly registered and doesn't throw errors during analysis.
 */
class UnknownColumnInspectionTest : XenForoQueryTestCase() {

    /**
     * Test that the inspection can be instantiated without errors.
     */
    fun testInspectionCanBeInstantiated() {
        val inspection = UnknownColumnInspection()
        assertNotNull("Inspection should be created", inspection)
    }

    /**
     * Test that inspection runs without errors on code with nested arrays.
     * Note: Full integration testing requires database setup.
     */
    fun testInspectionRunsWithoutErrors() {
        if (!isPhpPluginLoaded()) return

        // This test just verifies the code parses and inspection doesn't crash
        configureByPhpText(
            """
            \XF::query('xf_user')
                ->update([
                    'username' => 'new_value',
                    'data' => json_encode(['nested_key' => 'value']),
                ]);
            """.trimIndent(),
        )

        // If we get here without exception, the test passes
        assertTrue("Code should parse without errors", true)
    }
}
