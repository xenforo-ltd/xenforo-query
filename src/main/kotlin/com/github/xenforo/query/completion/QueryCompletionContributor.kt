package com.github.xenforo.query.completion

import com.intellij.codeInsight.completion.*
import com.intellij.patterns.PlatformPatterns
import com.jetbrains.php.lang.PhpLanguage
import com.jetbrains.php.lang.psi.elements.ParameterList
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression

class QueryCompletionContributor : CompletionContributor()
{
	init
	{
		extend(
			CompletionType.BASIC,
			PlatformPatterns.psiElement()
				.withLanguage(PhpLanguage.INSTANCE)
				.withParent(StringLiteralExpression::class.java)
				.withSuperParent(2, ParameterList::class.java),
			QueryCompletionProvider()
		)
	}
}

