package app.crimera.patches.dmsplus

import app.crimera.patches.dmsplus.Constants.DMSPLUS_COMPATIBILITY
import app.crimera.patches.dmsplus.Constants.ROOT_CHECK_CLASS
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch

@Suppress("unused")
val bypassRootDetectionPatch =
    bytecodePatch(
        name = "Bypass Root & Jailbreak Detection",
        description = "Bypasses DMS+ internal root, su binary, and Magisk detection to allow running on rooted devices and custom ROMs.",
        default = true,
    ) {
        compatibleWith(DMSPLUS_COMPATIBILITY)

        execute {
            // Find root check method via fingerprint or fallback to constant
            val targetMethod =
                try {
                    RootCheckFingerprint.method
                } catch (e: Exception) {
                    val rootClass = mutableClassDefBy(ROOT_CHECK_CLASS)
                    rootClass.methods.first { it.returnType == "Ljava/util/Map;" }
                }

            val targetClass = mutableClassDefBy(targetMethod.definingClass)
            targetClass.methods.first { it.name == targetMethod.name && it.returnType == targetMethod.returnType }.apply {
                addInstructions(
                    0,
                    """
                    new-instance v0, Ljava/util/HashMap;
                    invoke-direct {v0}, Ljava/util/HashMap;-><init>()V
                    const-string v1, "rooted"
                    sget-object v2, Ljava/lang/Boolean;->FALSE:Ljava/lang/Boolean;
                    invoke-virtual {v0, v1, v2}, Ljava/util/HashMap;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
                    const-string v1, "reasons"
                    invoke-static {}, Ljava/util/Collections;->emptyList()Ljava/util/List;
                    move-result-object v2
                    invoke-virtual {v0, v1, v2}, Ljava/util/HashMap;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
                    return-object v0
                    """.trimIndent(),
                )
            }
        }
    }
