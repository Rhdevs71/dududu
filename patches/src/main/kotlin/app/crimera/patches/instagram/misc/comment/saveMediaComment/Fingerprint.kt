/*
 * Copyright (C) 2026 piko <https://github.com/crimera/piko>
 *
 * See the included NOTICE file for GPLv3 §7(b) terms that apply to this code.
 */

package app.crimera.patches.instagram.misc.comment.saveMediaComment

import app.crimera.patches.instagram.entity.commentDataEntity.CHAT_CONTEXT_BUTTON_SUPER_CLASS
import app.crimera.patches.instagram.misc.comment.copyComment.CopyTextChatButtonToStringFingerprint
import app.crimera.patches.instagram.utils.Constants.COMMENT_BUTTON_EXTENSION_CLASS
import app.morphe.patcher.Fingerprint

import com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction

internal const val COMMENT_COPY_EXTENSION_CLASS = "${COMMENT_BUTTON_EXTENSION_CLASS}/saveMediaButton"
internal const val BUTTON_EXTENSION_CLASS = "${COMMENT_COPY_EXTENSION_CLASS}/SaveMediaButton;"

internal const val INIT_BUTTON_EXTENSION_CLASS = "${COMMENT_COPY_EXTENSION_CLASS}/InitSaveMediaButton;"

internal object InitSaveMediaButtonInitExtensionFingerprint : Fingerprint(
    name = "<init>",
    definingClass = INIT_BUTTON_EXTENSION_CLASS,
)

internal object InitSaveMediaButtonExtensionFingerprint : Fingerprint(
    name = "<init>",
    definingClass = BUTTON_EXTENSION_CLASS,
)

internal object SaveMediaChatButtonToStringFingerprint : Fingerprint(
    name = "toString",
    returnType = "Ljava/lang/String;",
    custom = custom@{ methodDef, classDef ->
        if (classDef.superclass != CHAT_CONTEXT_BUTTON_SUPER_CLASS) return@custom false

        val instructions = methodDef.implementation?.instructions ?: return@custom false
        val hasSaveMedia = instructions.any { inst ->
            (inst as? ReferenceInstruction)?.reference?.toString()?.contains("SaveMedia") == true
        }
        if (hasSaveMedia) return@custom true

        val hasConst275 = instructions.any { inst ->
            (inst as? NarrowLiteralInstruction)?.narrowLiteral == 275
        }
        if (hasConst275) return@custom true

        classDef.methods.any { m ->
            m.name == "<init>" && m.implementation?.instructions?.any { inst ->
                (inst as? NarrowLiteralInstruction)?.narrowLiteral == 0x7f08230d
            } == true
        }
    },
)

internal object SaveMediaChatButtonInitFingerprint : Fingerprint(
    classFingerprint = SaveMediaChatButtonToStringFingerprint,
    name = "<init>",
)

