package qoby.tiny_item_counters;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;

public class ConfigScreen extends Screen {

    private static final int BUTTON_WIDTH = 200;
    private static final int BUTTON_HEIGHT = 20;
    private static final int SPACING = 4;

    private final Screen parent;

    public ConfigScreen(Screen parent) {
        super(Text.translatable("tiny-item-counters.config.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int startY = this.height / 2 - (BUTTON_HEIGHT * 2 + SPACING) / 2;

        addDrawableChild(ButtonWidget.builder(
                toggleLabel("tiny-item-counters.config.option.shrink_item_count", TinyItemCountersConfig.shrinkItemCount),
                button -> {
                    TinyItemCountersConfig.shrinkItemCount = !TinyItemCountersConfig.shrinkItemCount;
                    button.setMessage(toggleLabel("tiny-item-counters.config.option.shrink_item_count", TinyItemCountersConfig.shrinkItemCount));
                    TinyItemCountersConfig.save();
                })
                .dimensions(centerX - BUTTON_WIDTH / 2, startY, BUTTON_WIDTH, BUTTON_HEIGHT)
                .tooltip(Tooltip.of(Text.translatable("tiny-item-counters.config.option.shrink_item_count.desc")))
                .build());

        addDrawableChild(ButtonWidget.builder(
                toggleLabel("tiny-item-counters.config.option.shrink_durability_bar", TinyItemCountersConfig.shrinkDurabilityBar),
                button -> {
                    TinyItemCountersConfig.shrinkDurabilityBar = !TinyItemCountersConfig.shrinkDurabilityBar;
                    button.setMessage(toggleLabel("tiny-item-counters.config.option.shrink_durability_bar", TinyItemCountersConfig.shrinkDurabilityBar));
                    TinyItemCountersConfig.save();
                })
                .dimensions(centerX - BUTTON_WIDTH / 2, startY + BUTTON_HEIGHT + SPACING, BUTTON_WIDTH, BUTTON_HEIGHT)
                .tooltip(Tooltip.of(Text.translatable("tiny-item-counters.config.option.shrink_durability_bar.desc")))
                .build());

        addDrawableChild(ButtonWidget.builder(ScreenTexts.DONE, button -> close())
                .dimensions(centerX - BUTTON_WIDTH / 2, startY + (BUTTON_HEIGHT + SPACING) * 2 + SPACING * 2, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 20, 0xFFFFFF);
    }

    @Override
    public void close() {
        this.client.setScreen(parent);
    }

    private static Text toggleLabel(String key, boolean value) {
        return Text.translatable(key)
                .append(": ")
                .append(value ? ScreenTexts.ON : ScreenTexts.OFF);
    }
}
