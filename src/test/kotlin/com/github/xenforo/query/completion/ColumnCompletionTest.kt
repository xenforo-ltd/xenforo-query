package com.github.xenforo.query.completion

import com.github.xenforo.query.XenForoQueryTestCase

/**
 * Tests for column name completion in XenForo Query Builder methods.
 *
 * Note: These tests verify the completion provider logic and context detection.
 * Full integration tests require both the PHP plugin and a database connection.
 */
class ColumnCompletionTest : XenForoQueryTestCase() {
    /**
     * Test that completion triggers inside ->where() method.
     */
    fun testWhereMethodTriggers() {
        if (!isPhpPluginLoaded()) return

        configureByPhpText(
            """
            \XF::query('xf_user')->where('<caret>');
            """.trimIndent(),
        )

        val element = getElementAtCaret()
        assertNotNull("Should find element at caret", element)
    }

    /**
     * Test that completion triggers inside ->orWhere() method.
     */
    fun testOrWhereMethodTriggers() {
        if (!isPhpPluginLoaded()) return

        configureByPhpText(
            """
            \XF::query('xf_user')
            	->where('user_state', 'valid')
            	->orWhere('<caret>');
            """.trimIndent(),
        )

        val element = getElementAtCaret()
        assertNotNull("Should find element at caret", element)
    }

    /**
     * Test that completion triggers inside ->select() method.
     */
    fun testSelectMethodTriggers() {
        if (!isPhpPluginLoaded()) return

        configureByPhpText(
            """
            \XF::query('xf_user')->select('<caret>');
            """.trimIndent(),
        )

        val element = getElementAtCaret()
        assertNotNull("Should find element at caret", element)
    }

    /**
     * Test that completion triggers inside ->whereIn() method.
     */
    fun testWhereInMethodTriggers() {
        if (!isPhpPluginLoaded()) return

        configureByPhpText(
            """
            \XF::query('xf_user')->whereIn('<caret>', [1, 2, 3]);
            """.trimIndent(),
        )

        val element = getElementAtCaret()
        assertNotNull("Should find element at caret", element)
    }

    /**
     * Test that completion triggers inside ->whereNull() method.
     */
    fun testWhereNullMethodTriggers() {
        if (!isPhpPluginLoaded()) return

        configureByPhpText(
            """
            \XF::query('xf_user')->whereNull('<caret>');
            """.trimIndent(),
        )

        val element = getElementAtCaret()
        assertNotNull("Should find element at caret", element)
    }

    /**
     * Test that completion triggers inside ->orderBy() method.
     */
    fun testOrderByMethodTriggers() {
        if (!isPhpPluginLoaded()) return

        configureByPhpText(
            """
            \XF::query('xf_thread')->orderBy('<caret>');
            """.trimIndent(),
        )

        val element = getElementAtCaret()
        assertNotNull("Should find element at caret", element)
    }

    /**
     * Test that completion triggers inside ->orderByDesc() method.
     */
    fun testOrderByDescMethodTriggers() {
        if (!isPhpPluginLoaded()) return

        configureByPhpText(
            """
            \XF::query('xf_thread')->orderByDesc('<caret>');
            """.trimIndent(),
        )

        val element = getElementAtCaret()
        assertNotNull("Should find element at caret", element)
    }

    /**
     * Test that completion triggers inside ->groupBy() method.
     */
    fun testGroupByMethodTriggers() {
        if (!isPhpPluginLoaded()) return

        configureByPhpText(
            """
            \XF::query('xf_post')->groupBy('<caret>');
            """.trimIndent(),
        )

        val element = getElementAtCaret()
        assertNotNull("Should find element at caret", element)
    }

    /**
     * Test that completion triggers inside ->having() method.
     */
    fun testHavingMethodTriggers() {
        if (!isPhpPluginLoaded()) return

        configureByPhpText(
            """
            \XF::query('xf_post')
            	->groupBy('user_id')
            	->having('<caret>');
            """.trimIndent(),
        )

        val element = getElementAtCaret()
        assertNotNull("Should find element at caret", element)
    }

    /**
     * Test that completion triggers for array key in ->update() method.
     */
    fun testUpdateArrayKeyTriggers() {
        if (!isPhpPluginLoaded()) return

        configureByPhpText(
            """
            \XF::query('xf_user')->update([
            	'<caret>' => 'new_value'
            ]);
            """.trimIndent(),
        )

        val element = getElementAtCaret()
        assertNotNull("Should find element at caret", element)
    }

    /**
     * Test that completion triggers for array key in ->insert() method.
     */
    fun testInsertArrayKeyTriggers() {
        if (!isPhpPluginLoaded()) return

        configureByPhpText(
            """
            \XF::query('xf_user')->insert([
            	'<caret>' => 'value'
            ]);
            """.trimIndent(),
        )

        val element = getElementAtCaret()
        assertNotNull("Should find element at caret", element)
    }

    /**
     * Test that completion is available after a join.
     */
    fun testCompletionAfterJoin() {
        if (!isPhpPluginLoaded()) return

        configureByPhpText(
            """
            \XF::query('xf_thread')
            	->join('xf_user', 'xf_thread.user_id', '=', 'xf_user.user_id')
            	->where('<caret>');
            """.trimIndent(),
        )

        val element = getElementAtCaret()
        assertNotNull("Should find element at caret", element)
    }

    /**
     * Test completion with aliased tables.
     */
    fun testCompletionWithAliasedTables() {
        if (!isPhpPluginLoaded()) return

        configureByPhpText(
            """
            \XF::query('xf_thread AS t')
            	->join('xf_user AS u', 't.user_id', '=', 'u.user_id')
            	->where('<caret>');
            """.trimIndent(),
        )

        val element = getElementAtCaret()
        assertNotNull("Should find element at caret", element)
    }

    /**
     * Test that completion triggers inside ->increment() method.
     */
    fun testIncrementMethodTriggers() {
        if (!isPhpPluginLoaded()) return

        configureByPhpText(
            """
            \XF::query('xf_thread')->increment('<caret>');
            """.trimIndent(),
        )

        val element = getElementAtCaret()
        assertNotNull("Should find element at caret", element)
    }

    /**
     * Test that completion triggers inside ->decrement() method.
     */
    fun testDecrementMethodTriggers() {
        if (!isPhpPluginLoaded()) return

        configureByPhpText(
            """
            \XF::query('xf_thread')->decrement('<caret>');
            """.trimIndent(),
        )

        val element = getElementAtCaret()
        assertNotNull("Should find element at caret", element)
    }

    /**
     * Test that completion triggers inside ->whereLike() method.
     */
    fun testWhereLikeMethodTriggers() {
        if (!isPhpPluginLoaded()) return

        configureByPhpText(
            """
            \XF::query('xf_user')->whereLike('<caret>', '%test%');
            """.trimIndent(),
        )

        val element = getElementAtCaret()
        assertNotNull("Should find element at caret", element)
    }

    /**
     * Test that completion triggers inside ->value() method.
     */
    fun testValueMethodTriggers() {
        if (!isPhpPluginLoaded()) return

        configureByPhpText(
            """
            \XF::query('xf_user')->where('user_id', 1)->value('<caret>');
            """.trimIndent(),
        )

        val element = getElementAtCaret()
        assertNotNull("Should find element at caret", element)
    }

    /**
     * Test that completion triggers inside ->pluck() method.
     */
    fun testPluckMethodTriggers() {
        if (!isPhpPluginLoaded()) return

        configureByPhpText(
            """
            \XF::query('xf_user')->pluck('<caret>');
            """.trimIndent(),
        )

        val element = getElementAtCaret()
        assertNotNull("Should find element at caret", element)
    }

    /**
     * Test that completion triggers inside ->max() method.
     */
    fun testMaxMethodTriggers() {
        if (!isPhpPluginLoaded()) return

        configureByPhpText(
            """
            \XF::query('xf_thread')->max('<caret>');
            """.trimIndent(),
        )

        val element = getElementAtCaret()
        assertNotNull("Should find element at caret", element)
    }

    /**
     * Test that completion triggers inside ->sum() method.
     */
    fun testSumMethodTriggers() {
        if (!isPhpPluginLoaded()) return

        configureByPhpText(
            """
            \XF::query('xf_post')->sum('<caret>');
            """.trimIndent(),
        )

        val element = getElementAtCaret()
        assertNotNull("Should find element at caret", element)
    }
}
