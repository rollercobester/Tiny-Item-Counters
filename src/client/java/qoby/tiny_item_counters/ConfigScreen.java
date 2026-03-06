package qoby.tiny_item_counters;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public class ConfigScreen extends Screen {

    private static final int BUTTON_WIDTH = 200;
    private static final int BUTTON_HEIGHT = 20;
    private static final int SPACING = 4;

    private final Screen parent;

    public ConfigScreen(Screen parent) {
        super(Component.translatable("tiny-item-counters.config.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int startY = this.height / 2 - (BUTTON_HEIGHT * 2 + SPACING) / 2;

        addRenderableWidget(Button.builder(
                toggleLabel("tiny-item-counters.config.option.shrink_item_count", TinyItemCountersConfig.shrinkItemCount),
                button -> {
                    TinyItemCountersConfig.shrinkItemCount = !TinyItemCountersConfig.shrinkItemCount;
                    button.setMessage(toggleLabel("tiny-item-counters.config.option.shrink_item_count", TinyItemCountersConfig.shrinkItemCount));
                    TinyItemCountersConfig.save();
                })
                .bounds(centerX - BUTTON_WIDTH / 2, startY, BUTTON_WIDTH, BUTTON_HEIGHT)
                .tooltip(Tooltip.create(Component.translatable("tiny-item-counters.config.option.shrink_item_count.desc")))
                .build());

        addRenderableWidget(Button.builder(
                toggleLabel("tiny-item-counters.config.option.shrink_durability_bar", TinyItemCountersConfig.shrinkDurabilityBar),
                button -> {
                    TinyItemCountersConfig.shrinkDurabilityBar = !TinyItemCountersConfig.shrinkDurabilityBar;
                    button.setMessage(toggleLabel("tiny-item-counters.config.option.shrink_durability_bar", TinyItemCountersConfig.shrinkDurabilityBar));
                    TinyItemCountersConfig.save();
                })
                .bounds(centerX - BUTTON_WIDTH / 2, startY + BUTTON_HEIGHT + SPACING, BUTTON_WIDTH, BUTTON_HEIGHT)
                .tooltip(Tooltip.create(Component.translatable("tiny-item-counters.config.option.shrink_durability_bar.desc")))
                .build());

        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
                .bounds(centerX - BUTTON_WIDTH / 2, startY + (BUTTON_HEIGHT + SPACING) * 2 + SPACING * 2, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());
    }

    @Override
    public void render(net.minecraft.client.gui.GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        super.render(graphics, mouseX, mouseY, delta);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }

    private static Component toggleLabel(String key, boolean value) {
        return Component.translatable(key)
                .append(": ")
                .append(value ? CommonComponents.OPTION_ON : CommonComponents.OPTION_OFF);
    }
}
