package ru.heldyy.hubswap.linkify;

import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
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
                    "(?<liteN>\\bLite-Anarchy-(?<liteNum>\\d+)\\b)" +
                    "|(?<liteShort>\\bLite-(?<liteShortNum>\\d+)\\b)" +
                    "|(?<lite120>\\b1-20L-(?<lite120Num>[1-3])\\b)" +
                    "|(?<l2a3>\\bl2anarchy3\\b)" +
                    "|(?<l2a2>\\bl2anarchy2\\b)" +
                    "|(?<l2a1>\\bl2anarchy\\b)" +
                    "|(?<lanN>\\blanarchy(?<lanNum>\\d+)\\b)" +
                    "|(?<lanBare>\\blanarchy\\b)" +
                    "|(?<clDash>(?<![a-zA-Z])Anarchy-(?<clDashNum>[1-5])\\b)" +
                    "|(?<clN>(?<![a-zA-Z])anarchy(?<clNum>[1-5])\\b)"
    );

    public static Text linkify(Text original, ModConfig cfg) {
        if (original == null || cfg == null) return original;

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

            boolean lite120 = m.group("lite120") != null
                    || m.group("l2a1") != null
                    || m.group("l2a2") != null
                    || m.group("l2a3") != null;

            boolean lanBare = m.group("lanBare") != null;

            boolean lite = lite120
                    || m.group("liteN") != null
                    || m.group("liteShort") != null
                    || m.group("lanN") != null
                    || lanBare;

            int serverNum = 1;

            if (m.group("l2a3") != null) {
                serverNum = 3;
            } else if (m.group("l2a2") != null) {
                serverNum = 2;
            } else if (m.group("l2a1") != null) {
                serverNum = 1;
            } else if (m.group("liteNum") != null) {
                serverNum = parseIntSafe(m.group("liteNum"), 1);
            } else if (m.group("liteShortNum") != null) {
                serverNum = parseIntSafe(m.group("liteShortNum"), 1);
            } else if (m.group("lite120Num") != null) {
                serverNum = parseIntSafe(m.group("lite120Num"), 1);
            } else if (m.group("lanNum") != null) {
                serverNum = parseIntSafe(m.group("lanNum"), 1);
            } else if (m.group("clDashNum") != null) {
                serverNum = parseIntSafe(m.group("clDashNum"), 1);
            } else if (m.group("clNum") != null) {
                serverNum = parseIntSafe(m.group("clNum"), 1);
            }

            String baseCmd = lite120
                    ? cfg.getLight120Command()
                    : (lite ? cfg.getLightCommand() : cfg.getClassicCommand());

            String command = "/" + baseCmd + " " + serverNum;

            Formatting linkColor = cfg.getLinkColor();
            Style linkStyle = Style.EMPTY
                    .withBold(baseStyle.isBold())
                    .withItalic(baseStyle.isItalic())
                    .withUnderline(true)
                    .withColor(linkColor)
                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
                    .withHoverEvent(new HoverEvent(
                            HoverEvent.Action.SHOW_TEXT,
                            Text.literal("Нажмите: ").formatted(Formatting.GRAY)
                                    .append(Text.literal(command).formatted(linkColor))
                    ));

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
