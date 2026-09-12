/*
 * Copyright (C) 2026 piko <https://github.com/crimera/piko>
 *
 * See the included NOTICE file for GPLv3 §7(b) terms that apply to this code.
 */

package app.crimera.patches.discord.misc.clone

import app.crimera.patches.discord.Constants.DISCORD_COMPATIBILITY
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.patch.stringOption
import app.morphe.util.asSequence
import app.morphe.util.findElementByAttributeValue
import app.morphe.util.findMutableMethodOf
import app.morphe.util.forEachChildElement
import app.morphe.util.getReference
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction21c
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import com.android.tools.smali.dexlib2.immutable.reference.ImmutableStringReference
import org.w3c.dom.Element

private const val ORIGINAL_PACKAGE_NAME = "com.discord"

@Suppress("unused")
val discordClonePatch =
    resourcePatch(
        name = "Clone",
        description =
            "Changes the package name and app name for Discord. " +
                "This allows you to install the patched Discord alongside the original Discord app.\n" +
                "Default cloned package: com.discord.pikoo",
        default = true,
    ) {
        compatibleWith(DISCORD_COMPATIBILITY)

        val packageName by stringOption(
            key = "packageName",
            default = "com.discord.pikoo",
            title = "Package name",
            description = "A new package name for the patched Discord app.",
            required = true,
        ) {
            it!!.matches(Regex("^[a-z]\\w*(\\.[a-z]\\w*)+$"))
        }

        val appName by stringOption(
            key = "appName",
            default = "Piko Discord",
            title = "App name",
            description = "A new app name (label). Entering \"Discord\" will skip changing the app name.",
            required = true,
        )

        var bytecodePatchContext: BytecodePatchContext? = null

        dependsOn(
            bytecodePatch {
                execute {
                    bytecodePatchContext = this
                }
            },
        )

        execute {
            val newPackageName = packageName!!

            /** Pairs of the original authority and the renamed authority */
            val providerReplacements = mutableListOf<Pair<String, String>>()

            document("AndroidManifest.xml").use { document ->
                val manifest = document.documentElement

                // 1. Change package name
                manifest.setAttribute("package", newPackageName)

                // 2. Rename custom permissions
                val permissions = manifest.getElementsByTagName("permission")
                val usesPermissions = manifest.getElementsByTagName("uses-permission")

                permissions.asSequence().map { it as Element }.forEach {
                    val oldName = it.getAttribute("android:name")
                    if (oldName.startsWith('.')) {
                        return@forEach
                    }
                    if (oldName.startsWith(ORIGINAL_PACKAGE_NAME)) {
                        val newName = oldName.replace(ORIGINAL_PACKAGE_NAME, newPackageName)
                        it.setAttribute("android:name", newName)

                        // Rename corresponding uses-permission
                        usesPermissions
                            .findElementByAttributeValue("android:name", oldName)
                            ?.setAttribute("android:name", newName)
                    }
                }

                // 3. Rename provider authorities (all 8 Discord content providers)
                val providers = manifest.getElementsByTagName("provider").asSequence().map { it as Element }
                for (provider in providers) {
                    val oldAuthority = provider.getAttribute("android:authorities")
                    if (oldAuthority.isNotEmpty()) {
                        val newAuthority =
                            if (oldAuthority.startsWith("$ORIGINAL_PACKAGE_NAME.")) {
                                oldAuthority.replaceFirst(ORIGINAL_PACKAGE_NAME, newPackageName)
                            } else {
                                "${newPackageName}_$oldAuthority"
                            }

                        provider.setAttribute("android:authorities", newAuthority)
                        providerReplacements.add(oldAuthority to newAuthority)
                    }
                }
            }

            // 4. Change app name in strings.xml
            if (!appName.isNullOrEmpty() && appName != "Discord") {
                try {
                    document("res/values/strings.xml").use { document ->
                        document.documentElement.forEachChildElement {
                            if (it.textContent == "Discord") {
                                it.textContent = appName
                            }
                        }
                    }
                } catch (ignored: Exception) {
                }
            }

            // 5. Replace string references in bytecode
            context(bytecodePatchContext!!) {
                transformStringReferences { string ->
                    if (string == ORIGINAL_PACKAGE_NAME) {
                        return@transformStringReferences newPackageName
                    }

                    val matchedReplacement = providerReplacements.find { string.contains(it.first) }
                    if (matchedReplacement != null) {
                        string.replaceFirst(matchedReplacement.first, matchedReplacement.second)
                    } else {
                        null
                    }
                }
            }
            bytecodePatchContext = null
        }
    }

context(patchContext: BytecodePatchContext)
private fun transformStringReferences(transform: (str: String) -> String?) {
    patchContext.getAllClassesWithStrings().forEach { classDef ->
        val mutableClass by lazy {
            patchContext.mutableClassDefBy(classDef)
        }

        classDef.methods.forEach { method ->
            val mutableMethod by lazy {
                mutableClass.findMutableMethodOf(method)
            }

            method.implementation?.instructions?.forEachIndexed { index, instruction ->
                val string =
                    instruction.getReference<StringReference>()?.string
                        ?: return@forEachIndexed

                val transformedString = transform(string) ?: return@forEachIndexed

                mutableMethod.replaceInstruction(
                    index,
                    BuilderInstruction21c(
                        Opcode.CONST_STRING,
                        (instruction as OneRegisterInstruction).registerA,
                        ImmutableStringReference(transformedString),
                    ),
                )
            }
        }
    }
}
