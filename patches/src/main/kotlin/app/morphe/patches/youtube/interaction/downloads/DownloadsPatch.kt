/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * Original hard forked code:
 * https://github.com/ReVanced/revanced-patches/commit/724e6d61b2ecd868c1a9a37d465a688e83a74799
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to Morphe contributions.
 */

package app.morphe.patches.youtube.interaction.downloads

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.morphe.patcher.util.registersUsed
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patches.shared.misc.fix.proto.fixProtoLibraryPatch
import app.morphe.patches.shared.misc.fix.proto.parseByteArrayWithRegistryMethodRef
import app.morphe.patches.shared.misc.proto.hookElement
import app.morphe.patches.shared.misc.settings.preference.ListPreference
import app.morphe.patches.shared.misc.settings.preference.NonInteractivePreference
import app.morphe.patches.shared.misc.settings.preference.PreferenceScreenPreference
import app.morphe.patches.shared.misc.settings.preference.PreferenceScreenPreference.Sorting
import app.morphe.patches.shared.misc.settings.preference.SwitchPreference
import app.morphe.patches.shared.misc.settings.preference.TextPreference
import app.morphe.patches.youtube.layout.flyout.flyoutPatch
import app.morphe.patches.youtube.layout.player.icons.copyPlayerButtonIcons
import app.morphe.patches.youtube.misc.auth.authHookPatch
import app.morphe.patches.youtube.misc.playercontrols.addTopControl
import app.morphe.patches.youtube.misc.playercontrols.initializeTopControl
import app.morphe.patches.youtube.misc.playercontrols.legacyPlayerControlsPatch
import app.morphe.patches.youtube.misc.proto.elementProtoParserHookPatch
import app.morphe.patches.youtube.misc.settings.PreferenceScreen
import app.morphe.patches.youtube.misc.settings.settingsPatch
import app.morphe.patches.youtube.misc.spoof.spoofVideoStreamsPatch
import app.morphe.patches.youtube.shared.Constants.COMPATIBILITY_YOUTUBE
import app.morphe.patches.youtube.video.information.videoInformationPatch
import app.morphe.util.ResourceGroup
import app.morphe.util.copyResources
import app.morphe.util.indexOfFirstInstructionOrThrow
import app.morphe.util.setExtensionIsPatchIncluded
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter
import java.util.logging.Logger

private val downloadsResourcePatch = resourcePatch {
    dependsOn(
        legacyPlayerControlsPatch,
        settingsPatch
    )

    execute {
        PreferenceScreen.PLAYER.addPreferences(
            PreferenceScreenPreference(
                key = "morphe_external_downloader_screen",
                sorting = Sorting.UNSORTED,
                preferences = setOf(
                    ListPreference(
                        key = "morphe_download_mode",
                        entriesKey = "morphe_download_mode_youtube_entries",
                        entryValuesKey = "morphe_download_mode_youtube_entry_values"
                    ),
                    SwitchPreference("morphe_offline_replace_downloads_page"),
                    NonInteractivePreference(
                        key = "morphe_offline_open_library",
                        tag = "app.morphe.extension.youtube.settings.preference.OpenOfflineLibraryPreference",
                        selectable = true
                    ),
                    SwitchPreference("morphe_external_downloader_overlay_button", summary = true),
                    TextPreference(
                        "morphe_external_downloader_name",
                        tag = "app.morphe.extension.shared.settings.preference.ExternalDownloaderPreference",
                    ),
                    SwitchPreference("morphe_external_downloader_quick_menu", summary = true),
                    SwitchPreference("morphe_external_downloader_mark_watched", summary = true),
                    SwitchPreference("morphe_shorts_download_flyout_button", summary = true)
                )
            )
        )

        copyPlayerButtonIcons("downloads", "morphe_yt_download_button")

        copyResources(
            "downloads",
            ResourceGroup(
                "drawable",
                "morphe_external_downloader_quick_video.xml",
                "morphe_external_downloader_quick_video_bold.xml",
                "morphe_external_downloader_quick_audio.xml",
                "morphe_external_downloader_quick_audio_bold.xml"
            )
        )
    }

    finalize {
        addTopControl("downloads",
            "@+id/morphe_external_download_button",
            "@+id/morphe_external_download_button")
    }
}

private const val EXTENSION_CLASS = "Lapp/morphe/extension/youtube/patches/DownloadsPatch;"

private const val EXTENSION_IN_APP_DOWNLOADS_CLASS = "Lapp/morphe/extension/youtube/patches/InAppDownloadsPatch;"

private const val EXTENSION_SPOOF_CLASS = "Lapp/morphe/extension/shared/spoof/SpoofVideoStreamsPatch;"

private const val EXTENSION_SHORTS_DOWNLOAD_BUTTON =
    "Lapp/morphe/extension/youtube/videoplayer/ShortsDownloadButton;"

private const val EXTENSION_OFFLINE_SERVICE_HOOKS =
    "Lapp/morphe/extension/youtube/patches/offline/OfflineServiceHooks;"

private const val EXTENSION_OFFLINE_SCREENS =
    "Lapp/morphe/extension/youtube/patches/offline/OfflineScreens;"

private const val EXTENSION_BUTTON = "Lapp/morphe/extension/youtube/videoplayer/ExternalDownloadButton;"

private const val GET_GENERATED_REGISTRY =
    "Lcom/google/protobuf/ExtensionRegistryLite;->getGeneratedRegistry()Lcom/google/protobuf/ExtensionRegistryLite;"

@Suppress("unused")
val downloadsPatch = bytecodePatch(
    name = "Downloads",
    description = "Adds support to download videos with an external downloader app " +
        "using the in-app download button or a video player action button, " +
        "and adds options to download videos in-app or save them to the device.",
) {
    dependsOn(
        downloadsResourcePatch,
        authHookPatch,
        videoInformationPatch,
        legacyPlayerControlsPatch,
        fixProtoLibraryPatch,
        spoofVideoStreamsPatch,
        elementProtoParserHookPatch,
        flyoutPatch
    )

    compatibleWith(COMPATIBILITY_YOUTUBE)

    execute {
        initializeTopControl(EXTENSION_BUTTON)

        setExtensionIsPatchIncluded(EXTENSION_SHORTS_DOWNLOAD_BUTTON)

        OfflineVideoEndpointFingerprint.method.apply {
            addInstructionsWithLabels(
                0,
                """
                    invoke-static/range { p3 .. p3 }, $EXTENSION_CLASS->inAppDownloadButtonOnClick(Ljava/lang/String;)Z
                    move-result v0
                    if-eqz v0, :show_native_downloader
                    return-void
                    :show_native_downloader
                    nop
                """
            )
        }

        // Open the external downloader with the playlist link, instead of saving the playlist.
        OfflinePlaylistEndpointFingerprint.method.addInstructionsWithLabels(
            0,
            """
                # The parameter registers can be higher than 15.
                move-object/from16 v0, p3
                move-object/from16 v1, p4
                invoke-static { v0, v1 }, $EXTENSION_CLASS->offlinePlaylistEndpointOnClick(Ljava/lang/String;Ljava/lang/Enum;)Z
                move-result v0
                if-eqz v0, :show_native_downloader
                return-void
                :show_native_downloader
                nop
            """
        )

        // Add the Download button to the playlist page headers, which only Premium accounts get.
        hookElement("$EXTENSION_IN_APP_DOWNLOADS_CLASS->addPlaylistDownloadButton")

        // region In-app downloads.

        // Save the video instead of asking the server for the download action, which answers with the Premium upsell.
        ResolveCommandFingerprint.method.apply {
            val commandType = parameterTypes.first().toString()

            addInstructionsWithLabels(
                0,
                """
                    # The parameter register can be higher than 15.
                    invoke-static/range { p1 .. p1 }, $EXTENSION_IN_APP_DOWNLOADS_CLASS->replaceDownloadActionCommand(Lcom/google/protobuf/MessageLite;)[B
                    move-result-object v0
                    if-eqz v0, :keep_command

                    # The endpoint is an extension, which is lost if parsed without the registry.
                    sget-object v1, $commandType->a:$commandType
                    invoke-static { }, $GET_GENERATED_REGISTRY
                    move-result-object v2
                    invoke-static { v1, v0, v2 }, ${parseByteArrayWithRegistryMethodRef.get()!!}
                    move-result-object v0
                    check-cast v0, $commandType
                    move-object/from16 p1, v0
                    :keep_command
                    nop
                """
            )
        }

        // Unlock the qualities of the video, and save the video instead of showing the Premium upsell.
        SaveOfflineVideoCallFingerprint.instructionMatches.first().getMethodCalled().apply {
            val offlineabilityType = parameterTypes[1].toString()

            addInstructionsWithLabels(
                0,
                """
                    # The parameter register can be higher than 15.
                    invoke-static/range { p2 .. p2 }, $EXTENSION_IN_APP_DOWNLOADS_CLASS->unlockOfflineability(Ljava/lang/Object;)[B
                    move-result-object v0
                    if-eqz v0, :keep_offlineability

                    sget-object v1, $offlineabilityType->a:$offlineabilityType
                    invoke-static { }, $GET_GENERATED_REGISTRY
                    move-result-object v2
                    invoke-static { v1, v0, v2 }, ${parseByteArrayWithRegistryMethodRef.get()!!}
                    move-result-object v0
                    check-cast v0, $offlineabilityType
                    move-object/from16 p2, v0
                    :keep_offlineability
                    nop
                """
            )
        }

        // Save the video instead of showing the Premium upsell of the offline upsell endpoint.
        OfflineUpsellResponseFingerprint.let {
            val responseType = it.instructionMatches.first().instruction.getReference<TypeReference>()!!.type
            val commandField = it.instructionMatches[2].instruction.getReference<FieldReference>()!!

            it.method.addInstructionsWithLabels(
                0,
                """
                    # The parameter registers can be higher than 15.
                    move-object/from16 v0, p0
                    iget-object v0, v0, $commandField
                    move-object/from16 v1, p1
                    invoke-static { v0, v1 }, $EXTENSION_IN_APP_DOWNLOADS_CLASS->replaceOfflineUpsell(Lcom/google/protobuf/MessageLite;Ljava/lang/Object;)[B
                    move-result-object v0
                    if-eqz v0, :keep_upsell_response

                    # The endpoint of the command is an extension, which is lost if parsed without the registry.
                    sget-object v1, $responseType->a:$responseType
                    invoke-static { }, $GET_GENERATED_REGISTRY
                    move-result-object v2
                    invoke-static { v1, v0, v2 }, ${parseByteArrayWithRegistryMethodRef.get()!!}
                    move-result-object v0
                    move-object/from16 p1, v0
                    :keep_upsell_response
                    nop
                """
            )
        }

        // Save the video with streams of another client, if the server declines to save the video.
        OfflinePlayerResponseFingerprint.let {
            val playerResponseModelType = it.method.returnType

            val fromBytesFingerprint = playerResponseModelFromBytesFingerprint(playerResponseModelType)
            val fromBytesMethod = fromBytesFingerprint.method
            val playerResponseType = fromBytesFingerprint.instructionMatches.last()
                .instruction.getReference<TypeReference>()!!.type
            val playerResponseGetter = playerResponseGetterFingerprint(
                fromBytesMethod.definingClass,
                playerResponseType
            ).method

            val helperMethod = ImmutableMethod(
                it.classDef.type,
                "patch_replaceDeclinedOfflinePlayerResponse",
                listOf(ImmutableMethodParameter(playerResponseModelType, null, null)),
                playerResponseModelType,
                AccessFlags.PUBLIC.value or AccessFlags.STATIC.value,
                null,
                null,
                MutableMethodImplementation(4),
            ).toMutable().apply {
                addInstructionsWithLabels(
                    0,
                    """
                        if-eqz p0, :keep_player_response
                        invoke-interface { p0 }, $playerResponseModelType->${playerResponseGetter.name}()$playerResponseType
                        move-result-object v0
                        invoke-static { v0 }, $EXTENSION_IN_APP_DOWNLOADS_CLASS->replaceDeclinedOfflinePlayerResponse(Ljava/lang/Object;)[B
                        move-result-object v0
                        if-eqz v0, :keep_player_response

                        # The playback stream override would replace the streams that can be saved.
                        const/4 v1, 0x1
                        invoke-static { v1 }, $EXTENSION_SPOOF_CLASS->setKeepStreamsOnThread(Z)V

                        # The response time, used for the expiration of the stream urls.
                        invoke-static { }, Ljava/lang/System;->currentTimeMillis()J
                        move-result-wide v1
                        invoke-static { v0, v1, v2 }, $fromBytesMethod
                        move-result-object v0

                        invoke-static { v0 }, $EXTENSION_IN_APP_DOWNLOADS_CLASS->onOfflinePlayerResponseReplaced(Ljava/lang/Object;)V
                        if-eqz v0, :keep_player_response
                        return-object v0

                        :keep_player_response
                        return-object p0
                    """
                )
            }
            it.classDef.methods.add(helperMethod)

            // Replace the player response before it is cached and returned.
            val cachePut = it.instructionMatches.last()
            val playerResponseModelRegister = cachePut.instruction.registersUsed[2]

            it.method.addInstructions(
                cachePut.index,
                """
                    invoke-static/range { v$playerResponseModelRegister .. v$playerResponseModelRegister }, $helperMethod
                    move-result-object v$playerResponseModelRegister
                """
            )
        }

        // endregion

        // region Device downloader.

        // Every hook is optional: a missing match only disables that part and is logged.
        fun hookOptional(name: String, block: () -> Unit) {
            runCatching(block).onFailure {
                Logger.getLogger(this::class.java.name).warning("$name: ${it.message}")
            }
        }

        // Returns early if the call returns true, otherwise continues with the original code.
        // A null register means the first local register, which the original code assigns before it reads it.
        fun MutableMethod.skipIfTrue(index: Int, call: String, scratch: String?, skip: String) {
            val register = scratch ?: run {
                if (implementation!!.registerCount - parameters.size - 1 < 1) {
                    throw PatchException("$name has no local register")
                }
                "v0"
            }
            addInstructionsWithLabels(
                index,
                """
                    $call
                    move-result $register
                    if-eqz $register, :original_code
                    $skip
                """,
                ExternalLabel("original_code", getInstruction(index))
            )
        }

        // BackgroundPlayerService is the only stock service with the media playback foreground type.
        // The offline player uses it with its own intent actions.
        hookOptional("BackgroundPlayerService.onCreate") {
            BackgroundPlayerServiceOnCreateFingerprint.method.apply {
                // After the injection in the super call, before the setup of the app.
                val index = indexOfFirstInstructionOrThrow(Opcode.INVOKE_SUPER) + 1
                skipIfTrue(
                    index,
                    "invoke-static { p0 }, $EXTENSION_OFFLINE_SERVICE_HOOKS->onPlaybackServiceCreate(Landroid/app/Service;)Z",
                    null,
                    "return-void"
                )
            }
        }
        hookOptional("BackgroundPlayerService.onStartCommand") {
            BackgroundPlayerServiceOnStartCommandFingerprint.method.skipIfTrue(
                0,
                // The flags parameter is not used by the original code, so it is free as a scratch register.
                "invoke-static { p0, p1, p3 }, $EXTENSION_OFFLINE_SERVICE_HOOKS->onPlaybackServiceStartCommand(Landroid/app/Service;Landroid/content/Intent;I)Z",
                "p2",
                """
                    const/4 p2, 0x2
                    return p2
                """
            )
        }
        hookOptional("BackgroundPlayerService.onBind") {
            BackgroundPlayerServiceOnBindFingerprint.method.addInstruction(
                0,
                "invoke-static { p0 }, $EXTENSION_OFFLINE_SERVICE_HOOKS->onPlaybackServiceBind(Landroid/app/Service;)V"
            )
        }
        hookOptional("BackgroundPlayerService.onTaskRemoved") {
            BackgroundPlayerServiceOnTaskRemovedFingerprint.method.skipIfTrue(
                0,
                "invoke-static { p0 }, $EXTENSION_OFFLINE_SERVICE_HOOKS->onPlaybackServiceTaskRemoved(Landroid/app/Service;)Z",
                null,
                "return-void"
            )
        }
        hookOptional("BackgroundPlayerService.onDestroy") {
            BackgroundPlayerServiceOnDestroyFingerprint.let {
                it.method.skipIfTrue(
                    0,
                    "invoke-static { p0 }, $EXTENSION_OFFLINE_SERVICE_HOOKS->onPlaybackServiceDestroy(Landroid/app/Service;)Z",
                    null,
                    """
                        invoke-super { p0 }, ${it.classDef.superclass}->onDestroy()V
                        return-void
                    """
                )
            }
        }

        // OfflineKeepAliveService (data sync foreground type) is only used by the in-app downloads.
        // The device downloader uses it to keep the process alive while downloading.
        hookOptional("OfflineKeepAliveService.onCreate") {
            OfflineKeepAliveServiceOnCreateFingerprint.let {
                val serviceType = it.classDef.type
                it.method.apply {
                    // Before the foreground setup, after the field injection.
                    val index = indexOfFirstInstructionOrThrow {
                        val reference = getReference<MethodReference>()
                        opcode == Opcode.INVOKE_DIRECT &&
                            reference?.definingClass == serviceType &&
                            reference.name != "<init>" &&
                            reference.parameterTypes.isEmpty()
                    }
                    skipIfTrue(
                        index,
                        "invoke-static { p0 }, $EXTENSION_OFFLINE_SERVICE_HOOKS->onKeepAliveServiceCreate(Landroid/app/Service;)Z",
                        null,
                        "return-void"
                    )
                }
            }
        }
        hookOptional("OfflineKeepAliveService.onStartCommand") {
            OfflineKeepAliveServiceOnStartCommandFingerprint.method.skipIfTrue(
                0,
                "invoke-static { p0, p1, p3 }, $EXTENSION_OFFLINE_SERVICE_HOOKS->onKeepAliveServiceStartCommand(Landroid/app/Service;Landroid/content/Intent;I)Z",
                "p2",
                """
                    const/4 p2, 0x2
                    return p2
                """
            )
        }
        hookOptional("OfflineKeepAliveService.onDestroy") {
            val method = OfflineKeepAliveServiceOnDestroyFingerprint.methodOrNull
            if (method != null) {
                method.skipIfTrue(
                    0,
                    "invoke-static { p0 }, $EXTENSION_OFFLINE_SERVICE_HOOKS->onKeepAliveServiceDestroy(Landroid/app/Service;)Z",
                    null,
                    """
                        invoke-super { p0 }, ${OfflineKeepAliveServiceOnDestroyFingerprint.classDef.superclass}->onDestroy()V
                        return-void
                    """
                )
            } else {
                // Some versions do not override onDestroy.
                OfflineKeepAliveServiceOnCreateFingerprint.classDef.apply {
                    ImmutableMethod(
                        type,
                        "onDestroy",
                        emptyList(),
                        "V",
                        AccessFlags.PUBLIC.value,
                        null,
                        null,
                        MutableMethodImplementation(1)
                    ).toMutable().apply {
                        addInstructions(
                            0,
                            """
                                invoke-static { p0 }, $EXTENSION_OFFLINE_SERVICE_HOOKS->onKeepAliveServiceDestroy(Landroid/app/Service;)Z
                                invoke-super { p0 }, $superclass->onDestroy()V
                                return-void
                            """
                        )
                    }.let(methods::add)
                }
            }
        }
        // Service.onTimeout exists since Android 15, older targets do not override it.
        OfflineKeepAliveServiceOnTimeoutFingerprint.methodOrNull?.let { method ->
            hookOptional("OfflineKeepAliveService.onTimeout") {
                method.skipIfTrue(
                    0,
                    "invoke-static { p0 }, $EXTENSION_OFFLINE_SERVICE_HOOKS->onKeepAliveServiceTimeout(Landroid/app/Service;)Z",
                    null,
                    "return-void"
                )
            }
        }

        // Replace the Downloads page: cancel the navigation to "FEdownloads"
        // and open the saved videos on top of the current page instead.
        hookOptional("Browse endpoint command") {
            BrowseEndpointCommandFingerprint.let {
                it.method.apply {
                    val constIndex = it.instructionMatches.first().index
                    val constRegister = getInstruction<OneRegisterInstruction>(constIndex).registerA
                    val equalsIndex = indexOfFirstInstructionOrThrow(constIndex) {
                        opcode == Opcode.INVOKE_VIRTUAL &&
                            getReference<MethodReference>()?.name == "equals"
                    }
                    val equalsInstruction = getInstruction<FiveRegisterInstruction>(equalsIndex)
                    val browseIdRegister = if (equalsInstruction.registerC == constRegister) {
                        equalsInstruction.registerD
                    } else {
                        equalsInstruction.registerC
                    }
                    // The const string overwrites the register next, so it is free here.
                    skipIfTrue(
                        constIndex,
                        "invoke-static/range { v$browseIdRegister .. v$browseIdRegister }, $EXTENSION_OFFLINE_SCREENS->openLibraryInsteadOfBrowse(Ljava/lang/String;)Z",
                        "v$constRegister",
                        "return-void"
                    )
                }
            }
        }

        // The library row of the You tab opens the Downloads page with its own command,
        // which never reaches the browse command handler. The command resolver sees the
        // commands of taps, links and rows, so both forms are replaced there.
        hookOptional("Command resolver") {
            ResolveCommandFingerprint.method.skipIfTrue(
                0,
                "invoke-static/range { p1 .. p1 }, $EXTENSION_OFFLINE_SCREENS->openLibraryInsteadOfCommand(Ljava/lang/Object;)Z",
                null,
                "return-void"
            )
        }

        // endregion
    }
}
