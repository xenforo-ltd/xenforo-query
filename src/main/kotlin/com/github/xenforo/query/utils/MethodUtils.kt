package com.github.xenforo.query.utils

import com.intellij.psi.PsiElement
import com.jetbrains.php.lang.psi.elements.MethodReference

object MethodUtils
{
	fun resolveMethodReference(position: PsiElement): MethodReference?
	{
		var current: PsiElement? = position.parent
		var depth = 0

		while (current != null && depth < 10)
		{
			if (current is MethodReference)
			{
				return current
			}
			current = current.parent
			depth++
		}

		return null
	}
}
