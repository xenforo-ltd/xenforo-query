package com.github.xenforo.query.model

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement

class DbReferenceExpression(val expression: PsiElement)
{
	val project: Project = expression.project

	val parts = mutableListOf<String>()
	val ranges = mutableListOf<TextRange>()

	init
	{
		// TODO: should move PSI parsing here when more advanced
	}
}