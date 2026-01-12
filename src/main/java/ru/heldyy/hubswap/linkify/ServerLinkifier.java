package ru.heldyy.hubswap.linkify;

import net.minecraft.text.ClickEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import ru.heldyy.hubswap.config.ModConfig;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ServerLinkifier {

    private static final Pattern PATTERN = Pattern.compile(
            "(?i)" +
                    "(?<l2N>\\bl2anarchy(?<l2Num>\\d+)\\b)" +
                    "|(?<l2One>\\bl2anarchy\\b)" +
                    "|(?<liteN>\\bLite-Anarchy-(?<liteNum>\\d+)\\b)" +
                    "|(?<lite1>\\bLite-Anarchy\\b)" +
                    "|(?<lanN>\\blanarchy(?<lanNum>\\d+)\\b)" +
                    "|(?<lan1>\\blanarchy\\b)" +
                    "|(?<clDash>(?<!lite-)\\banarchy-(?<clDashNum>\\d+)\\b)" +
                    "|(?<clN>(?<!lite-)\\banarchy(?<clNum>\\d+)\\b)" +
                    "|(?<cl1>(?<!lite-)\\banarchy\\b)"
    );

    public static Text linkify(Text original, ModConfig cfg) {
        if (original == null) return null;
        if (cfg == null) return original;

        String rawAll = original.getString();
        if (rawAll == null || rawAll.isEmpty()) return original;
        if (!PATTERN.matcher(rawAll).find()) return original;

        MutableText out = Text.empty();
        final boolean[] changed = new boolean[]{false};

        original.visit((style, part) -> {
            if (part == null || part.isEmpty()) return Optional.empty();

            boolean segmentChanged = appendLinkifiedPart(out, style, part, cfg);
            if (segmentChanged) changed[0] = true;

            return Optional.empty();
        }, Style.EMPTY);

        return changed[0] ? out : original;
    }

    private static boolean appendLinkifiedPart(MutableText out, Style baseStyle, String segment, ModConfig cfg) {
        Matcher m = PATTERN.matcher(segment);
        if (!m.find()) {
            out.append(Text.literal(segment).setStyle(baseStyle));
            return false;
        }

        m.reset();
        int last = 0;

        while (m.find()) {
            if (m.start() > last) {
                out.append(Text.literal(segment.substring(last, m.start())).setStyle(baseStyle));
            }

            String matchedText = segment.substring(m.start(), m.end());

            boolean lite120 = m.group("l2One") != null || m.group("l2N") != null;
            boolean lite = lite120
                    || m.group("lite1") != null || m.group("liteN") != null
                    || m.group("lan1") != null || m.group("lanN") != null;

            int serverNum = 1;
            if (m.group("l2Num") != null) serverNum = parseIntSafe(m.group("l2Num"), 1);
            else if (m.group("liteNum") != null) serverNum = parseIntSafe(m.group("liteNum"), 1);
            else if (m.group("lanNum") != null) serverNum = parseIntSafe(m.group("lanNum"), 1);
            else if (m.group("clDashNum") != null) serverNum = parseIntSafe(m.group("clDashNum"), 1);
            else if (m.group("clNum") != null) serverNum = parseIntSafe(m.group("clNum"), 1);

            String baseCmd;
            if (lite120) baseCmd = cfg.getLight120Command();
            else if (lite) baseCmd = cfg.getLightCommand();
            else baseCmd = cfg.getClassicCommand();
            String command = "/" + baseCmd + " " + serverNum;

            Formatting linkColor = cfg.getLinkColor();

            Style linkStyle = baseStyle
                    .withUnderline(true)
                    .withColor(linkColor)
                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command));

            out.append(Text.literal(matchedText).setStyle(linkStyle));
            last = m.end();
        }

        if (last < segment.length()) {
            out.append(Text.literal(segment.substring(last)).setStyle(baseStyle));
        }

        return true;
    }

    private static int parseIntSafe(String s, int def) {
        try {
            return Integer.parseInt(s);
        } catch (Exception e) {
            return def;
        }
    }
}
