package com.github.xenforo.query.utils

import com.github.xenforo.query.XenForoQueryTestCase

/**
 * Tests for XenForoClassDetector utility.
 *
 * These tests verify that the type detection correctly identifies XenForo's Query Builder and rejects unrelated
 * classes.
 */
class XenForoClassDetectorTest : XenForoQueryTestCase() {
    /** Test that direct \XF::query() calls are detected. */
    fun testDetectsDirectXfQuery() {
        if (!isPhpPluginLoaded()) return

        configureByPhpText(
            """
            \XF::query('xf_user')->where<caret>('user_id', 1);
            """
                .trimIndent()
        )

        val method = findMethodReferenceAtCaret()
        assertNotNull("Should find method reference", method)
        assertTrue("Should detect XenForo Query Builder", XenForoClassDetector.isXenForoQueryBuilder(method!!))
    }

    /** Test that chained method calls are detected. */
    fun testDetectsChainedMethods() {
        if (!isPhpPluginLoaded()) return

        configureByPhpText(
            """
            \XF::query('xf_user')
            	->where('user_state', 'valid')
            	->orderBy<caret>('username');
            """
                .trimIndent()
        )

        val method = findMethodReferenceAtCaret()
        assertNotNull("Should find method reference", method)
        assertTrue("Should detect XenForo Query Builder in chain", XenForoClassDetector.isXenForoQueryBuilder(method!!))
    }

    /** Test that the query() method on \XF is detected. */
    fun testDetectsXfQueryMethod() {
        if (!isPhpPluginLoaded()) return

        configureByPhpText(
            """
            \XF::query<caret>('xf_user');
            """
                .trimIndent()
        )

        val method = findMethodReferenceAtCaret()
        assertNotNull("Should find method reference", method)
        assertTrue(
            "Should detect \\XF::query() as XenForo Builder entry point",
            XenForoClassDetector.isXenForoQueryBuilder(method!!),
        )
    }

    /** Test that unrelated classes with similar method names are rejected. */
    fun testRejectsUnrelatedClass() {
        if (!isPhpPluginLoaded()) return

        val phpCode =
            """
            class MyQueryBuilder {
            	public function where(${'$'}column, ${'$'}value) { return ${'$'}this; }
            }
            
            ${'$'}builder = new MyQueryBuilder();
            ${'$'}builder->where<caret>('test', 1);
            """
                .trimIndent()

        configureByPhpText(phpCode)

        val method = findMethodReferenceAtCaret()
        assertNotNull("Should find method reference", method)
        assertFalse("Should reject non-XenForo class", XenForoClassDetector.isXenForoQueryBuilder(method!!))
    }

    /** Test that generic where() methods on collections are rejected. */
    fun testRejectsGenericWhereMethod() {
        if (!isPhpPluginLoaded()) return

        val phpCode =
            """
            class Collection {
            	public function where(${'$'}key, ${'$'}value) { return ${'$'}this; }
            }
            
            ${'$'}items = new Collection();
            ${'$'}items->where<caret>('active', true);
            """
                .trimIndent()

        configureByPhpText(phpCode)

        val method = findMethodReferenceAtCaret()
        assertNotNull("Should find method reference", method)
        assertFalse("Should reject generic where() method", XenForoClassDetector.isXenForoQueryBuilder(method!!))
    }

    /** Test that plain function calls without a class are rejected. */
    fun testRejectsPlainFunctionCall() {
        if (!isPhpPluginLoaded()) return

        val phpCode =
            """
            function query(${'$'}table) { return new stdClass(); }
            
            query<caret>('xf_user');
            """
                .trimIndent()

        configureByPhpText(phpCode)

        // This should either not find a MethodReference (since it's a FunctionReference)
        // or the detection should return false
        val element = getElementAtCaret()
        val method = element?.let { QueryChainResolver.findMethodReference(it) }

        if (method != null) {
            assertFalse("Should reject plain function calls", XenForoClassDetector.isXenForoQueryBuilder(method))
        }
        // If method is null, the test passes - function calls aren't MethodReferences
    }

    /** Test detection with the table() method entry point. */
    fun testDetectsTableMethodEntryPoint() {
        if (!isPhpPluginLoaded()) return

        configureByPhpText(
            """
            \XF::query()->table('xf_user')->where<caret>('user_id', 1);
            """
                .trimIndent()
        )

        val method = findMethodReferenceAtCaret()
        assertNotNull("Should find method reference", method)
        assertTrue(
            "Should detect XenForo Builder via table() entry point",
            XenForoClassDetector.isXenForoQueryBuilder(method!!),
        )
    }

    /** Test that deeply chained queries are still detected. */
    fun testDetectsDeeplyChainedQueries() {
        if (!isPhpPluginLoaded()) return

        configureByPhpText(
            """
            \XF::query('xf_thread')
            	->join('xf_user', 'xf_thread.user_id', '=', 'xf_user.user_id')
            	->where('discussion_state', 'visible')
            	->where('sticky', 0)
            	->orderBy('post_date', 'desc')
            	->limit(10)
            	->select<caret>('*');
            """
                .trimIndent()
        )

        val method = findMethodReferenceAtCaret()
        assertNotNull("Should find method reference", method)
        assertTrue("Should detect XenForo Builder in deep chain", XenForoClassDetector.isXenForoQueryBuilder(method!!))
    }

    /**
     * Test detection through variable assignment. This is a common pattern: $query = \XF::query('table');
     * $query->method();
     */
    fun testDetectsXenForoBuilderThroughVariableAssignment() {
        if (!isPhpPluginLoaded()) return

        configureByPhpText(
            """
            ${'$'}query = \XF::query('xf_user');
            ${'$'}query->where<caret>('user_id', 1);
            """
                .trimIndent()
        )

        val method = findMethodReferenceAtCaret()
        assertNotNull("Should find method reference", method)
        assertTrue(
            "Should detect XenForo Builder through variable assignment",
            XenForoClassDetector.isXenForoQueryBuilder(method!!),
        )
    }

    /** Test detection through variable with chained methods on assignment. */
    fun testDetectsXenForoBuilderThroughVariableWithChain() {
        if (!isPhpPluginLoaded()) return

        configureByPhpText(
            """
            ${'$'}query = \XF::query('xf_thread')->join('xf_user', 'xf_thread.user_id', '=', 'xf_user.user_id');
            ${'$'}query->select<caret>('title');
            """
                .trimIndent()
        )

        val method = findMethodReferenceAtCaret()
        assertNotNull("Should find method reference", method)
        assertTrue(
            "Should detect XenForo Builder through variable with chain",
            XenForoClassDetector.isXenForoQueryBuilder(method!!),
        )
    }

    /** Test that variable assignment to non-XenForo builder is rejected. */
    fun testRejectsVariableWithNonXenForoBuilder() {
        if (!isPhpPluginLoaded()) return

        val phpCode =
            """
            class OtherBuilder {
            	public function where(${'$'}col, ${'$'}val) { return ${'$'}this; }
            }
            
            ${'$'}query = new OtherBuilder();
            ${'$'}query->where<caret>('test', 1);
            """
                .trimIndent()

        configureByPhpText(phpCode)

        val method = findMethodReferenceAtCaret()
        assertNotNull("Should find method reference", method)
        assertFalse(
            "Should reject non-XenForo builder through variable",
            XenForoClassDetector.isXenForoQueryBuilder(method!!),
        )
    }

    /** Test detection with variable assigned using table() method. */
    fun testDetectsXenForoBuilderThroughVariableWithTableMethod() {
        if (!isPhpPluginLoaded()) return

        configureByPhpText(
            """
            ${'$'}builder = \XF::query()->table('xf_node');
            ${'$'}builder->where<caret>('node_id', 1);
            """
                .trimIndent()
        )

        val method = findMethodReferenceAtCaret()
        assertNotNull("Should find method reference", method)
        assertTrue(
            "Should detect XenForo Builder through variable with table() method",
            XenForoClassDetector.isXenForoQueryBuilder(method!!),
        )
    }

    /** Test detection inside a closure parameter. */
    fun testDetectsXenForoBuilderInsideClosureParameter() {
        if (!isPhpPluginLoaded()) return

        configureByPhpText(
            """
            \XF::query('xf_post')
            	->where(function (${'$'}query) {
            		${'$'}query->where<caret>('position', '>', 0);
            	});
            """
                .trimIndent()
        )

        val method = findMethodReferenceAtCaret()
        assertNotNull("Should find method reference", method)
        assertTrue(
            "Should detect XenForo Builder inside closure parameter",
            XenForoClassDetector.isXenForoQueryBuilder(method!!),
        )
    }

    /** Test detection inside nested closures. */
    fun testDetectsXenForoBuilderInsideNestedClosures() {
        if (!isPhpPluginLoaded()) return

        configureByPhpText(
            """
            \XF::query('xf_post')
            	->where(function (${'$'}query) {
            		${'$'}query->where('col1', 1)
            			->orWhere(function (${'$'}query) {
            				${'$'}query->where<caret>('col2', 2);
            			});
            	});
            """
                .trimIndent()
        )

        val method = findMethodReferenceAtCaret()
        assertNotNull("Should find method reference", method)
        assertTrue(
            "Should detect XenForo Builder inside nested closures",
            XenForoClassDetector.isXenForoQueryBuilder(method!!),
        )
    }
}
