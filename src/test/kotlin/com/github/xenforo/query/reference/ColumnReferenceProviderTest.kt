package com.github.xenforo.query.reference

import com.github.xenforo.query.XenForoQueryTestCase
import com.github.xenforo.query.utils.QueryChainResolver
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression

/**
 * Tests for ColumnReferenceProvider to ensure Go To Definition works correctly
 * for column names in various positions of upsert/update/insert methods.
 */
class ColumnReferenceProviderTest : XenForoQueryTestCase() {

    /**
     * Test that column references are created for array keys in the first argument.
     */
    fun testReferenceForFirstArgKey() {
        if (!isPhpPluginLoaded()) return

        configureByPhpText(
            """
            \XF::query('xf_user')->update([
                '<caret>username' => 'new_value'
            ]);
            """.trimIndent(),
        )

        // The reference provider should create a reference for 'username'
        val element = getElementAtCaret()
        assertNotNull("Should find element at caret", element)

        // Verify the position is detected as KEY
        val position = QueryChainResolver.getColumnArrayPosition(element!!)
        assertEquals(
            "Position should be KEY for first arg array key",
            QueryChainResolver.ColumnArrayPosition.KEY,
            position
        )
    }

    /**
     * Test that column references are created for array values in upsert's second argument.
     */
    fun testReferenceForUpsertSecondArgValue() {
        if (!isPhpPluginLoaded()) return

        configureByPhpText(
            """
            \XF::query('xf_user')
                ->upsert(
                    ['username' => 'test'],
                    ['<caret>username'],
                    ['last_seen']
                );
            """.trimIndent(),
        )

        val element = getElementAtCaret()
        assertNotNull("Should find element at caret", element)

        // Verify the position is detected as VALUE
        val position = QueryChainResolver.getColumnArrayPosition(element!!)
        assertEquals(
            "Position should be VALUE for upsert second arg",
            QueryChainResolver.ColumnArrayPosition.VALUE,
            position
        )
    }

    /**
     * Test that column references are created for array values in upsert's third argument.
     */
    fun testReferenceForUpsertThirdArgValue() {
        if (!isPhpPluginLoaded()) return

        configureByPhpText(
            """
            \XF::query('xf_user')
                ->upsert(
                    ['username' => 'test'],
                    ['username'],
                    ['<caret>last_seen']
                );
            """.trimIndent(),
        )

        val element = getElementAtCaret()
        assertNotNull("Should find element at caret", element)

        // Verify the position is detected as VALUE
        val position = QueryChainResolver.getColumnArrayPosition(element!!)
        assertEquals(
            "Position should be VALUE for upsert third arg",
            QueryChainResolver.ColumnArrayPosition.VALUE,
            position
        )
    }

    /**
     * Test that NO reference is created for nested array keys inside json_encode.
     */
    fun testNoReferenceForNestedJsonEncodeKeys() {
        if (!isPhpPluginLoaded()) return

        configureByPhpText(
            """
            \XF::query('xf_user')
                ->upsert(
                    [
                        'data' => json_encode(['<caret>nested_key' => 'value']),
                    ],
                    ['username']
                );
            """.trimIndent(),
        )

        val element = getElementAtCaret()
        assertNotNull("Should find element at caret", element)

        // Verify the position is NONE (not a column)
        val position = QueryChainResolver.getColumnArrayPosition(element!!)
        assertEquals(
            "Position should be NONE for nested json_encode keys",
            QueryChainResolver.ColumnArrayPosition.NONE,
            position
        )
    }

    /**
     * Test that NO reference is created for plain nested array keys.
     */
    fun testNoReferenceForPlainNestedArrayKeys() {
        if (!isPhpPluginLoaded()) return

        configureByPhpText(
            """
            \XF::query('xf_user')
                ->upsert(
                    [
                        'options' => ['<caret>nested' => 'value'],
                    ],
                    ['username']
                );
            """.trimIndent(),
        )

        val element = getElementAtCaret()
        assertNotNull("Should find element at caret", element)

        // Verify the position is NONE (not a column)
        val position = QueryChainResolver.getColumnArrayPosition(element!!)
        assertEquals(
            "Position should be NONE for plain nested array keys",
            QueryChainResolver.ColumnArrayPosition.NONE,
            position
        )
    }

    /**
     * Test that references work for insert method array keys.
     */
    fun testReferenceForInsertArrayKeys() {
        if (!isPhpPluginLoaded()) return

        configureByPhpText(
            """
            \XF::query('xf_user')->insert([
                '<caret>username' => 'test'
            ]);
            """.trimIndent(),
        )

        val element = getElementAtCaret()
        assertNotNull("Should find element at caret", element)

        val position = QueryChainResolver.getColumnArrayPosition(element!!)
        assertEquals(
            "Position should be KEY for insert array key",
            QueryChainResolver.ColumnArrayPosition.KEY,
            position
        )
    }

    /**
     * Test the ColumnReferenceProvider can be instantiated.
     */
    fun testProviderCanBeInstantiated() {
        val provider = ColumnReferenceProvider()
        assertNotNull("Provider should be created", provider)
    }
}
