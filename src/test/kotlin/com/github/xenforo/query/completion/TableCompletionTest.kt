package com.github.xenforo.query.completion

import com.github.xenforo.query.XenForoQueryTestCase

/**
 * Tests for table name completion in XenForo Query Builder methods.
 *
 * Note: These tests verify the completion provider logic. Full integration tests
 * require both the PHP plugin and a database connection to be available.
 */
class TableCompletionTest : XenForoQueryTestCase() {
    /**
     * Test that completion triggers inside XF::query() first argument.
     */
    fun testQueryMethodTriggers() {
        if (!isPhpPluginLoaded()) return

        configureByPhpText(
            """
            \XF::query('<caret>');
            """.trimIndent(),
        )

        val element = getElementAtCaret()
        assertNotNull("Should find element at caret", element)
    }

    /**
     * Test that completion triggers inside ->table() method.
     */
    fun testTableMethodTriggers() {
        if (!isPhpPluginLoaded()) return

        configureByPhpText(
            """
            \XF::query()->table('<caret>');
            """.trimIndent(),
        )

        val element = getElementAtCaret()
        assertNotNull("Should find element at caret", element)
    }

    /**
     * Test that completion triggers inside ->join() first argument.
     */
    fun testJoinMethodTriggers() {
        if (!isPhpPluginLoaded()) return

        configureByPhpText(
            """
            \XF::query('xf_thread')
            	->join('<caret>', 'xf_thread.user_id', '=', 'xf_user.user_id');
            """.trimIndent(),
        )

        val element = getElementAtCaret()
        assertNotNull("Should find element at caret", element)
    }

    /**
     * Test that completion triggers inside ->leftJoin() first argument.
     */
    fun testLeftJoinMethodTriggers() {
        if (!isPhpPluginLoaded()) return

        configureByPhpText(
            """
            \XF::query('xf_thread')
            	->leftJoin('<caret>', 'xf_thread.node_id', '=', 'xf_node.node_id');
            """.trimIndent(),
        )

        val element = getElementAtCaret()
        assertNotNull("Should find element at caret", element)
    }

    /**
     * Test that completion triggers inside ->rightJoin() first argument.
     */
    fun testRightJoinMethodTriggers() {
        if (!isPhpPluginLoaded()) return

        configureByPhpText(
            """
            \XF::query('xf_thread')
            	->rightJoin('<caret>', 'xf_thread.node_id', '=', 'xf_node.node_id');
            """.trimIndent(),
        )

        val element = getElementAtCaret()
        assertNotNull("Should find element at caret", element)
    }

    /**
     * Test that table completion works with table alias syntax.
     */
    fun testQueryWithAliasSyntax() {
        if (!isPhpPluginLoaded()) return

        configureByPhpText(
            """
            \XF::query('<caret> AS t');
            """.trimIndent(),
        )

        val element = getElementAtCaret()
        assertNotNull("Should find element at caret", element)
    }

    /**
     * Test completion in a longer chain.
     */
    fun testCompletionInLongChain() {
        if (!isPhpPluginLoaded()) return

        configureByPhpText(
            """
            \XF::query('xf_thread')
            	->where('discussion_state', 'visible')
            	->join('<caret>', 'xf_thread.user_id', '=', 'xf_user.user_id')
            	->orderBy('post_date')
            	->limit(10);
            """.trimIndent(),
        )

        val element = getElementAtCaret()
        assertNotNull("Should find element at caret", element)
    }

    /**
     * Test that from() method also triggers table completion.
     */
    fun testFromMethodTriggers() {
        if (!isPhpPluginLoaded()) return

        configureByPhpText(
            """
            \XF::query()->from('<caret>');
            """.trimIndent(),
        )

        val element = getElementAtCaret()
        assertNotNull("Should find element at caret", element)
    }
}
