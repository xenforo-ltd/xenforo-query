package com.github.xenforo.query.reference

import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiReferenceContributor
import com.intellij.psi.PsiReferenceRegistrar
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression

class ColumnReferenceContributor : PsiReferenceContributor()
{
	override fun registerReferenceProviders(registrar: PsiReferenceRegistrar)
	{
		registrar.registerReferenceProvider(
			PlatformPatterns.psiElement(StringLiteralExpression::class.java),
			ColumnReferenceProvider()
		)
	}
}
