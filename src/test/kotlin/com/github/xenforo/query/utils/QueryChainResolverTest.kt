package com.github.xenforo.query.utils

import com.github.xenforo.query.XenForoQueryTestCase
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.php.lang.psi.elements.MethodReference
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression
import com.jetbrains.php.lang.psi.elements.Variable

/**
 * Tests for QueryChainResolver utility class.
 *
 * These tests verify that the chain resolver correctly extracts table information
 * from XenForo query builder method chains.
 *
 * Note: These tests require the PHP plugin to be loaded. If the PHP plugin is not
 * available (e.g., in CI without full IDE), tests will be skipped.
 */
class QueryChainResolverTest : XenForoQueryTestCase() {
    override fun setUp() {
        super.setUp()
        // Skip tests if PHP plugin isn't loaded
        if (!isPhpPluginLoaded()) {
            println("PHP plugin not loaded, skipping test")
        }
    }

    /**
     * Test resolving a simple query with single table.
     */
    fun testResolvesSimpleQueryTable() {
        if (!isPhpPluginLoaded()) return

        configureByPhpText(
            """
            \XF::query('xf_user')->where('user_id', 1);
            """.trimIndent(),
        )

        val whereMethod = findMethodByName("where")
        assertNotNull("Should find where method", whereMethod)

        val tables = QueryChainResolver.resolveTables(whereMethod!!)

        assertEquals("Should find 1 table", 1, tables.size)
        assertEquals("xf_user", tables[0].baseTable)
        assertNull("Should have no alias", tables[0].alias)
    }

    /**
     * Test resolving a table with AS alias syntax.
     */
    fun testResolvesTableWithAlias() {
        if (!isPhpPluginLoaded()) return

        configureByPhpText(
            """
            \XF::query('xf_user AS u')->where('user_id', 1);
            """.trimIndent(),
        )

        val whereMethod = findMethodByName("where")
        val tables = QueryChainResolver.resolveTables(whereMethod!!)

        assertEquals(1, tables.size)
        assertEquals("xf_user", tables[0].baseTable)
        assertEquals("u", tables[0].alias)
    }

    /**
     * Test resolving a table with space alias syntax (no AS keyword).
     */
    fun testResolvesTableWithSpaceAlias() {
        if (!isPhpPluginLoaded()) return

        configureByPhpText(
            """
            \XF::query('xf_user u')->where('user_id', 1);
            """.trimIndent(),
        )

        val whereMethod = findMethodByName("where")
        val tables = QueryChainResolver.resolveTables(whereMethod!!)

        assertEquals(1, tables.size)
        assertEquals("xf_user", tables[0].baseTable)
        assertEquals("u", tables[0].alias)
    }

    /**
     * Test resolving with a single JOIN.
     */
    fun testResolvesJoinedTables() {
        if (!isPhpPluginLoaded()) return

        configureByPhpText(
            """
            \XF::query('xf_thread')
            	->join('xf_user', 'xf_thread.user_id', '=', 'xf_user.user_id')
            	->where('user_id', 1);
            """.trimIndent(),
        )

        val whereMethod = findMethodByName("where")
        val tables = QueryChainResolver.resolveTables(whereMethod!!)

        assertEquals("Should find 2 tables", 2, tables.size)
        assertEquals("xf_thread", tables[0].baseTable)
        assertEquals("xf_user", tables[1].baseTable)
        assertEquals("JOIN", tables[1].joinType)
    }

    /**
     * Test resolving LEFT JOIN.
     */
    fun testResolvesLeftJoin() {
        if (!isPhpPluginLoaded()) return

        configureByPhpText(
            """
            \XF::query('xf_thread')
            	->leftJoin('xf_node', 'xf_thread.node_id', '=', 'xf_node.node_id')
            	->where('node_id', 1);
            """.trimIndent(),
        )

        val whereMethod = findMethodByName("where")
        val tables = QueryChainResolver.resolveTables(whereMethod!!)

        assertEquals(2, tables.size)
        assertEquals("LEFT JOIN", tables[1].joinType)
    }

    /**
     * Test resolving RIGHT JOIN.
     */
    fun testResolvesRightJoin() {
        if (!isPhpPluginLoaded()) return

        configureByPhpText(
            """
            \XF::query('xf_thread')
            	->rightJoin('xf_node', 'xf_thread.node_id', '=', 'xf_node.node_id')
            	->where('node_id', 1);
            """.trimIndent(),
        )

        val whereMethod = findMethodByName("where")
        val tables = QueryChainResolver.resolveTables(whereMethod!!)

        assertEquals(2, tables.size)
        assertEquals("RIGHT JOIN", tables[1].joinType)
    }

    /**
     * Test resolving multiple JOINs.
     */
    fun testResolvesMultipleJoins() {
        if (!isPhpPluginLoaded()) return

        configureByPhpText(
            """
            \XF::query('xf_thread AS t')
            	->join('xf_user AS u', 't.user_id', '=', 'u.user_id')
            	->leftJoin('xf_node AS n', 't.node_id', '=', 'n.node_id')
            	->where('t.thread_id', 1);
            """.trimIndent(),
        )

        val whereMethod = findMethodByName("where")
        val tables = QueryChainResolver.resolveTables(whereMethod!!)

        assertEquals("Should find 3 tables", 3, tables.size)

        assertEquals("xf_thread", tables[0].baseTable)
        assertEquals("t", tables[0].alias)

        assertEquals("xf_user", tables[1].baseTable)
        assertEquals("u", tables[1].alias)
        assertEquals("JOIN", tables[1].joinType)

        assertEquals("xf_node", tables[2].baseTable)
        assertEquals("n", tables[2].alias)
        assertEquals("LEFT JOIN", tables[2].joinType)
    }

    /**
     * Test that ->table() method is recognized.
     */
    fun testResolvesTableMethod() {
        if (!isPhpPluginLoaded()) return

        configureByPhpText(
            """
            \XF::query()->table('xf_post')->where('post_id', 1);
            """.trimIndent(),
        )

        val whereMethod = findMethodByName("where")
        val tables = QueryChainResolver.resolveTables(whereMethod!!)

        assertEquals(1, tables.size)
        assertEquals("xf_post", tables[0].baseTable)
    }

    /**
     * Test chain traversal works through many chained methods.
     */
    fun testChainTraversalNotBroken() {
        if (!isPhpPluginLoaded()) return

        configureByPhpText(
            """
            \XF::query('xf_thread')
            	->where('discussion_state', 'visible')
            	->where('sticky', 0)
            	->orderBy('post_date')
            	->limit(10)
            	->select('title');
            """.trimIndent(),
        )

        val selectMethod = findMethodByName("select")
        val tables = QueryChainResolver.resolveTables(selectMethod!!)

        assertEquals("Should still find the base table despite long chain", 1, tables.size)
        assertEquals("xf_thread", tables[0].baseTable)
    }

    /**
     * Test finding method reference works.
     */
    fun testFindMethodReference() {
        if (!isPhpPluginLoaded()) return

        configureByPhpText(
            """
            \XF::query('xf_user')->where('user_id<caret>', 1);
            """.trimIndent(),
        )

        val element = getElementAtCaret()
        assertNotNull("Should find element at caret", element)

        val method = QueryChainResolver.findMethodReference(element!!)
        assertNotNull("Should find method reference", method)
        assertEquals("where", method?.name)
    }

    /**
     * Test that isStringLiteralArrayKeyInColumnArray correctly identifies array keys.
     */
    fun testIsArrayKeyDetection() {
        if (!isPhpPluginLoaded()) return

        configureByPhpText(
            """
            \XF::query('xf_user')->update([
            	'username' => 'new_value'
            ]);
            """.trimIndent(),
        )

        // Find the 'username' string literal (the key)
        val stringLiterals = PsiTreeUtil.findChildrenOfType(myFixture.file, StringLiteralExpression::class.java)
        val keyLiteral = stringLiterals.find { it.contents == "username" }
        assertNotNull("Should find username string literal", keyLiteral)

        val result = QueryChainResolver.isStringLiteralArrayKeyInColumnArray(keyLiteral!!)
        assertTrue("Should detect string as array key", result)
    }

    /**
     * Test that isStringLiteralArrayKeyInColumnArray returns false for array values.
     */
    fun testIsArrayValueDetection() {
        if (!isPhpPluginLoaded()) return

        configureByPhpText(
            """
            \XF::query('xf_user')->update([
            	'username' => 'new_value'
            ]);
            """.trimIndent(),
        )

        // Find the 'new_value' string literal (the value)
        val stringLiterals = PsiTreeUtil.findChildrenOfType(myFixture.file, StringLiteralExpression::class.java)
        val valueLiteral = stringLiterals.find { it.contents == "new_value" }
        assertNotNull("Should find new_value string literal", valueLiteral)

        val result = QueryChainResolver.isStringLiteralArrayKeyInColumnArray(valueLiteral!!)
        assertFalse("Should not detect string as array key when it's a value", result)
    }

    /**
     * Test resolving with lowercase 'as' keyword.
     */
    fun testResolvesTableWithLowercaseAs() {
        if (!isPhpPluginLoaded()) return

        configureByPhpText(
            """
            \XF::query('xf_user as u')->where('user_id', 1);
            """.trimIndent(),
        )

        val whereMethod = findMethodByName("where")
        val tables = QueryChainResolver.resolveTables(whereMethod!!)

        assertEquals(1, tables.size)
        assertEquals("xf_user", tables[0].baseTable)
        assertEquals("u", tables[0].alias)
    }

    /**
     * Test with empty query (no table).
     */
    fun testEmptyQuery() {
        if (!isPhpPluginLoaded()) return

        configureByPhpText(
            """
            \XF::query()->where('col', 1);
            """.trimIndent(),
        )

        val whereMethod = findMethodByName("where")
        val tables = QueryChainResolver.resolveTables(whereMethod!!)

        assertTrue("Should return empty list when no table specified", tables.isEmpty())
    }

    /**
     * Test resolving tables through variable assignment.
     * This is a common pattern: $query = \XF::query('table'); $query->method();
     */
    fun testResolvesTablesThroughVariableAssignment() {
        if (!isPhpPluginLoaded()) return

        configureByPhpText(
            """
            ${'$'}query = \XF::query('xf_post');
            ${'$'}query->where('post_id', 1);
            """.trimIndent(),
        )

        val whereMethod = findMethodByName("where")
        assertNotNull("Should find where method", whereMethod)

        val tables = QueryChainResolver.resolveTables(whereMethod!!)

        assertEquals("Should find 1 table through variable", 1, tables.size)
        assertEquals("xf_post", tables[0].baseTable)
    }

    /**
     * Test resolving tables through variable with chained methods on assignment.
     */
    fun testResolvesTablesThroughVariableWithChain() {
        if (!isPhpPluginLoaded()) return

        configureByPhpText(
            """
            ${'$'}query = \XF::query('xf_thread')->join('xf_user', 'xf_thread.user_id', '=', 'xf_user.user_id');
            ${'$'}query->where('thread_id', 1);
            """.trimIndent(),
        )

        val whereMethod = findMethodByName("where")
        assertNotNull("Should find where method", whereMethod)

        val tables = QueryChainResolver.resolveTables(whereMethod!!)

        assertEquals("Should find 2 tables through variable", 2, tables.size)
        assertEquals("xf_thread", tables[0].baseTable)
        assertEquals("xf_user", tables[1].baseTable)
    }

    /**
     * Test resolving tables when variable is reassigned (uses latest assignment before usage).
     */
    fun testResolvesTablesFromLatestVariableAssignment() {
        if (!isPhpPluginLoaded()) return

        configureByPhpText(
            """
            ${'$'}query = \XF::query('xf_user');
            ${'$'}query = \XF::query('xf_post');
            ${'$'}query->where('post_id', 1);
            """.trimIndent(),
        )

        val whereMethod = findMethodByName("where")
        assertNotNull("Should find where method", whereMethod)

        val tables = QueryChainResolver.resolveTables(whereMethod!!)

        assertEquals("Should find 1 table", 1, tables.size)
        assertEquals("Should use latest assignment (xf_post)", "xf_post", tables[0].baseTable)
    }

    /**
     * Test resolving tables with variable and table() method.
     */
    fun testResolvesTablesThroughVariableWithTableMethod() {
        if (!isPhpPluginLoaded()) return

        configureByPhpText(
            """
            ${'$'}builder = \XF::query()->table('xf_node');
            ${'$'}builder->select('node_id');
            """.trimIndent(),
        )

        val selectMethod = findMethodByName("select")
        assertNotNull("Should find select method", selectMethod)

        val tables = QueryChainResolver.resolveTables(selectMethod!!)

        assertEquals("Should find 1 table through variable with table()", 1, tables.size)
        assertEquals("xf_node", tables[0].baseTable)
    }

    /**
     * Test resolving tables inside a closure parameter.
     * This is a common pattern for complex where conditions.
     */
    fun testResolvesTablesInsideClosureParameter() {
        if (!isPhpPluginLoaded()) return

        configureByPhpText(
            """
            \XF::query('xf_post')
            	->where(function (${'$'}query) {
            		${'$'}query->where('position', '>', 0);
            	});
            """.trimIndent(),
        )

        // Find the inner where method (inside the closure)
        val methods = findAllMethodReferences()
        val innerWhere =
            methods.find {
                it.name == "where" && it.parameterList?.parameters?.getOrNull(0)?.text?.contains("position") == true
            }
        assertNotNull("Should find inner where method", innerWhere)

        val tables = QueryChainResolver.resolveTables(innerWhere!!)

        assertEquals("Should find 1 table through closure parameter", 1, tables.size)
        assertEquals("xf_post", tables[0].baseTable)
    }

    /**
     * Test resolving tables inside nested closures.
     */
    fun testResolvesTablesInsideNestedClosures() {
        if (!isPhpPluginLoaded()) return

        configureByPhpText(
            """
            \XF::query('xf_post')
            	->where(function (${'$'}query) {
            		${'$'}query->where('col1', 1)
            			->orWhere(function (${'$'}query) {
            				${'$'}query->where('col2', 2);
            			});
            	});
            """.trimIndent(),
        )

        // Find the innermost where method (col2)
        val methods = findAllMethodReferences()
        val innerWhere =
            methods.find {
                it.name == "where" && it.parameterList?.parameters?.getOrNull(0)?.text?.contains("col2") == true
            }
        assertNotNull("Should find innermost where method", innerWhere)

        val tables = QueryChainResolver.resolveTables(innerWhere!!)

        assertEquals("Should find 1 table through nested closure", 1, tables.size)
        assertEquals("xf_post", tables[0].baseTable)
    }

    /**
     * Test resolving tables inside closure with joins.
     */
    fun testResolvesTablesInsideClosureWithJoins() {
        if (!isPhpPluginLoaded()) return

        configureByPhpText(
            """
            \XF::query('xf_thread')
            	->join('xf_user', 'xf_thread.user_id', '=', 'xf_user.user_id')
            	->where(function (${'$'}query) {
            		${'$'}query->where('user_state', 'valid');
            	});
            """.trimIndent(),
        )

        // Find the inner where method
        val methods = findAllMethodReferences()
        val innerWhere =
            methods.find {
                it.name == "where" && it.parameterList?.parameters?.getOrNull(0)?.text?.contains("user_state") == true
            }
        assertNotNull("Should find inner where method", innerWhere)

        val tables = QueryChainResolver.resolveTables(innerWhere!!)

        assertEquals("Should find 2 tables through closure with joins", 2, tables.size)
        assertEquals("xf_thread", tables[0].baseTable)
        assertEquals("xf_user", tables[1].baseTable)
    }

    /**
     * Test forward chain resolution: joins defined AFTER select are still resolved.
     * This verifies the resolver looks forward in the chain, not just backward.
     */
    fun testResolvesJoinAfterSelect() {
        if (!isPhpPluginLoaded()) return

        configureByPhpText(
            """
            \XF::query('xf_thread')
                ->select('thread_id', 'title')
                ->join('xf_user', 'xf_thread.user_id', '=', 'xf_user.user_id')
                ->where('thread_id', 1);
            """.trimIndent(),
        )

        // Find the select method - join comes AFTER it
        val selectMethod = findMethodByName("select")
        assertNotNull("Should find select method", selectMethod)

        val tables = QueryChainResolver.resolveTables(selectMethod!!)

        // Should find both tables even though join comes after select
        assertEquals("Should find 2 tables including join after select", 2, tables.size)
        assertEquals("xf_thread", tables[0].baseTable)
        assertEquals("xf_user", tables[1].baseTable)
        assertEquals("JOIN", tables[1].joinType)
    }

    /**
     * Test forward chain resolution: multiple joins at different positions work.
     * Joins can be scattered throughout the chain and should all be found.
     */
    fun testResolvesMultipleJoinsAfterSelect() {
        if (!isPhpPluginLoaded()) return

        configureByPhpText(
            """
            \XF::query('xf_thread AS t')
                ->select('t.thread_id', 't.title')
                ->join('xf_user AS u', 't.user_id', '=', 'u.user_id')
                ->where('t.discussion_state', 'visible')
                ->leftJoin('xf_node AS n', 't.node_id', '=', 'n.node_id')
                ->orderBy('t.post_date')
                ->rightJoin('xf_attachment AS a', 't.thread_id', '=', 'a.content_id')
                ->limit(10);
            """.trimIndent(),
        )

        // Find the select method - joins are both before and after it
        val selectMethod = findMethodByName("select")
        assertNotNull("Should find select method", selectMethod)

        val tables = QueryChainResolver.resolveTables(selectMethod!!)

        // Should find all 4 tables
        assertEquals("Should find 4 tables with multiple joins", 4, tables.size)

        assertEquals("xf_thread", tables[0].baseTable)
        assertEquals("t", tables[0].alias)

        assertEquals("xf_user", tables[1].baseTable)
        assertEquals("u", tables[1].alias)
        assertEquals("JOIN", tables[1].joinType)

        assertEquals("xf_node", tables[2].baseTable)
        assertEquals("n", tables[2].alias)
        assertEquals("LEFT JOIN", tables[2].joinType)

        assertEquals("xf_attachment", tables[3].baseTable)
        assertEquals("a", tables[3].alias)
        assertEquals("RIGHT JOIN", tables[3].joinType)
    }

    /**
     * Test forward chain resolution: join after where works.
     * This is a common pattern where conditions are added before joins.
     */
    fun testResolvesJoinAfterWhere() {
        if (!isPhpPluginLoaded()) return

        configureByPhpText(
            """
            \XF::query('xf_post AS p')
                ->where('p.message_state', 'visible')
                ->where('p.post_date', '>', 1234567890)
                ->join('xf_thread AS t', 'p.thread_id', '=', 't.thread_id')
                ->orderBy('p.post_date', 'DESC');
            """.trimIndent(),
        )

        // Find the first where method - join comes AFTER it
        val methods = findAllMethodReferences()
        val firstWhere =
            methods.find {
                it.name == "where" &&
                    it.parameterList?.parameters?.getOrNull(0)?.text?.contains("message_state") == true
            }
        assertNotNull("Should find first where method", firstWhere)

        val tables = QueryChainResolver.resolveTables(firstWhere!!)

        // Should find both tables even though join comes after where
        assertEquals("Should find 2 tables including join after where", 2, tables.size)
        assertEquals("xf_post", tables[0].baseTable)
        assertEquals("p", tables[0].alias)
        assertEquals("xf_thread", tables[1].baseTable)
        assertEquals("t", tables[1].alias)
        assertEquals("JOIN", tables[1].joinType)
    }

    /**
     * Helper method to find a MethodReference by method name.
     */
    private fun findMethodByName(name: String): MethodReference? {
        val methods = findAllMethodReferences()
        return methods.find { it.name == name }
    }

    /**
     * Test that isStringLiteralArrayValueInColumnArray correctly identifies array values
     * in the second and third arguments of upsert.
     */
    fun testIsArrayValueInUpsertSecondArg() {
        if (!isPhpPluginLoaded()) return

        configureByPhpText(
            """
            \XF::query('xf_user')
                ->upsert(
                    ['username' => 'test'],
                    ['username', 'email'],
                    ['last_seen']
                );
            """.trimIndent(),
        )

        // Find the 'username' string literal in the second argument (uniqueBy array)
        val stringLiterals = PsiTreeUtil.findChildrenOfType(myFixture.file, StringLiteralExpression::class.java)
        val secondArgValues = stringLiterals.filter { it.contents == "username" || it.contents == "email" }

        // At least one should be detected as a value in the second argument
        val foundValue =
            secondArgValues.any { lit ->
                QueryChainResolver.isStringLiteralArrayValueInColumnArray(lit)
            }

        assertTrue("Should detect strings as array values in upsert second arg", foundValue)
    }

    /**
     * Test that isStringLiteralArrayKeyInColumnArray returns false for nested array keys
     * inside json_encode() calls.
     */
    fun testNestedJsonEncodeKeysNotDetected() {
        if (!isPhpPluginLoaded()) return

        configureByPhpText(
            """
            \XF::query('xf_user')
                ->upsert(
                    [
                        'username' => 'test',
                        'data' => json_encode(['nested_key' => 'value']),
                    ],
                    ['username']
                );
            """.trimIndent(),
        )

        // Find the 'nested_key' string literal
        val stringLiterals = PsiTreeUtil.findChildrenOfType(myFixture.file, StringLiteralExpression::class.java)
        val nestedKey = stringLiterals.find { it.contents == "nested_key" }
        assertNotNull("Should find nested_key string literal", nestedKey)

        // Should NOT be detected as a column array key
        val isKey = QueryChainResolver.isStringLiteralArrayKeyInColumnArray(nestedKey!!)
        assertFalse("Should not detect nested json_encode key as array key", isKey)

        // Should NOT be detected as a column array value
        val isValue = QueryChainResolver.isStringLiteralArrayValueInColumnArray(nestedKey)
        assertFalse("Should not detect nested json_encode key as array value", isValue)
    }

    /**
     * Test getColumnArrayPosition for various positions.
     */
    fun testGetColumnArrayPosition() {
        if (!isPhpPluginLoaded()) return

        configureByPhpText(
            """
            \XF::query('xf_user')
                ->upsert(
                    ['username' => 'test'],
                    ['username'],
                    ['last_seen']
                );
            """.trimIndent(),
        )

        val stringLiterals = PsiTreeUtil.findChildrenOfType(myFixture.file, StringLiteralExpression::class.java)

        // Find 'username' in first argument (should be KEY)
        val firstArgKey =
            stringLiterals.find {
                it.contents == "username" &&
                    QueryChainResolver.getColumnArrayPosition(it) == QueryChainResolver.ColumnArrayPosition.KEY
            }
        assertNotNull("Should find username as KEY in first arg", firstArgKey)

        // Find 'username' in second argument (should be VALUE)
        val secondArgValue =
            stringLiterals.find {
                it.contents == "username" &&
                    QueryChainResolver.getColumnArrayPosition(it) == QueryChainResolver.ColumnArrayPosition.VALUE
            }
        assertNotNull("Should find username as VALUE in second arg", secondArgValue)

        // Find 'last_seen' in third argument (should be VALUE)
        val thirdArgValue =
            stringLiterals.find {
                it.contents == "last_seen" &&
                    QueryChainResolver.getColumnArrayPosition(it) == QueryChainResolver.ColumnArrayPosition.VALUE
            }
        assertNotNull("Should find last_seen as VALUE in third arg", thirdArgValue)
    }

    /**
     * Test enhanced closure resolution: join inside closure is resolved.
     * This verifies that when a join is added inside a closure, it's available
     * for completions/inspections within that closure.
     */
    fun testResolvesJoinInsideClosure() {
        if (!isPhpPluginLoaded()) return

        configureByPhpText(
            """
            \XF::query('xf_thread AS t')
                ->where(function (${'$'}query) {
                    ${'$'}query->join('xf_user AS u', 't.user_id', '=', 'u.user_id');
                    ${'$'}query->where('u.username', 'admin');
                });
            """.trimIndent(),
        )

        // Find the inner where method that uses the joined table
        // We need to find the where method whose classReference is a Variable (inside closure),
        // not the outer where whose classReference is a MethodReference
        val methods = findAllMethodReferences()
        val innerWhere =
            methods.find {
                it.name == "where" &&
                    it.classReference is Variable &&
                    it.parameterList?.parameters?.getOrNull(0)?.text?.contains("username") == true
            }
        assertNotNull("Should find inner where method", innerWhere)

        val tables = QueryChainResolver.resolveTables(innerWhere!!)

        // Should find both the outer table and the joined table from inside the closure
        assertEquals("Should find 2 tables including join from closure", 2, tables.size)

        // Verify we have both tables
        val threadTable = tables.find { it.baseTable == "xf_thread" }
        val userTable = tables.find { it.baseTable == "xf_user" }

        assertNotNull("Should find xf_thread from outer chain", threadTable)
        assertNotNull("Should find xf_user from closure join", userTable)
        assertEquals("t", threadTable?.alias)
        assertEquals("u", userTable?.alias)
    }

    /**
     * Test enhanced closure resolution: nested closures with joins at different levels.
     * Verifies that joins from both outer and inner closures are all available.
     */
    fun testResolvesNestedClosuresWithJoins() {
        if (!isPhpPluginLoaded()) return

        configureByPhpText(
            """
            \XF::query('xf_post AS p')
                ->where(function (${'$'}query) {
                    ${'$'}query->join('xf_thread AS t', 'p.thread_id', '=', 't.thread_id');
                    ${'$'}query->where(function (${'$'}query) {
                        ${'$'}query->join('xf_user AS u', 't.user_id', '=', 'u.user_id');
                        ${'$'}query->where('u.username', 'admin');
                    });
                });
            """.trimIndent(),
        )

        // Find the innermost where that uses 'username'
        // We need to find the where method whose classReference is a Variable (inside closure),
        // not the outer where methods whose classReference is a MethodReference
        val methods = findAllMethodReferences()
        val innermostWhere =
            methods.find {
                it.name == "where" &&
                    it.classReference is Variable &&
                    it.parameterList?.parameters?.getOrNull(0)?.text?.contains("username") == true
            }
        assertNotNull("Should find innermost where method", innermostWhere)

        val tables = QueryChainResolver.resolveTables(innermostWhere!!)

        // Should find all 3 tables from the nested closures
        assertEquals("Should find 3 tables from nested closures", 3, tables.size)

        val postTable = tables.find { it.baseTable == "xf_post" }
        val threadTable = tables.find { it.baseTable == "xf_thread" }
        val userTable = tables.find { it.baseTable == "xf_user" }

        assertNotNull("Should find xf_post", postTable)
        assertNotNull("Should find xf_thread from outer closure", threadTable)
        assertNotNull("Should find xf_user from inner closure", userTable)
    }

    /**
     * Test that resolveClosureWithTables returns null for non-closure variables.
     * This ensures we don't incorrectly treat regular variables as closures.
     */
    fun testResolveClosureWithTablesReturnsNullForNonClosures() {
        if (!isPhpPluginLoaded()) return

        configureByPhpText(
            """
            ${'$'}name = 'test';
            \XF::query('xf_user')->where('username', ${'$'}name);
            """.trimIndent(),
        )

        // Find the Variable for ${'$'}name (which is NOT a closure parameter)
        val variables = PsiTreeUtil.findChildrenOfType(myFixture.file, Variable::class.java)
        val nameVar = variables.find { it.name == "name" && it.text == "${'$'}name" }
        assertNotNull("Should find ${'$'}name variable", nameVar)

        // Should return null since it's not a closure parameter
        val result = QueryChainResolver.resolveClosureWithTables(nameVar!!)
        assertNull("Should return null for non-closure variable", result)
    }
}
