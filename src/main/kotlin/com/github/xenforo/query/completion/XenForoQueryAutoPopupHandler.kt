package com.github.xenforo.query.completion

import com.github.xenforo.query.constants.BuilderMethods
import com.github.xenforo.query.utils.QueryChainResolver
import com.github.xenforo.query.utils.XenForoClassDetector
import com.intellij.codeInsight.AutoPopupController
import com.intellij.codeInsight.editorActions.TypedHandlerDelegate
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.jetbrains.php.lang.PhpLanguage

/** Triggers completion popup when typing a quote character inside XenForo Query Builder method calls. */
class XenForoQueryAutoPopupHandler : TypedHandlerDelegate() {
    override fun checkAutoPopup(charTyped: Char, project: Project, editor: Editor, file: PsiFile): Result {
        if (file.language != PhpLanguage.INSTANCE) return Result.CONTINUE
        if (charTyped != '\'' && charTyped != '"') return Result.CONTINUE

        val offset = editor.caretModel.offset
        if (offset <= 0) return Result.CONTINUE

        val element = file.findElementAt(offset - 1) ?: return Result.CONTINUE
        val method = QueryChainResolver.findMethodReference(element) ?: return Result.CONTINUE
        val methodName = method.name ?: return Result.CONTINUE

        val allMethods = BuilderMethods.TableMethods + BuilderMethods.ColumnMethods + BuilderMethods.ColumnArrayMethods
        if (!allMethods.contains(methodName)) return Result.CONTINUE
        if (!XenForoClassDetector.isXenForoQueryBuilder(method)) return Result.CONTINUE

        AutoPopupController.getInstance(project).scheduleAutoPopup(editor)
        return Result.STOP
    }
}
