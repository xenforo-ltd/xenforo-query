package com.github.xenforo.query.completion

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.patterns.PlatformPatterns
import com.jetbrains.php.lang.lexer.PhpTokenTypes

class TableCompletionContributor : CompletionContributor()
{
	init
	{
		extend(
			CompletionType.BASIC,
			PlatformPatterns.or(
				PlatformPatterns.psiElement(PhpTokenTypes.STRING_LITERAL),
				PlatformPatterns.psiElement(PhpTokenTypes.STRING_LITERAL_SINGLE_QUOTE)
			),
			TableCompletionProvider()
		)
	}
}