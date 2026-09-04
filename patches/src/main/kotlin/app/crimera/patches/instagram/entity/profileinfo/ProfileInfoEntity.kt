/*
 * Copyright (C) 2026 piko <https://github.com/crimera/piko>
 *
 * See the included NOTICE file for GPLv3 §7(b) terms that apply to this code.
 */

package app.crimera.patches.instagram.entity.profileinfo

import app.crimera.patches.instagram.utils.Constants.USER_DETAIL_VIEW_MODEL_CLASS
import app.crimera.utils.changeFirstString
import app.morphe.patcher.patch.bytecodePatch

@Suppress("unused")
val profileInfoEntity =
    bytecodePatch(
        description = "Used to decode profile info",
    ) {

        execute {

            ProfileUserInfoViewBinderFingerprint.method.apply {
                mutableClassDefBy(parameters[1].type).apply {
                    val profileRelatedDetailsClass = ProfileRelatedDetailsFingerprint.classDef

                    val profileRelatedDetailsFieldName = fields.last { it.type == profileRelatedDetailsClass.type }.name
                    GetProfileRelatedDetailsExtensionFingerprint.changeFirstString(profileRelatedDetailsFieldName)

                    val userDetailViewModelFieldName =
                        fields
                            .last { it.type == USER_DETAIL_VIEW_MODEL_CLASS }
                            .name
                    GetUserDetailViewModelExtensionFingerprint.changeFirstString(userDetailViewModelFieldName)

                    val isSelfProfileFieldName =
                        profileRelatedDetailsClass.fields
                            .last { it.type == "Z" }
                            .name
                    IsSelfProfileExtensionFingerprint.changeFirstString(isSelfProfileFieldName)
                }
            }

            val userDetailViewModelClass = mutableClassDefBy(USER_DETAIL_VIEW_MODEL_CLASS)
            val userObjectFieldName =
                userDetailViewModelClass.fields
                    .firstOrNull { it.type.endsWith("/User;") }
                    ?.name
                    ?: userDetailViewModelClass.fields.first { it.type.contains("User") }.name
            GetUserDataExtensionFingerprint.changeFirstString(userObjectFieldName)
        }
    }
