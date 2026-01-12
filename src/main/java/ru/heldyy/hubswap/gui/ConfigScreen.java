package ru.heldyy.hubswap.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import ru.heldyy.hubswap.HubSwap;
import ru.heldyy.hubswap.client.HubSwapClient;
import ru.heldyy.hubswap.config.ModConfig;

public class ConfigScreen extends Screen {
    private final Screen parent;
    private final ModConfig config;

    private TextFieldWidget classicDelayField;
    private TextFieldWidget clickDelayField;
    private TextFieldWidget classicCommandField;
    private TextFieldWidget lightCommandField;
    private TextFieldWidget light120CommandField;

    private boolean notificationsEnabledTmp;
    private ButtonWidget notificationsToggleButton;

    private ModConfig.ColorTheme currentTheme;
    private ButtonWidget themeToggleButton;

    private Formatting currentLinkColor;
    private ButtonWidget linkColorToggleButton;

    private ButtonWidget saveButton;
    private ButtonWidget cancelButton;

    private float backgroundAlpha = 0.0f;
    private float contentOffset = 20.0f;

    public ConfigScreen(Screen parent) {
        super(Text.literal("Настройки HubSwap"));
        this.parent = parent;
        this.config = HubSwap.getConfig();
        this.currentTheme = config.getColorTheme();
        this.currentLinkColor = config.getLinkColor();
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int leftColX = centerX - 310;
        int rightColX = centerX + 10;
        int startY = 90;
        int fieldWidth = 300;
        int fieldHeight = 20;
        int spacing = 50;

        classicDelayField = new TextFieldWidget(this.textRenderer, leftColX, startY, fieldWidth, fieldHeight, Text.literal("Задержка /hub"));
        classicDelayField.setText(String.valueOf(config.getClassicDelay()));
        classicDelayField.setMaxLength(4);
        addDrawableChild(classicDelayField);

        clickDelayField = new TextFieldWidget(this.textRenderer, leftColX, startY + spacing, fieldWidth, fieldHeight, Text.literal("Задержка кликов"));
        clickDelayField.setText(String.valueOf(config.getClickDelay()));
        clickDelayField.setMaxLength(4);
        addDrawableChild(clickDelayField);

        classicCommandField = new TextFieldWidget(this.textRenderer, leftColX, startY + spacing * 2, fieldWidth, fieldHeight, Text.literal("Команда классик"));
        classicCommandField.setText(config.getClassicCommand());
        classicCommandField.setMaxLength(10);
        addDrawableChild(classicCommandField);

        lightCommandField = new TextFieldWidget(this.textRenderer, leftColX, startY + spacing * 3, fieldWidth, fieldHeight, Text.literal("Команда лайт"));
        lightCommandField.setText(config.getLightCommand());
        lightCommandField.setMaxLength(10);
        addDrawableChild(lightCommandField);

        light120CommandField = new TextFieldWidget(this.textRenderer, leftColX, startY + spacing * 4, fieldWidth, fieldHeight, Text.literal("Команда лайт 1.20"));
        light120CommandField.setText(config.getLight120Command());
        light120CommandField.setMaxLength(10);
        addDrawableChild(light120CommandField);

        notificationsEnabledTmp = config.isNotificationsEnabled();
        notificationsToggleButton = addDrawableChild(ButtonWidget.builder(
                getNotificationButtonText(),
                btn -> {
                    notificationsEnabledTmp = !notificationsEnabledTmp;
                    btn.setMessage(getNotificationButtonText());
                }
        ).dimensions(rightColX, startY, fieldWidth, fieldHeight).build());

        themeToggleButton = addDrawableChild(ButtonWidget.builder(
                getThemeButtonText(),
                btn -> {
                    currentTheme = currentTheme.next();
                    btn.setMessage(getThemeButtonText());
                }
        ).dimensions(rightColX, startY + spacing, fieldWidth, fieldHeight).build());

        linkColorToggleButton = addDrawableChild(ButtonWidget.builder(
                getLinkColorButtonText(),
                btn -> {
                    currentLinkColor = nextLinkColor(currentLinkColor);
                    btn.setMessage(getLinkColorButtonText());
                }
        ).dimensions(rightColX, startY + spacing * 2, fieldWidth, fieldHeight).build());

        int buttonY = this.height - 35;
        int buttonWidth = 140;

        saveButton = addDrawableChild(ButtonWidget.builder(Text.literal("✓ Сохранить"), btn -> onSave())
                .dimensions(centerX - buttonWidth - 5, buttonY, buttonWidth, fieldHeight)
                .build());

        cancelButton = addDrawableChild(ButtonWidget.builder(Text.literal("✕ Отмена"), btn -> close())
                .dimensions(centerX + 5, buttonY, buttonWidth, fieldHeight)
                .build());
    }

    private Text getNotificationButtonText() {
        return (notificationsEnabledTmp
                ? Text.literal("🔔 Уведомления: ВКЛ")
                : Text.literal("🔕 Уведомления: ВЫКЛ")
        ).formatted(Formatting.WHITE);
    }

    private Text getThemeButtonText() {
        return Text.literal("🎨 Тема: " + currentTheme.getDisplayName())
                .formatted(currentTheme.getFormatting());
    }

    private Text getLinkColorButtonText() {
        String colorName = getLinkColorName(currentLinkColor);
        return Text.literal("🔗 Цвет ссылок: " + colorName)
                .formatted(currentLinkColor);
    }

    private String getLinkColorName(Formatting color) {
        if (color == Formatting.GOLD) return "Золотой";
        if (color == Formatting.GREEN) return "Зелёный";
        if (color == Formatting.YELLOW) return "Жёлтый";
        if (color == Formatting.AQUA) return "Синий";
        if (color == Formatting.LIGHT_PURPLE) return "Фиолетовый";
        if (color == Formatting.RED) return "Красный";
        return "Золотой";
    }

    private Formatting nextLinkColor(Formatting current) {
        Formatting[] colors = {
                Formatting.GOLD,
                Formatting.GREEN,
                Formatting.YELLOW,
                Formatting.AQUA,
                Formatting.LIGHT_PURPLE,
                Formatting.RED
        };

        for (int i = 0; i < colors.length; i++) {
            if (colors[i] == current) {
                return colors[(i + 1) % colors.length];
            }
        }
        return Formatting.GOLD;
    }

    private void onSave() {
        try {
            int classicDelay = Integer.parseInt(classicDelayField.getText());
            int clickDelay = Integer.parseInt(clickDelayField.getText());

            if (classicDelay < 100 || classicDelay > 5000) {
                if (client != null && client.player != null) {
                    client.player.sendMessage(Text.literal("[HubSwap] ").formatted(currentTheme.getFormatting())
                            .append(Text.literal("Ошибка: Задержка /hub должна быть от 100 до 5000 мс").formatted(Formatting.RED)), false);
                }
                return;
            }
            if (clickDelay < 50 || clickDelay > 1000) {
                if (client != null && client.player != null) {
                    client.player.sendMessage(Text.literal("[HubSwap] ").formatted(currentTheme.getFormatting())
                            .append(Text.literal("Ошибка: Задержка кликов должна быть от 50 до 1000 мс").formatted(Formatting.RED)), false);
                }
                return;
            }

            config.setDelays(classicDelay, clickDelay);
            config.setCommands(classicCommandField.getText(), lightCommandField.getText(), light120CommandField.getText());
            config.setNotificationsEnabled(notificationsEnabledTmp);
            config.setColorTheme(currentTheme);
            config.setLinkColor(currentLinkColor);
            HubSwap.saveConfig();

            HubSwapClient.registerConfiguredCommands();

            if (client != null && client.player != null) {
                client.player.sendMessage(Text.literal("[HubSwap] ").formatted(currentTheme.getFormatting())
                        .append(Text.literal("✓ Настройки сохранены успешно!").formatted(Formatting.GREEN)), false);
            }
            close();
        } catch (NumberFormatException e) {
            if (client != null && client.player != null) {
                client.player.sendMessage(Text.literal("[HubSwap] ").formatted(currentTheme.getFormatting())
                        .append(Text.literal("Ошибка: Введите числовые значения для задержек").formatted(Formatting.RED)), false);
            }
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        backgroundAlpha = Math.min(1.0f, backgroundAlpha + delta * 2.0f);
        contentOffset = Math.max(0.0f, contentOffset - delta * 60.0f);

        renderGradientBackground(context);
        renderHeaderPanel(context);

        context.getMatrices().push();
        context.getMatrices().translate(0, contentOffset, 0);

        renderLabels(context);

        super.render(context, mouseX, mouseY, delta);

        context.getMatrices().pop();

        renderFooterLine(context);
    }

    private void renderGradientBackground(DrawContext context) {
        int colorTop = ((int) (backgroundAlpha * 200) << 24) | 0x0a0e27;
        int colorBottom = ((int) (backgroundAlpha * 220) << 24) | 0x1a1f3a;
        context.fillGradient(0, 0, this.width, this.height, colorTop, colorBottom);
    }

    private void renderHeaderPanel(DrawContext context) {
        int panelHeight = 65;
        int alpha = (int) (backgroundAlpha * 180);

        context.fillGradient(0, 0, this.width, panelHeight, alpha << 24 | 0x16213e, alpha << 24 | 0x0f1728);

        int themeColor = currentTheme.getRgbColor();
        context.fill(0, panelHeight - 2, this.width, panelHeight, alpha << 24 | themeColor);

        Text title = Text.literal("⚙ Настройки HubSwap").formatted(currentTheme.getFormatting(), Formatting.BOLD);
        context.drawCenteredTextWithShadow(this.textRenderer, title, this.width / 2, 20, 0xFFFFFF);
    }

    private void renderLabels(DrawContext context) {
        int centerX = this.width / 2;
        int leftColX = centerX - 310;
        int rightColX = centerX + 10;
        int startY = 90;
        int spacing = 50;
        int themeColor = currentTheme.getRgbColor();

        String[] leftLabels = {
                "⏱ Задержка /hub (мс)",
                "⏱ Задержка между кликами (мс)",
                "⌨ Команда для Classic",
                "⌨ Команда для Lite",
                "⌨ Команда для Lite 1.20"
        };

        for (int i = 0; i < leftLabels.length; i++) {
            int y = startY + spacing * i - 20;
            renderLabel(context, leftColX, y, 300, leftLabels[i], themeColor);
        }

        String[] rightLabels = {
                "🔔 Настройки уведомлений",
                "🎨 Цветовая тема интерфейса",
                "🔗 Цвет названий серверов"
        };

        for (int i = 0; i < rightLabels.length; i++) {
            int y = startY + spacing * i - 20;
            renderLabel(context, rightColX, y, 300, rightLabels[i], themeColor);
        }
    }

    private void renderLabel(DrawContext context, int x, int y, int width, String label, int themeColor) {
        int alpha = (int) (backgroundAlpha * 100);

        context.fill(x - 2, y, x + width + 2, y + 14, alpha << 24 | 0x1a1f3a);
        context.fill(x - 2, y, x, y + 14, (int) (backgroundAlpha * 255) << 24 | themeColor);

        context.getMatrices().push();
        context.getMatrices().translate(x + 3, y + 3, 0);
        context.getMatrices().scale(0.85f, 0.85f, 1.0f);
        context.drawText(this.textRenderer, Text.literal(label).formatted(currentTheme.getFormatting()), 0, 0, themeColor, false);
        context.getMatrices().pop();
    }

    private void renderFooterLine(DrawContext context) {
        int footerY = this.height - 50;
        int alpha = (int) (backgroundAlpha * 200);
        int themeColor = currentTheme.getRgbColor();
        context.fill(0, footerY, this.width, footerY + 2, alpha << 24 | themeColor);
    }

    @Override
    public void close() {
        if (client != null) {
            client.setScreen(parent);
        }
    }
            }
