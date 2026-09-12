/*
 * Copyright (C) 2026 piko <https://github.com/crimera/piko>
 *
 * See the included NOTICE file for GPLv3 §7(b) terms that apply to this code.
 */

package app.crimera.patches.discord.customization

import app.crimera.patches.discord.Constants.DISCORD_COMPATIBILITY
import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch

internal object EmojiPickerRowFingerprint : Fingerprint(
    definingClass = "Lcom/discord/emoji_picker/EmojiPickerRow;",
)

internal object EmojiPickerRowEmojiFingerprint : Fingerprint(
    definingClass = "Lcom/discord/emoji_picker/EmojiPickerRow${'$'}Emoji;",
)

@Suppress("unused")
val unlockNitroFeaturesPatch =
    bytecodePatch(
        name = "Unlock Nitro Emojis & Roadblocks",
        description = "Unlocks custom emojis from all servers in the emoji picker and neutralizes Nitro locked sections.",
        default = true,
    ) {
        compatibleWith(DISCORD_COMPATIBILITY)

        execute {
            // 1. Force isSectionNitroLocked to return false
            EmojiPickerRowFingerprint.classDefOrNull?.methods?.forEach { method ->
                if (method.name == "isSectionNitroLocked" && method.returnType == "Z") {
                    method.addInstructions(
                        0,
                        """
                        const/4 v0, 0
                        return v0
                        """.trimIndent(),
                    )
                }
            }

            // 2. Force getDisabled on all emojis to return false
            EmojiPickerRowEmojiFingerprint.classDefOrNull?.methods?.forEach { method ->
                if (method.name == "getDisabled" && method.returnType == "Z") {
                    method.addInstructions(
                        0,
                        """
                        const/4 v0, 0
                        return v0
                        """.trimIndent(),
                    )
                }
            }
        }
    }
