package app.crimera.patches.duolingo

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.OpcodesFilter
import app.morphe.patcher.fieldAccess
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

// Targets SubscriptionInfo constructor for full subscription object replacement.
// Signature stable: (String, J, Z, I, I, String, String, Z, String) -> V
object SubscriptionInfoConstructorFingerprint : Fingerprint(
    definingClass = "Lcom/duolingo/data/plus/SubscriptionInfo;",
    name = "<init>",
    returnType = "V",
    parameters = listOf(
        "Ljava/lang/String;",
        "J",
        "Z",
        "I",
        "I",
        "Ljava/lang/String;",
        "Ljava/lang/String;",
        "Z",
        "Ljava/lang/String;",
    ),
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.CONSTRUCTOR),
)

// Targets User subscription getter method returning SubscriptionInfo.
// In 6.88: "r". In 6.90.3: "s". In 6.95.4: "r".
// By omitting the method name constraint, it matches any getter on User returning SubscriptionInfo.
object UserSubscriptionInfoFingerprint : Fingerprint(
    definingClass = "Lcom/duolingo/data/user/User;",
    returnType = "Lcom/duolingo/data/plus/SubscriptionInfo;",
    parameters = emptyList(),
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
)

// Targets LoggedIn state wrapper (e.g. mc60/ef20).
// toString prints "LoggedIn(user=...)" - stable across obfuscation.
object LoggedInStateFingerprint : Fingerprint(
    strings = listOf("LoggedIn(user=", ")"),
)

// Targets com/duolingo/data/user/User toString method.
// Used to locate hasPlus (y:Z) and subscriberLevel (A0:SubscriberLevel) fields.
object UserFingerprint : Fingerprint(
    strings = listOf("User(adsConfig=", ", id=", ", betaStatus="),
)

// Targets MaxHooksUserData to locate User hasGold/MAX indicator.
object MaxHooksUserDataFingerprint : Fingerprint(
    strings = listOf(
        "MaxHooksUserData(isAdmin=",
        ", hasMax=",
        ", plusSubscriptionInventoryItem=",
    ),
)

// Locates User hasMax/hasGold field via MaxHooksUserData constructor.
object UserHasGoldFieldUsageFingerprint : Fingerprint(
    classFingerprint = Fingerprint(
        strings = listOf("MaxHooksUserData(isAdmin="),
    ),
    filters = listOf(
        fieldAccess(
            definingClass = "Lcom/duolingo/data/user/User;",
            type = "Z",
        ),
    ),
)

// Targets DebugMenuProvider for debug menu enabling.
object BuildTargetFieldFingerprint : Fingerprint(
    strings = listOf("BUILD_TARGET", "debug", "release"),
    filters = OpcodesFilter.opcodesToFilters(
        Opcode.IGET_OBJECT,
        Opcode.IGET_BOOLEAN,
        Opcode.IF_EQZ,
    ),
)

// Targets VideoCallDebugSettings (e.g. ts60).
// Controls showPromptSelect, showVideoCallTab, completeSessionOnHangup.
object VideoCallDebugSettingsFingerprint : Fingerprint(
    strings = listOf(
        "VideoCallDebugSettings(showPromptSelect=",
        ", showVideoCallTab=",
    ),
)

// Targets VideoCallTabEligibility evaluator (e.g. z570).
object VideoCallTabEligibilityFingerprint : Fingerprint(
    strings = listOf(
        "Video call tab eligibility: ",
        ", hasVideoCallInPath=",
    ),
)
