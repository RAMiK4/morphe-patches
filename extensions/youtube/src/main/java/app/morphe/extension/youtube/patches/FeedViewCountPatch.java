/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */

package app.morphe.extension.youtube.patches;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.icu.text.CompactDecimalFormat;
import android.text.Spannable;
import android.text.Spanned;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.facebook.litho.ComponentHost;
import com.facebook.litho.TextContent;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import app.morphe.extension.shared.Logger;
import app.morphe.extension.youtube.patches.utils.FeedVideoCards;
import app.morphe.extension.youtube.settings.Settings;

/**
 * Shows the view count of feed videos at the bottom left of their thumbnails,
 * lined up with the duration badge at the bottom right.
 * <p>
 * The view count is the text YouTube shows below the title, such as
 * "Channel · 4.7M views · 2 days ago". When that text is created, the shortened view count
 * is attached to it as a span, so the card that draws the text knows its view count.
 * The span is copied with the text, so it is kept when the Hide view count option
 * of the Hide layout components patch removes the view count from below the title.
 * No request is made, and the count is the same that YouTube shows.
 */
@SuppressWarnings("unused")
public final class FeedViewCountPatch {

    public static final boolean ENABLED = isPatchIncluded() && Settings.FEED_VIEW_COUNT.get();

    /** Delimiter of the texts below the title. */
    private static final String DELIMITER = " · ";

    /**
     * A number, such as 457, 4,7 or 1 234. Some languages show words before it.
     */
    private static final Pattern NUMBER_PATTERN =
            Pattern.compile("\\p{Nd}+(?:[.,'\u2019\u066B\u066C\u00A0\u202F ]\\p{Nd}+)*");

    /** Different words of view counts that were seen, such as "views" and "watching". */
    private static final int MAX_VIEW_WORDS = 8;

    private static final int FALLBACK_MARGIN_DP = 8;
    private static final int FALLBACK_HEIGHT_DP = 20;
    /** A duration badge larger than this is not the badge itself, but a host around it. */
    private static final int MAX_DURATION_WIDTH_DP = 120;
    private static final int MAX_DURATION_HEIGHT_DP = 32;
    private static final int MIN_DURATION_HEIGHT_DP = 12;
    private static final int PADDING_DP = 5;
    private static final int GAP_DP = 3;
    private static final int RADIUS_DP = 4;
    private static final float TEXT_SP = 12f;
    private static final float ICON_STROKE_DP = 1.4f;

    private static final int BACKGROUND_COLOR = 0xB3000000;
    private static final int TEXT_COLOR = 0xFFFFFFFF;

    private static final Pattern DURATION_PATTERN =
            Pattern.compile("(?:^|[^\\p{L}\\p{N}])\\d{1,2}(?:[:.]\\d{2}){1,2}$");
    private static final int MAX_DURATION_TEXT_LENGTH = 12;
    /** A short badge in capital letters at the same place, such as the live badge. */
    private static final Pattern BADGE_PATTERN = Pattern.compile("^\\p{Lu}{2,12}$");

    /** Shortened number units of the language of the app, such as K and M, longest first. */
    @Nullable
    private static List<String> units;
    @Nullable
    private static Locale unitsLocale;

    /** Used on more than one thread. */
    private static final Set<String> viewWords = new LinkedHashSet<>();

    /** Root hosts that show a view count. Used only on the main thread. */
    private static final Map<ComponentHost, ViewCountBadge> badges = new WeakHashMap<>();

    public static final FeedVideoCards.Listener LISTENER = FeedViewCountPatch::onCardLaidOut;

    private FeedViewCountPatch() {
    }

    public static boolean isPatchIncluded() {
        return false; // Modified during patching.
    }

    /**
     * Shortened view count attached to the texts below the title. Not a {@link android.text.NoCopySpan},
     * so it is copied with the text. Equal by value, so texts with it are still equal
     * to the same texts created again.
     */
    private static final class ViewCountSpan {
        final String viewCount;

        ViewCountSpan(String viewCount) {
            this.viewCount = viewCount;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof ViewCountSpan other && other.viewCount.equals(viewCount);
        }

        @Override
        public int hashCode() {
            return viewCount.hashCode();
        }
    }

    // region view count text

    private static synchronized List<String> getUnits(Locale locale) {
        if (units == null || !locale.equals(unitsLocale)) {
            Set<String> found = new LinkedHashSet<>();
            try {
                CompactDecimalFormat format = CompactDecimalFormat.getInstance(
                        locale, CompactDecimalFormat.CompactStyle.SHORT);
                long value = 1_000;
                for (int i = 0; i < 13; i++) {
                    String unit = format.format(value).replaceAll("[\\p{Nd}\\s\u00A0\u202F.,'\u2019]", "");
                    if (!unit.isEmpty()) {
                        found.add(unit);
                    }
                    value *= 10;
                }
            } catch (Exception ex) {
                Logger.printException(() -> "Units failure", ex);
            }
            // The app can be in another language than its locale.
            found.add("K");
            found.add("M");
            found.add("B");
            List<String> list = new ArrayList<>(found);
            list.sort((a, b) -> b.length() - a.length());
            units = list;
            unitsLocale = locale;
        }
        return units;
    }

    private static boolean isSeparator(char c) {
        return Character.isWhitespace(c) || c == '\u00A0' || c == '\u202F';
    }

    /**
     * Languages written without spaces show the unit attached to the word, such as 4.7万次观看.
     */
    private static boolean isWrittenWithoutSpaces(char c) {
        Character.UnicodeScript script = Character.UnicodeScript.of(c);
        return script == Character.UnicodeScript.HAN
                || script == Character.UnicodeScript.HANGUL
                || script == Character.UnicodeScript.HIRAGANA
                || script == Character.UnicodeScript.KATAKANA
                || script == Character.UnicodeScript.THAI;
    }

    /**
     * Result of {@link #parseViewCount}: the shortened count, such as 4,7Mn, and the words of it,
     * such as "views", which are the same for every video.
     */
    private static final class ParsedViewCount {
        final String count;
        final String words;

        ParsedViewCount(String count, String words) {
            this.count = count;
            this.words = words;
        }
    }

    /**
     * @return The number and its unit, such as 4,7Mn from "4,7 Mn views", or null if there is no number.
     */
    @Nullable
    private static ParsedViewCount parseViewCount(String text, Locale locale) {
        Matcher matcher = NUMBER_PATTERN.matcher(text);
        if (!matcher.find()) {
            return null;
        }
        String number = matcher.group().trim();
        final int length = text.length();
        int index = matcher.end();
        while (index < length && isSeparator(text.charAt(index))) {
            index++;
        }

        String unit = "";
        for (String candidate : getUnits(locale)) {
            if (!text.regionMatches(true, index, candidate, 0, candidate.length())) {
                continue;
            }
            final int end = index + candidate.length();
            final boolean wordEnds = end >= length
                    || !Character.isLetterOrDigit(text.charAt(end))
                    || isWrittenWithoutSpaces(candidate.charAt(candidate.length() - 1));
            if (wordEnds) {
                unit = text.substring(index, end);
                index = end;
                break;
            }
        }

        String words = (text.substring(0, matcher.start()) + " " + text.substring(index))
                .replaceAll("[\\s\u00A0\u202F]+", " ").trim();
        return new ParsedViewCount(number + unit, words);
    }

    private static void addViewWords(String words) {
        if (words.isEmpty()) {
            return;
        }
        synchronized (viewWords) {
            if (viewWords.size() < MAX_VIEW_WORDS) {
                viewWords.add(words);
            }
        }
    }

    private static boolean isViewWords(String words) {
        synchronized (viewWords) {
            return viewWords.contains(words);
        }
    }

    /**
     * Injection point.
     * <p>
     * Called when the text below the title of a video is created, such as
     * "Channel · 4.7M views · 2 days ago". The text is not changed, other than the span.
     */
    public static void onFeedSubtitleCreated(@Nullable CharSequence text) {
        if (!ENABLED || !(text instanceof Spannable spannable)) {
            return;
        }
        try {
            String string = text.toString();
            final int first = string.indexOf(DELIMITER);
            if (first < 0) {
                return;
            }
            final int second = string.indexOf(DELIMITER, first + DELIMITER.length());
            if (second >= 0 && string.indexOf(DELIMITER, second + DELIMITER.length()) >= 0) {
                // Not the channel, the views and the upload time.
                return;
            }

            Locale locale = Locale.getDefault();
            ParsedViewCount parsed;
            if (second >= 0) {
                // The channel, the view count and the upload time. Same as the Hide view count option.
                parsed = parseViewCount(string.substring(first + DELIMITER.length(), second), locale);
                if (parsed == null) {
                    return;
                }
                addViewWords(parsed.words);
            } else {
                // The view count and the upload time, such as on the channel page.
                // A channel name can begin with a number too, so only the words of
                // view counts that were seen are used.
                parsed = parseViewCount(string.substring(0, first), locale);
                if (parsed == null || !isViewWords(parsed.words)) {
                    return;
                }
            }

            spannable.setSpan(new ViewCountSpan(parsed.count), 0, spannable.length(),
                    Spanned.SPAN_INCLUSIVE_INCLUSIVE);
        } catch (Exception ex) {
            Logger.printException(() -> "onFeedSubtitleCreated failure", ex);
        }
    }

    // endregion

    // region card

    @Nullable
    private static Activity findActivity(Context context) {
        while (context instanceof ContextWrapper wrapper) {
            if (context instanceof Activity activity) {
                return activity;
            }
            context = wrapper.getBaseContext();
        }
        return null;
    }

    /**
     * The share sheet and other bottom sheets can show the thumbnail and the texts
     * of the opened video, so they look like a card. Feeds are in the window of the activity.
     */
    private static boolean isInDialog(ComponentHost host) {
        Activity activity = findActivity(host.getContext());
        if (activity != null && activity.getWindow() != null) {
            View decorView = activity.getWindow().peekDecorView();
            if (decorView != null && host.getRootView() != decorView) {
                return true;
            }
        }
        ViewParent parent = host.getParent();
        while (parent instanceof View parentView) {
            final int id = parentView.getId();
            if (id != View.NO_ID) {
                try {
                    if (parentView.getResources().getResourceEntryName(id).contains("bottom_sheet")) {
                        return true;
                    }
                } catch (Resources.NotFoundException ignored) {
                    // A generated id.
                }
            }
            parent = parentView.getParent();
        }
        return false;
    }

    /**
     * What is found on a card: its view count, and where its duration badge is.
     */
    private static final class CardInfo {
        @Nullable
        String viewCount;
        @Nullable
        Rect duration;
    }

    private static void findCardInfo(ComponentHost root, View view, CardInfo info, Rect rect) {
        if (view instanceof ComponentHost host) {
            TextContent textContent = host.getTextContent();
            if (textContent != null) {
                for (CharSequence text : textContent.getTextItems()) {
                    if (text == null) {
                        continue;
                    }
                    if (info.viewCount == null && text instanceof Spanned spanned) {
                        ViewCountSpan[] spans = spanned.getSpans(0, spanned.length(), ViewCountSpan.class);
                        if (spans.length > 0) {
                            info.viewCount = spans[0].viewCount;
                        }
                    }
                    if (info.duration == null) {
                        String item = text.toString().trim();
                        if ((item.length() <= MAX_DURATION_TEXT_LENGTH && DURATION_PATTERN.matcher(item).find())
                                || BADGE_PATTERN.matcher(item).matches()) {
                            rect.set(0, 0, host.getWidth(), host.getHeight());
                            if (host != root) {
                                root.offsetDescendantRectToMyCoords(host, rect);
                            }
                            if (isDurationBadgeShaped(root, rect)) {
                                info.duration = new Rect(rect);
                            }
                        }
                    }
                }
            }
        }
        if (view instanceof ViewGroup group) {
            for (int i = 0, count = group.getChildCount(); i < count; i++) {
                View child = group.getChildAt(i);
                if (child.getVisibility() == View.VISIBLE) {
                    findCardInfo(root, child, info, rect);
                }
            }
        }
    }

    /**
     * A host only as large as the badge, at the bottom right of the thumbnail.
     */
    private static boolean isDurationBadgeShaped(ComponentHost root, Rect rect) {
        final float density = root.getResources().getDisplayMetrics().density;
        final int thumbnailHeight = Math.round(root.getWidth() * FeedVideoCards.THUMBNAIL_RATIO);
        return rect.width() > 0
                && rect.width() <= MAX_DURATION_WIDTH_DP * density
                && rect.height() >= MIN_DURATION_HEIGHT_DP * density
                && rect.height() <= MAX_DURATION_HEIGHT_DP * density
                && rect.centerX() > root.getWidth() / 2
                && rect.bottom <= thumbnailHeight + 4 * density;
    }

    private static void onCardLaidOut(ComponentHost host, @Nullable String videoId) {
        String viewCount = null;
        Rect duration = null;
        if (videoId != null && !isInDialog(host)) {
            CardInfo info = new CardInfo();
            findCardInfo(host, host, info, new Rect());
            viewCount = info.viewCount;
            duration = info.duration;
            if (viewCount != null && duration == null) {
                Logger.printDebug(() -> "Duration badge not found, default place is used: " + videoId);
            }
        }

        ViewCountBadge badge = badges.get(host);
        if (viewCount == null) {
            if (badge != null) {
                badge.detach(host);
                badges.remove(host);
            }
            return;
        }
        if (badge == null) {
            badge = new ViewCountBadge(host);
            badges.put(host, badge);
        }
        badge.bind(host, viewCount, duration);
    }

    // endregion

    /**
     * Dark rounded badge with a play icon and the view count, drawn on the overlay of the card.
     */
    private static final class ViewCountBadge extends Drawable {
        private final Paint backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint iconPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path iconPath = new Path();
        private final RectF rect = new RectF();
        private final float density;
        private final float maxTextSize;
        private final WeakReference<ComponentHost> hostRef;

        @Nullable
        private String viewCount;
        private boolean added;

        ViewCountBadge(ComponentHost host) {
            hostRef = new WeakReference<>(host);
            density = host.getResources().getDisplayMetrics().density;
            maxTextSize = TEXT_SP * density * host.getResources().getConfiguration().fontScale;

            backgroundPaint.setColor(BACKGROUND_COLOR);

            textPaint.setColor(TEXT_COLOR);
            textPaint.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            textPaint.setTextSize(maxTextSize);

            iconPaint.setColor(TEXT_COLOR);
            iconPaint.setStyle(Paint.Style.STROKE);
            iconPaint.setStrokeJoin(Paint.Join.ROUND);
            iconPaint.setStrokeCap(Paint.Cap.ROUND);
            iconPaint.setStrokeWidth(ICON_STROKE_DP * density);
        }

        private int px(float dp) {
            return Math.round(dp * density);
        }

        void bind(ComponentHost host, String newViewCount, @Nullable Rect duration) {
            viewCount = newViewCount;

            // Same height, bottom and margin as the duration badge.
            final int top;
            final int bottom;
            final int left;
            if (duration != null) {
                top = duration.top;
                bottom = duration.bottom;
                left = Math.max(0, host.getWidth() - duration.right);
            } else {
                final int thumbnailHeight = Math.round(host.getWidth() * FeedVideoCards.THUMBNAIL_RATIO);
                bottom = thumbnailHeight - px(FALLBACK_MARGIN_DP);
                top = bottom - px(FALLBACK_HEIGHT_DP);
                left = px(FALLBACK_MARGIN_DP);
            }
            final int height = bottom - top;

            textPaint.setTextSize(Math.min(maxTextSize, height * 0.62f));
            final float iconSize = textPaint.getTextSize() * 0.72f;
            final int width = Math.round(px(PADDING_DP) * 2 + iconSize + px(GAP_DP)
                    + textPaint.measureText(viewCount));

            setBounds(left, top, left + width, bottom);
            if (!added) {
                host.getOverlay().add(this);
                added = true;
            }
            invalidateSelf();
        }

        void detach(ComponentHost host) {
            if (added) {
                host.getOverlay().remove(this);
                added = false;
            }
        }

        @Override
        public void draw(@NonNull Canvas canvas) {
            if (viewCount == null) {
                return;
            }
            ComponentHost host = hostRef.get();
            if (host != null && FeedVideoCards.isCardPlayingInline(host)) {
                // The inline player shows its own controls over the thumbnail.
                return;
            }
            Rect bounds = getBounds();
            final float radius = px(RADIUS_DP);
            rect.set(bounds);
            canvas.drawRoundRect(rect, radius, radius, backgroundPaint);

            final float centerY = bounds.exactCenterY();
            final float iconSize = textPaint.getTextSize() * 0.72f;
            final float iconLeft = bounds.left + px(PADDING_DP) + iconPaint.getStrokeWidth() / 2;
            final float iconHeight = iconSize - iconPaint.getStrokeWidth();
            final float iconWidth = iconHeight * 0.88f;
            iconPath.reset();
            iconPath.moveTo(iconLeft, centerY - iconHeight / 2);
            iconPath.lineTo(iconLeft + iconWidth, centerY);
            iconPath.lineTo(iconLeft, centerY + iconHeight / 2);
            iconPath.close();
            canvas.drawPath(iconPath, iconPaint);

            Paint.FontMetrics metrics = textPaint.getFontMetrics();
            final float textX = bounds.left + px(PADDING_DP) + iconSize + px(GAP_DP);
            final float baseline = centerY - (metrics.ascent + metrics.descent) / 2;
            canvas.drawText(viewCount, textX, baseline, textPaint);
        }

        @Override
        public void setAlpha(int alpha) {
        }

        @Override
        public void setColorFilter(@Nullable ColorFilter colorFilter) {
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }
    }
}
