/*
 * Copyright (C) 2026 piko <https://github.com/crimera/piko>
 *
 * See the included NOTICE file for GPLv3 §7(b) terms that apply to this code.
 */

package app.crimera.patches.spotify.misc.clone

import app.crimera.patches.spotify.utils.Constants.COMPATIBILITY_SPOTIFY
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

private const val ORIGINAL_PACKAGE_NAME = "com.spotify.music"

@Suppress("unused")
val spotifyClonePatch =
    resourcePatch(
        name = "Spotify Clone",
        description =
            "Changes the package name and app name for Spotify. " +
                "This allows you to install the patched Spotify alongside the original Spotify app.\n" +
                "Default cloned package: com.spotify.music.pikoo",
        default = true,
    ) {
        compatibleWith(COMPATIBILITY_SPOTIFY)

        val packageName by stringOption(
            key = "packageName",
            default = "com.spotify.music.pikoo",
            title = "Package name",
            description = "A new package name for the patched Spotify app.",
            required = true,
        ) {
            it!!.matches(Regex("^[a-z]\\w*(\\.[a-z]\\w*)+$"))
        }

        val appName by stringOption(
            key = "appName",
            default = "Piko Spotify",
            title = "App name",
            description = "A new app name (label). Entering \"Spotify\" will skip changing the app name.",
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

                        usesPermissions
                            .findElementByAttributeValue("android:name", oldName)
                            ?.setAttribute("android:name", newName)
                    }
                }

                // 3. Rename authorities
                val providers = manifest.getElementsByTagName("provider")

                providers.asSequence().map { it as Element }.forEach {
                    val oldAuthority = it.getAttribute("android:authorities")

                    if (oldAuthority.startsWith(ORIGINAL_PACKAGE_NAME)) {
                        val newAuthority = oldAuthority.replace(ORIGINAL_PACKAGE_NAME, newPackageName)
                        it.setAttribute("android:authorities", newAuthority)
                        providerReplacements.add(oldAuthority to newAuthority)
                    }
                }

                // 4. Rename application name
                if (appName != null && appName != "Spotify") {
                    val application = manifest.getElementsByTagName("application").item(0) as? Element
                    application?.setAttribute("android:label", appName)
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
