package com.github.xenforo.query.completion

import com.github.xenforo.query.utils.QueryChainResolver.isStringLiteralArrayKeyInColumnArray
import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.patterns.PatternCondition
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiElement
import com.intellij.util.ProcessingContext
import com.jetbrains.php.lang.lexer.PhpTokenTypes

class ColumnCompletionContributor : CompletionContributor()
{
	init
	{
		extend(
			CompletionType.BASIC,
			PlatformPatterns.or(
				PlatformPatterns.psiElement(PhpTokenTypes.STRING_LITERAL),
				PlatformPatterns.psiElement(PhpTokenTypes.STRING_LITERAL_SINGLE_QUOTE)
			),
			ColumnCompletionProvider()
		)
		extend(
			CompletionType.BASIC,
			PlatformPatterns.psiElement(PhpTokenTypes.STRING_LITERAL)
				.with(object : PatternCondition<PsiElement>("isArrayKeyInUpdate")
				{
					override fun accepts(t: PsiElement, context: ProcessingContext?): Boolean
					{
						return isStringLiteralArrayKeyInColumnArray(t)
					}
				}),
			ColumnCompletionProvider()
		)
		extend(
			CompletionType.SMART,
			PlatformPatterns.or(
				PlatformPatterns.psiElement(PhpTokenTypes.STRING_LITERAL),
				PlatformPatterns.psiElement(PhpTokenTypes.STRING_LITERAL_SINGLE_QUOTE)
			),
			ColumnCompletionProvider()
		)
	}
}