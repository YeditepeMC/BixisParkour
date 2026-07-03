package net.yeditepemc.bixisparkour.util;

/**
 * Süre formatlama yardımcısı.
 * - {@link #format(long)}      : mm:ss.SSS (leaderboard / bitiş)
 * - {@link #formatShort(long)} : mm:ss     (ActionBar timer)
 */
public final class TimeFormatter {

    private TimeFormatter() {
    }

    /**
     * Milisaniyeyi mm:ss.SSS formatına çevirir (örn. 01:23.456).
     */
    public static String format(long millis) {
        if (millis < 0) {
            millis = 0;
        }
        long minutes = millis / 60_000L;
        long seconds = (millis % 60_000L) / 1_000L;
        long ms = millis % 1_000L;
        return String.format("%02d:%02d.%03d", minutes, seconds, ms);
    }

    /**
     * Milisaniyeyi mm:ss formatına çevirir (milisaniye yok, örn. 01:23).
     */
    public static String formatShort(long millis) {
        if (millis < 0) {
            millis = 0;
        }
        long s = millis / 1000;
        long m = s / 60;
        s = s % 60;
        return String.format("%02d:%02d", m, s);
    }
}
