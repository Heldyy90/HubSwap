package ru.heldyy.hubswap.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;
import ru.heldyy.hubswap.HubSwap;
import ru.heldyy.hubswap.client.HubSwapClient;
import ru.heldyy.hubswap.config.AnarchyRanges;
import ru.heldyy.hubswap.config.HotkeySlot;
import ru.heldyy.hubswap.config.ModConfig;
import ru.heldyy.hubswap.config.StatsData;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ConfigScreen extends Screen {
    private static final MinecraftClient client = MinecraftClient.getInstance();
    private final Screen parent;
    private final ModConfig config;

    private enum Tab { SETTINGS, RANGES, HOTKEYS, STATS }

    private Tab currentTab = Tab.SETTINGS;
    private ButtonWidget tabSettings;
    private ButtonWidget tabRanges;
    private ButtonWidget tabHotkeys;
    private ButtonWidget tabStats;

    private TextFieldWidget classicDelayField;
    private TextFieldWidget clickDelayField;
    private TextFieldWidget classicCommandField;
    private TextFieldWidget lightCommandField;
    private TextFieldWidget light120CommandField;
    private TextFieldWidget primeCommandField;

    private TextFieldWidget soloStartField;
    private TextFieldWidget soloEndField;
    private TextFieldWidget duoStartField;
    private TextFieldWidget duoEndField;
    private TextFieldWidget trioStartField;
    private TextFieldWidget trioEndField;
    private TextFieldWidget clanStartField;
    private TextFieldWidget clanEndField;

    private boolean remoteRangesEnabledTmp;
    private boolean notificationsEnabledTmp;
    private boolean smartAutoTuneEnabledTmp;
    private ModConfig.ColorTheme currentTheme;
    private Formatting currentLinkColor;

    private final List<HotkeySlot> hotkeyTmp = new ArrayList<>();
    private final List<ButtonWidget> hotkeyKeyBtns = new ArrayList<>();
    private final List<TextFieldWidget> hotkeyNumFields = new ArrayList<>();
    private int listeningSlot = -1;

    private float backgroundAlpha;
    private int margin;
    private int panelW;
    private int lx;
    private int rx;
    private int colW;
    private int contentY;
    private int footerY;

    public ConfigScreen(Screen parent) {
        super(Text.literal("Настройки HubSwap"));
        this.parent = parent;
        this.config = HubSwap.getConfig();
        this.currentTheme = config.getColorTheme();
        this.currentLinkColor = config.getLinkColor();
        this.notificationsEnabledTmp = config.isNotificationsEnabled();
        this.smartAutoTuneEnabledTmp = config.isSmartAutoTuneEnabled();
        this.remoteRangesEnabledTmp = config.isRemoteRangesEnabled();
        for (HotkeySlot slot : config.getHotkeySlots()) {
            hotkeyTmp.add(new HotkeySlot(slot.getKeyCode(), slot.getMode(), slot.getServerNumber(), slot.isEnabled()));
        }
    }

    @Override
    protected void init() {
        recalcLayout();
        rebuild();
    }

    private void recalcLayout() {
        margin = Math.max(12, width / 14);
        panelW = width - margin * 2;
        lx = margin;
        rx = width / 2 + 6;
        colW = width / 2 - margin - 6;
        contentY = 100;
        footerY = height - 50;
    }

    private void rebuild() {
        clearChildren();
        hotkeyKeyBtns.clear();
        hotkeyNumFields.clear();
        buildPersistentWidgets();
        switch (currentTab) {
            case SETTINGS -> buildSettingsTab();
            case RANGES -> buildRangesTab();
            case HOTKEYS -> buildHotkeysTab();
            case STATS -> { }
        }
    }

    private void buildPersistentWidgets() {
        int cx = width / 2;
        int tabY = 36;
        int tabW = Math.min(118, panelW / 4 - 4);
        int tabX = cx - (tabW * 4 + 12) / 2;

        tabSettings = addDrawableChild(ButtonWidget.builder(Text.literal("⚙ Настройки"), b -> switchTab(Tab.SETTINGS)).dimensions(tabX, tabY, tabW, 18).build());
        tabRanges = addDrawableChild(ButtonWidget.builder(Text.literal("⇆ Диапазоны"), b -> switchTab(Tab.RANGES)).dimensions(tabX + tabW + 4, tabY, tabW, 18).build());
        tabHotkeys = addDrawableChild(ButtonWidget.builder(Text.literal("⌨ Хоткеи"), b -> switchTab(Tab.HOTKEYS)).dimensions(tabX + (tabW + 4) * 2, tabY, tabW, 18).build());
        tabStats = addDrawableChild(ButtonWidget.builder(Text.literal("📊 Статистика"), b -> switchTab(Tab.STATS)).dimensions(tabX + (tabW + 4) * 3, tabY, tabW, 18).build());

        int btnW = Math.min(150, panelW / 3);
        addDrawableChild(ButtonWidget.builder(Text.literal("✓ Сохранить"), b -> onSave()).dimensions(cx - btnW - 4, footerY + 10, btnW, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("✕ Отмена"), b -> close()).dimensions(cx + 4, footerY + 10, btnW, 20).build());
    }

    private void switchTab(Tab tab) {
        currentTab = tab;
        listeningSlot = -1;
        recalcLayout();
        rebuild();
    }

    private void buildSettingsTab() {
        int sp = Math.min(52, (footerY - contentY) / 6);
        classicDelayField = addField(lx, contentY + 10, colW, 20, String.valueOf(config.getClassicDelay()), 4);
        clickDelayField = addField(lx, contentY + sp + 10, colW, 20, String.valueOf(config.getClickDelay()), 4);
        classicCommandField = addField(lx, contentY + sp * 2 + 10, colW, 20, config.getClassicCommand(), 10);
        lightCommandField = addField(lx, contentY + sp * 3 + 10, colW, 20, config.getLightCommand(), 10);
        light120CommandField = addField(lx, contentY + sp * 4 + 10, colW, 20, config.getLight120Command(), 10);
        primeCommandField = addField(lx, contentY + sp * 5 + 10, colW, 20, config.getPrimeCommand(), 10);

        addDrawableChild(ButtonWidget.builder(notificationText(), b -> {
            notificationsEnabledTmp = !notificationsEnabledTmp;
            b.setMessage(notificationText());
        }).dimensions(rx, contentY + 10, colW, 20).build());

        addDrawableChild(ButtonWidget.builder(autoTuneText(), b -> {
            smartAutoTuneEnabledTmp = !smartAutoTuneEnabledTmp;
            b.setMessage(autoTuneText());
        }).dimensions(rx, contentY + sp + 10, colW, 20).build());

        addDrawableChild(ButtonWidget.builder(themeText(), b -> {
            currentTheme = currentTheme.next();
            b.setMessage(themeText());
        }).dimensions(rx, contentY + sp * 2 + 10, colW, 20).build());

        addDrawableChild(ButtonWidget.builder(linkColorText(), b -> {
            currentLinkColor = nextLinkColor(currentLinkColor);
            b.setMessage(linkColorText());
        }).dimensions(rx, contentY + sp * 3 + 10, colW, 20).build());
    }

    private void buildRangesTab() {
        AnarchyRanges ranges = config.getAnarchyRanges();
        int rowH = 42;
        int startY = contentY + 36;
        int gap = 8;
        int inputW = Math.max(55, (colW - gap) / 2);
        int endX = rx + inputW + gap;

        soloStartField = addField(rx, startY, inputW, 20, String.valueOf(ranges.getSolo().getStart()), 4);
        soloEndField = addField(endX, startY, inputW, 20, String.valueOf(ranges.getSolo().getEnd()), 4);
        duoStartField = addField(rx, startY + rowH, inputW, 20, String.valueOf(ranges.getDuo().getStart()), 4);
        duoEndField = addField(endX, startY + rowH, inputW, 20, String.valueOf(ranges.getDuo().getEnd()), 4);
        trioStartField = addField(rx, startY + rowH * 2, inputW, 20, String.valueOf(ranges.getTrio().getStart()), 4);
        trioEndField = addField(endX, startY + rowH * 2, inputW, 20, String.valueOf(ranges.getTrio().getEnd()), 4);
        clanStartField = addField(rx, startY + rowH * 3, inputW, 20, String.valueOf(ranges.getClan().getStart()), 4);
        clanEndField = addField(endX, startY + rowH * 3, inputW, 20, String.valueOf(ranges.getClan().getEnd()), 4);

        int toggleY = startY + rowH * 4 + 8;
        addDrawableChild(ButtonWidget.builder(remoteRangesText(), b -> {
            remoteRangesEnabledTmp = !remoteRangesEnabledTmp;
            b.setMessage(remoteRangesText());
        }).dimensions(lx, toggleY, panelW, 20).build());
    }

    private void buildHotkeysTab() {
        int rowH = Math.min(20, (footerY - contentY - 30) / 9);
        int startY = contentY + 24;
        int keyW = (int) (panelW * 0.30);
        int modeW = (int) (panelW * 0.16);
        int numW = (int) (panelW * 0.09);
        int togW = (int) (panelW * 0.12);
        int gap = (panelW - keyW - modeW - numW - togW) / 3;
        int kx = lx;
        int mx = kx + keyW + gap;
        int nx = mx + modeW + gap;
        int tx = nx + numW + gap;

        for (int i = 0; i < 8; i++) {
            int idx = i;
            HotkeySlot slot = hotkeyTmp.get(i);
            int y = startY + (rowH + 2) * i;

            ButtonWidget keyButton = addDrawableChild(ButtonWidget.builder(Text.literal(slot.getKeyCode() < 0 ? "[ --- ]" : "[ " + getKeyName(slot.getKeyCode()) + " ]"), b -> {
                listeningSlot = idx;
                b.setMessage(Text.literal("[ нажми... ]"));
            }).dimensions(kx, y, keyW, rowH).build());
            hotkeyKeyBtns.add(keyButton);

            addDrawableChild(ButtonWidget.builder(Text.literal(modeName(slot.getMode())), b -> {
                String next = nextMode(hotkeyTmp.get(idx).getMode());
                hotkeyTmp.get(idx).setMode(next);
                b.setMessage(Text.literal(modeName(next)));
            }).dimensions(mx, y, modeW, rowH).build());

            TextFieldWidget number = addField(nx, y, numW, rowH, String.valueOf(slot.getServerNumber()), 3);
            hotkeyNumFields.add(number);

            addDrawableChild(ButtonWidget.builder(enabledText(slot.isEnabled()), b -> {
                boolean enabled = !hotkeyTmp.get(idx).isEnabled();
                hotkeyTmp.get(idx).setEnabled(enabled);
                b.setMessage(enabledText(enabled));
            }).dimensions(tx, y, togW, rowH).build());
        }
    }

    private TextFieldWidget addField(int x, int y, int w, int h, String text, int maxLength) {
        TextFieldWidget field = new TextFieldWidget(textRenderer, x, y, w, h, Text.empty());
        field.setText(text);
        field.setMaxLength(maxLength);
        return addDrawableChild(field);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (listeningSlot >= 0) {
            int keyCode = input.key();
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                hotkeyTmp.get(listeningSlot).setKeyCode(-1);
                hotkeyKeyBtns.get(listeningSlot).setMessage(Text.literal("[ --- ]"));
            } else {
                hotkeyTmp.get(listeningSlot).setKeyCode(keyCode);
                hotkeyKeyBtns.get(listeningSlot).setMessage(Text.literal("[ " + getKeyName(keyCode) + " ]"));
            }
            listeningSlot = -1;
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        backgroundAlpha = Math.min(1.0f, backgroundAlpha + delta * 2.0f);
        renderBackgroundPanels(context);
        switch (currentTab) {
            case SETTINGS -> renderSettings(context);
            case RANGES -> renderRanges(context);
            case HOTKEYS -> renderHotkeys(context);
            case STATS -> renderStats(context);
        }
        super.render(context, mouseX, mouseY, delta);
        renderTabUnderline(context);
    }

    private void renderBackgroundPanels(DrawContext context) {
        int a = (int) (backgroundAlpha * 220);
        context.fillGradient(0, 0, width, height, (a << 24) | 0x0a0e27, (a << 24) | 0x1a1f3a);
        context.fillGradient(0, 0, width, 60, (a << 24) | 0x16213e, (a << 24) | 0x0f1728);
        context.fill(0, 58, width, 60, (a << 24) | currentTheme.getRgbColor());
        context.drawCenteredTextWithShadow(textRenderer, Text.literal("HubSwap").formatted(currentTheme.getFormatting(), Formatting.BOLD), width / 2, 17, textArgb(0xFFFFFF));
        context.fill(0, footerY, width, footerY + 2, (a << 24) | currentTheme.getRgbColor());
        context.fill(0, footerY + 2, width, height, ((int) (backgroundAlpha * 160) << 24) | 0x0a0e27);
    }

    private void renderSettings(DrawContext context) {
        int sp = Math.min(52, (footerY - contentY) / 6);
        String[] left = {"⏱ Задержка /hub (мс)", "⏱ Задержка между кликами (мс)", "⌨ Команда для Classic", "⌨ Команда для Lite", "⌨ Команда для Lite 1.20", "⌨ Команда для Prime"};
        String[] leftHint = {"Напрямую при выключенном автоподборе", "Напрямую при выключенном автоподборе", "Например: cn", "Например: ln", "Например: ln120", "Например: pn"};
        for (int i = 0; i < left.length; i++) renderLabel(context, lx, contentY + sp * i - 12, colW, left[i], leftHint[i]);

        String[] right = {"🔔 Уведомления", "🧠 Умный автоподбор", "🎨 Цветовая тема", "🔗 Цвет серверов"};
        String[] rightHint = {"Показывать уведомления о переходе", "Автоматически подбирает задержки", "Изменяет цвет элементов интерфейса", "Цвет кликабельных серверов в чате"};
        for (int i = 0; i < right.length; i++) renderLabel(context, rx, contentY + sp * i - 12, colW, right[i], rightHint[i]);
    }

    private void renderRanges(DrawContext context) {
        int rowH = 42;
        int startY = contentY + 36;
        int inputW = Math.max(55, (colW - 8) / 2);
        renderSectionHeader(context, contentY, "⇆ Диапазоны Lite-анархий");

        int headerY = startY - 16;
        context.drawText(textRenderer, Text.literal("Тип"), lx + 8, headerY, textArgb(0xCCCCCC), false);
        context.drawText(textRenderer, Text.literal("От"), rx + 4, headerY, textArgb(0xCCCCCC), false);
        context.drawText(textRenderer, Text.literal("До"), rx + inputW + 12, headerY, textArgb(0xCCCCCC), false);

        String[] names = {"Соло", "Дуо", "Трио", "Клан"};
        String[] hints = {"1-я страница меню", "2-я страница меню", "3-я страница меню", "4-я страница меню"};
        for (int i = 0; i < names.length; i++) {
            int y = startY + rowH * i;
            context.drawText(textRenderer, Text.literal(names[i]).formatted(currentTheme.getFormatting()), lx + 8, y + 2, textArgb(currentTheme.getRgbColor()), true);
            context.drawText(textRenderer, Text.literal(hints[i]), lx + 8, y + 14, textArgb(0x999999), false);
        }

        int infoY = startY + rowH * 4 + 34;
        String text = remoteRangesEnabledTmp
                ? "Общий anarchy_ranges.json имеет приоритет и проверяется каждые 15 минут."
                : "Удалённая синхронизация выключена: используются только локальные значения.";
        context.drawCenteredTextWithShadow(textRenderer, Text.literal(text), width / 2, infoY, textArgb(0xBBBBBB));
    }

    private void renderHotkeys(DrawContext context) {
        int rowH = Math.min(20, (footerY - contentY - 30) / 9);
        int startY = contentY + 24;
        int theme = currentTheme.getRgbColor();
        context.drawText(textRenderer, Text.literal("Клавиша").formatted(currentTheme.getFormatting()), lx, contentY + 8, textArgb(theme), false);
        context.drawText(textRenderer, Text.literal("Нажмите кнопку и затем клавишу. ESC очищает назначение."), lx, startY + (rowH + 2) * 8 + 2, textArgb(0x777788), false);
    }

    private void renderStats(DrawContext context) {
        StatsData stats = HubSwap.getStats();
        int y = contentY;
        renderSectionHeader(context, y, "📊 Переходы");
        y += 18;
        context.drawText(textRenderer, Text.literal("Всего: " + stats.getTotalSwitches()), lx + 8, y, textArgb(0xFFFFFF), true);
        context.drawText(textRenderer, Text.literal("За сессию: " + stats.getSessionSwitches()), rx, y, textArgb(0xFFFFFF), true);
        y += 24;

        renderSectionHeader(context, y, "🏆 Любимый сервер");
        y += 18;
        String favorite = stats.getFavoriteKey();
        context.drawText(textRenderer, Text.literal(favorite == null ? "Пока нет данных" : StatsData.formatKey(favorite) + " — " + stats.getCountForKey(favorite) + " раз"), lx + 8, y, textArgb(favorite == null ? 0x777777 : currentTheme.getRgbColor()), favorite != null);
        y += 24;

        renderSectionHeader(context, y, "📋 Топ серверов");
        y += 18;
        List<Map.Entry<String, Long>> top = stats.getServerCounts().entrySet().stream().sorted(Map.Entry.<String, Long>comparingByValue().reversed()).limit(5).toList();
        if (top.isEmpty()) {
            context.drawText(textRenderer, Text.literal("Пока нет данных"), lx + 8, y, textArgb(0x777777), false);
            return;
        }
        for (int i = 0; i < top.size(); i++) {
            Map.Entry<String, Long> entry = top.get(i);
            context.drawText(textRenderer, Text.literal((i + 1) + ". " + StatsData.formatKey(entry.getKey()) + " — " + entry.getValue() + " раз"), lx + 8, y + i * 14, textArgb(i == 0 ? currentTheme.getRgbColor() : 0xCCCCCC), i == 0);
        }
    }

    private void renderSectionHeader(DrawContext context, int y, String title) {
        int a = (int) (backgroundAlpha * 160);
        context.fill(lx, y, lx + panelW, y + 13, (a << 24) | 0x16213e);
        context.fill(lx, y, lx + 3, y + 13, ((int) (backgroundAlpha * 255) << 24) | currentTheme.getRgbColor());
        context.drawText(textRenderer, Text.literal(title).formatted(currentTheme.getFormatting()), lx + 7, y + 3, textArgb(currentTheme.getRgbColor()), false);
    }

    private void renderLabel(DrawContext context, int x, int y, int w, String label, String hint) {
        int theme = currentTheme.getRgbColor();
        context.fill(x - 2, y, x + w + 2, y + 18, ((int) (backgroundAlpha * 100) << 24) | 0x1a1f3a);
        context.fill(x - 2, y, x, y + 18, ((int) (backgroundAlpha * 255) << 24) | theme);
        context.drawText(textRenderer, Text.literal(label).formatted(currentTheme.getFormatting()), x + 3, y + 2, textArgb(theme), false);
        context.getMatrices().pushMatrix();
        context.getMatrices().translate(x + 3, y + 11);
        context.getMatrices().scale(0.7f, 0.7f);
        context.drawText(textRenderer, hint, 0, 0, textArgb(0xCCCCCC), false);
        context.getMatrices().popMatrix();
    }

    private void renderTabUnderline(DrawContext context) {
        ButtonWidget button = switch (currentTab) {
            case SETTINGS -> tabSettings;
            case RANGES -> tabRanges;
            case HOTKEYS -> tabHotkeys;
            case STATS -> tabStats;
        };
        int color = ((int) (backgroundAlpha * 240) << 24) | currentTheme.getRgbColor();
        context.fill(button.getX(), button.getY() + button.getHeight() - 2, button.getX() + button.getWidth(), button.getY() + button.getHeight(), color);
    }

    private int textArgb(int rgb) {
        return ((int) (backgroundAlpha * 255) << 24) | (rgb & 0x00FFFFFF);
    }

    private void onSave() {
        try {
            if (classicDelayField != null) {
                int classicDelay = Integer.parseInt(classicDelayField.getText());
                int clickDelay = Integer.parseInt(clickDelayField.getText());
                if (classicDelay < 100 || classicDelay > 5000) {
                    sendError("Задержка /hub: от 100 до 5000 мс");
                    return;
                }
                if (clickDelay < 50 || clickDelay > 1000) {
                    sendError("Задержка кликов: от 50 до 1000 мс");
                    return;
                }
                config.setDelays(classicDelay, clickDelay);
                config.setCommands(classicCommandField.getText(), lightCommandField.getText(), light120CommandField.getText(), primeCommandField.getText());
                config.setNotificationsEnabled(notificationsEnabledTmp);
                config.setSmartAutoTuneEnabled(smartAutoTuneEnabledTmp);
            }

            if (soloStartField != null) {
                AnarchyRanges ranges = new AnarchyRanges(
                        range(soloStartField, soloEndField),
                        range(duoStartField, duoEndField),
                        range(trioStartField, trioEndField),
                        range(clanStartField, clanEndField)
                );
                String error = ranges.validationError();
                if (error != null) {
                    sendError(error);
                    return;
                }
                config.setAnarchyRanges(ranges);
            }
        } catch (NumberFormatException e) {
            sendError("Введите числовые значения");
            return;
        }

        config.setRemoteRangesEnabled(remoteRangesEnabledTmp);
        config.setColorTheme(currentTheme);
        config.setLinkColor(currentLinkColor);

        for (int i = 0; i < hotkeyNumFields.size() && i < hotkeyTmp.size(); i++) {
            try {
                hotkeyTmp.get(i).setServerNumber(Math.max(1, Integer.parseInt(hotkeyNumFields.get(i).getText().trim())));
            } catch (NumberFormatException ignored) { }
        }

        List<HotkeySlot> destination = config.getHotkeySlots();
        for (int i = 0; i < 8; i++) {
            HotkeySlot src = hotkeyTmp.get(i);
            HotkeySlot dst = destination.get(i);
            dst.setKeyCode(src.getKeyCode());
            dst.setMode(src.getMode());
            dst.setServerNumber(src.getServerNumber());
            dst.setEnabled(src.isEnabled());
        }

        HubSwap.saveConfig();
        HubSwapClient.registerConfiguredCommands();
        if (client.player != null) client.player.sendMessage(Text.literal("[HubSwap] ").formatted(currentTheme.getFormatting()).append(Text.literal("✓ Настройки сохранены!").formatted(Formatting.GREEN)), false);
        close();
    }

    private AnarchyRanges.Range range(TextFieldWidget start, TextFieldWidget end) {
        return new AnarchyRanges.Range(Integer.parseInt(start.getText().trim()), Integer.parseInt(end.getText().trim()));
    }

    private void sendError(String message) {
        if (client.player != null) client.player.sendMessage(Text.literal("[HubSwap] ").formatted(currentTheme.getFormatting()).append(Text.literal("Ошибка: " + message).formatted(Formatting.RED)), false);
    }

    private Text notificationText() {
        return notificationsEnabledTmp ? Text.literal("🔔 Уведомления: ВКЛ").formatted(Formatting.GREEN) : Text.literal("🔕 Уведомления: ВЫКЛ").formatted(Formatting.RED);
    }

    private Text autoTuneText() {
        return smartAutoTuneEnabledTmp ? Text.literal("🧠 Умный автоподбор: ВКЛ").formatted(Formatting.GREEN) : Text.literal("🧠 Умный автоподбор: ВЫКЛ").formatted(Formatting.RED);
    }

    private Text remoteRangesText() {
        return remoteRangesEnabledTmp ? Text.literal("🌐 Общие диапазоны: ВКЛ").formatted(Formatting.GREEN) : Text.literal("🌐 Общие диапазоны: ВЫКЛ").formatted(Formatting.RED);
    }

    private Text themeText() {
        return Text.literal("🎨 Тема: " + currentTheme.getDisplayName()).formatted(currentTheme.getFormatting());
    }

    private Text linkColorText() {
        return Text.literal("🔗 Цвет ссылок: " + linkColorName(currentLinkColor)).formatted(currentLinkColor);
    }

    private Text enabledText(boolean enabled) {
        return enabled ? Text.literal("✓ Вкл").formatted(Formatting.GREEN) : Text.literal("✗ Выкл").formatted(Formatting.RED);
    }

    private String modeName(String mode) {
        return switch (mode) {
            case "classic" -> "Classic";
            case "light120" -> "Lite 1.20";
            case "prime" -> "Prime";
            default -> "Lite";
        };
    }

    private String nextMode(String mode) {
        return switch (mode) {
            case "classic" -> "light";
            case "light" -> "light120";
            case "light120" -> "prime";
            default -> "classic";
        };
    }

    private String getKeyName(int keyCode) {
        if (keyCode >= GLFW.GLFW_KEY_A && keyCode <= GLFW.GLFW_KEY_Z) return String.valueOf((char) keyCode);
        if (keyCode >= GLFW.GLFW_KEY_0 && keyCode <= GLFW.GLFW_KEY_9) return String.valueOf((char) keyCode);
        String name = GLFW.glfwGetKeyName(keyCode, 0);
        if (name != null && !name.isBlank()) return name.toUpperCase();
        return switch (keyCode) {
            case GLFW.GLFW_KEY_SPACE -> "SPACE";
            case GLFW.GLFW_KEY_ENTER -> "ENTER";
            case GLFW.GLFW_KEY_TAB -> "TAB";
            case GLFW.GLFW_KEY_LEFT_SHIFT -> "L-SHIFT";
            case GLFW.GLFW_KEY_RIGHT_SHIFT -> "R-SHIFT";
            case GLFW.GLFW_KEY_LEFT_CONTROL -> "L-CTRL";
            case GLFW.GLFW_KEY_RIGHT_CONTROL -> "R-CTRL";
            case GLFW.GLFW_KEY_LEFT_ALT -> "L-ALT";
            case GLFW.GLFW_KEY_RIGHT_ALT -> "R-ALT";
            default -> "Key" + keyCode;
        };
    }

    private String linkColorName(Formatting color) {
        if (color == Formatting.GREEN) return "Зелёный";
        if (color == Formatting.YELLOW) return "Жёлтый";
        if (color == Formatting.AQUA) return "Синий";
        if (color == Formatting.LIGHT_PURPLE) return "Фиолетовый";
        if (color == Formatting.RED) return "Красный";
        return "Золотой";
    }

    private Formatting nextLinkColor(Formatting current) {
        Formatting[] colors = {Formatting.GOLD, Formatting.GREEN, Formatting.YELLOW, Formatting.AQUA, Formatting.LIGHT_PURPLE, Formatting.RED};
        for (int i = 0; i < colors.length; i++) if (colors[i] == current) return colors[(i + 1) % colors.length];
        return Formatting.GOLD;
    }

    @Override
    public void close() {
        client.setScreen(parent);
    }
}
