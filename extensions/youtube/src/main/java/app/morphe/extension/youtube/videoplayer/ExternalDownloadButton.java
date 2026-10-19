/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * Original hard forked code:
 * https://github.com/ReVanced/revanced-patches/commit/724e6d61b2ecd868c1a9a37d465a688e83a74799
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to Morphe contributions.
 */

package app.morphe.extension.youtube.videoplayer;

import static app.morphe.extension.shared.StringRef.str;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RippleDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import app.morphe.extension.shared.Logger;
import app.morphe.extension.shared.ResourceUtils;
import app.morphe.extension.shared.settings.SharedYouTubeSettings;
import app.morphe.extension.shared.settings.preference.ExternalDownloaderPreference;
import app.morphe.extension.shared.theme.ThemeUtils;
import app.morphe.extension.shared.ui.Dim;
import app.morphe.extension.shared.ui.SheetBottomDialog;
import app.morphe.extension.youtube.patches.VideoInformation;
import app.morphe.extension.youtube.shared.PipDismissHelper;

@SuppressWarnings("unused")
public class ExternalDownloadButton {

    static {
        if (SharedYouTubeSettings.EXTERNAL_DOWNLOADER.get()) {
            LegacyPlayerControlButton.incrementUpperButtonCount();
        }
    }

    /**
     * Injection point.
     */
    public static void initializeLegacyButton(View controlsView) {
        try {
            new LegacyPlayerControlButton(
                    controlsView,
                    "morphe_external_download_button",
                    null,
                    "morphe_yt_download_button",
                    SharedYouTubeSettings.EXTERNAL_DOWNLOADER,
                    ExternalDownloadButton::onDownloadClick,
                    ExternalDownloadButton::onDownloadLongClick
            );
        } catch (Exception ex) {
            Logger.printException(() -> "initializeButton failure", ex);
        }
    }

    private static String getVideoUrl(String videoId) {
        return "https://youtu.be/" + videoId;
    }

    private static void onDownloadClick(View view) {
        final String videoId = VideoInformation.getVideoId();
        ExternalDownloaderPreference.launchExternalDownloader(videoId, view.getContext(), getVideoUrl(videoId));
    }

    /**
     * Tap and hold opens a quick download menu,
     * if the chosen downloader can queue downloads in the background.
     */
    private static boolean onDownloadLongClick(View view) {
        if (!ExternalDownloaderPreference.supportsQuickDownload()) {
            return false;
        }
        showQuickDownloadMenu(view.getContext());
        return true;
    }

    private static void showQuickDownloadMenu(Context context) {
        try {
            // Capture the video now, the player may move on while the menu is open.
            final String videoId = VideoInformation.getVideoId();
            final String url = getVideoUrl(videoId);

            SheetBottomDialog.DraggableLinearLayout mainLayout =
                    SheetBottomDialog.createMainLayout(context, LegacyPlayerControlButton.getDialogBackgroundColor());

            TextView title = new TextView(context);
            title.setText(str("morphe_external_downloader_quick_menu_title"));
            title.setTextColor(ThemeUtils.getAppForegroundColor());
            title.setTextSize(16);
            title.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            title.setPadding(Dim.dp12, Dim.dp16, Dim.dp12, Dim.dp8);
            mainLayout.addView(title);

            SheetBottomDialog.SlideDialog dialog =
                    SheetBottomDialog.createSlideDialog(context, mainLayout, LegacyPlayerControlButton.fadeInDuration);

            mainLayout.addView(createItem(context,
                    "morphe_external_downloader_quick_video",
                    "morphe_external_downloader_quick_video", () -> {
                        dialog.dismiss();
                        ExternalDownloaderPreference.launchExternalDownloader(videoId, context, url, "video");
                    }));
            mainLayout.addView(createItem(context,
                    "morphe_external_downloader_quick_audio",
                    "morphe_external_downloader_quick_audio", () -> {
                        dialog.dismiss();
                        ExternalDownloaderPreference.launchExternalDownloader(videoId, context, url, "audio");
                    }));
            mainLayout.addView(createItem(context,
                    "morphe_external_downloader_quick_options",
                    "morphe_external_downloader_quick_options", () -> {
                        dialog.dismiss();
                        ExternalDownloaderPreference.launchExternalDownloader(videoId, context, url);
                    }));

            PipDismissHelper.dismissOnPip(dialog);
            dialog.show();
        } catch (Exception ex) {
            Logger.printException(() -> "showQuickDownloadMenu failure", ex);
        }
    }

    private static View createItem(Context context, String iconName, String textKey, Runnable onClick) {
        final int foregroundColor = ThemeUtils.getAppForegroundColor();

        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(Dim.dp48);
        row.setPadding(Dim.dp12, 0, Dim.dp12, 0);
        row.setClickable(true);
        row.setFocusable(true);
        row.setBackground(new RippleDrawable(
                ColorStateList.valueOf((foregroundColor & 0x00FFFFFF) | 0x33000000),
                null,
                new ColorDrawable(0xFFFFFFFF)));
        row.setOnClickListener(v -> onClick.run());

        Drawable icon = getIcon(context, iconName);
        if (icon != null) {
            icon.setTint(foregroundColor);
            ImageView iconView = new ImageView(context);
            iconView.setImageDrawable(icon);
            LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(Dim.dp24, Dim.dp24);
            iconParams.setMarginEnd(Dim.dp20);
            row.addView(iconView, iconParams);
        }

        TextView text = new TextView(context);
        text.setText(str(textKey));
        text.setTextColor(foregroundColor);
        text.setTextSize(16);
        row.addView(text, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        return row;
    }

    @Nullable
    private static Drawable getIcon(Context context, String name) {
        try {
            final int id = ResourceUtils.getDrawableIdentifier(name);
            if (id == 0) return null;
            Drawable drawable = context.getDrawable(id);
            return drawable == null ? null : drawable.mutate();
        } catch (Exception ex) {
            Logger.printDebug(() -> "Icon not found: " + name);
            return null;
        }
    }
}
