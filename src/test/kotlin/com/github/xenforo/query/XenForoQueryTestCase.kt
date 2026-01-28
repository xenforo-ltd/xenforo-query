package com.github.xenforo.query

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * Base test case for XenForo Query plugin tests.
 *
 * Provides common utilities for testing completion, references, and query chain resolution.
 *
 * Note: Due to the complexity of loading the PHP plugin in test environments,
 * some tests may be integration tests that require manual verification.
 * The PHP PSI structure requires the PHP plugin to be properly initialized.
 */
abstract class XenForoQueryTestCase : BasePlatformTestCase() {
    override fun getTestDataPath(): String = "src/test/resources/testData"

    override fun setUp() {
        super.setUp()
        setupPhpStubs()
    }

    /**
     * Copy PHP stubs to the test project so type resolution works.
     */
    private fun setupPhpStubs() {
        myFixture.copyDirectoryToProject("stubs", "stubs")
    }

    /**
     * Configure the test fixture with PHP code.
     * The file must have .php extension for PHP plugin to parse it correctly.
     */
    protected fun configureByPhpText(text: String) {
        val phpCode =
            if (text.trimStart().startsWith("<?php")) {
                text
            } else {
                "<?php\n$text"
            }
        myFixture.configureByText("test.php", phpCode)
    }

    /**
     * Get completion results as a list of lookup strings.
     */
    protected fun getCompletionStrings(): List<String> {
        val lookupElements = myFixture.completeBasic()
        return lookupElements?.map { it.lookupString } ?: emptyList()
    }

    /**
     * Get raw completion lookup elements.
     */
    protected fun getCompletionElements(): Array<LookupElement>? {
        return myFixture.completeBasic()
    }

    /**
     * Assert that completion contains all expected items.
     */
    protected fun assertCompletionContains(vararg expected: String) {
        val completions = getCompletionStrings()
        expected.forEach { item ->
            assertTrue(
                "Expected completion '$item' not found. Available: $completions",
                completions.contains(item),
            )
        }
    }

    /**
     * Assert that completion does not contain any of the specified items.
     */
    protected fun assertCompletionNotContains(vararg unexpected: String) {
        val completions = getCompletionStrings()
        unexpected.forEach { item ->
            assertFalse(
                "Unexpected completion '$item' was found in: $completions",
                completions.contains(item),
            )
        }
    }

    /**
     * Assert that completion is empty or null.
     */
    protected fun assertNoCompletion() {
        val completions = getCompletionStrings()
        assertTrue(
            "Expected no completions but found: $completions",
            completions.isEmpty(),
        )
    }

    /**
     * Assert that the reference at caret resolves to something.
     */
    protected fun assertReferenceResolves() {
        val ref = myFixture.getReferenceAtCaretPosition()
        assertNotNull("No reference found at caret", ref)
        val resolved = ref?.resolve()
        assertNotNull("Reference did not resolve", resolved)
    }

    /**
     * Assert that there is no reference at the caret position.
     */
    protected fun assertNoReference() {
        val ref = myFixture.getReferenceAtCaretPosition()
        assertTrue(
            "Expected no reference at caret but found: $ref",
            ref == null || ref.resolve() == null,
        )
    }

    /**
     * Get the element at caret position.
     */
    protected fun getElementAtCaret() = myFixture.file.findElementAt(myFixture.caretOffset)

    /**
     * Find a MethodReference in the file at or near the caret position.
     * This searches up the PSI tree from the caret position.
     */
    protected fun findMethodReferenceAtCaret(): com.jetbrains.php.lang.psi.elements.MethodReference? {
        val element = getElementAtCaret() ?: return null
        return com.github.xenforo.query.utils.QueryChainResolver.findMethodReference(element)
    }

    /**
     * Find all MethodReferences in the configured file.
     */
    protected fun findAllMethodReferences(): List<com.jetbrains.php.lang.psi.elements.MethodReference> {
        return com.intellij.psi.util.PsiTreeUtil.findChildrenOfType(
            myFixture.file,
            com.jetbrains.php.lang.psi.elements.MethodReference::class.java,
        ).toList()
    }

    /**
     * Check if PHP plugin is properly loaded and PHP files are being parsed.
     * Returns true if the file is recognized as PHP, false if parsed as plain text.
     */
    protected fun isPhpPluginLoaded(): Boolean {
        configureByPhpText("<?php echo 'test';")
        val fileClass = myFixture.file.javaClass.name
        return fileClass.contains("Php")
    }
}
