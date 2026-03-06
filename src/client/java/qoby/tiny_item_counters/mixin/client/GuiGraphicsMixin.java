package qoby.tiny_item_counters.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import org.joml.Matrix3x2fStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import qoby.tiny_item_counters.TinyItemCountersConfig;

@Mixin(DrawContext.class)
public abstract class GuiGraphicsMixin {

    @Shadow
    public abstract Matrix3x2fStack getMatrices();

    /**
     * Clean scale = ceil(guiScale/2) / guiScale. Smallest clean scale above 50%.
     * Examples: 2->50%, 3->66%, 4->50%, 5->60%, 6->50%, 7->57%
     */
    @Unique
    private static float tinyItemCounters$computeScale(int guiScale) {
        if (guiScale <= 0) return 0.5f;
        int half = (int) Math.ceil(guiScale / 2.0);
        return (float) half / guiScale;
    }

    @Unique
    private static int tinyItemCounters$getGuiScale() {
        int guiScale = MinecraftClient.getInstance().getWindow().getScaleFactor();
        if (guiScale <= 0) guiScale = MinecraftClient.getInstance().options.getGuiScale().getValue();
        if (guiScale <= 0) guiScale = 2; // fallback
        return guiScale;
    }

    /**
     * 1.21.6+: replaces renderItemCount. Cancels vanilla draw and redraws scaled.
     * Anchor is fixed at slot bottom-right (x+17, y+17); drawX/drawY are derived from it.
     */
    @Inject(method = "drawStackCount(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/item/ItemStack;IILjava/lang/String;)V", at = @At("HEAD"), cancellable = true)
    private void tinyItemCounters$renderCountScaled(TextRenderer textRenderer, ItemStack stack, int x, int y, String countText,
            CallbackInfo ci) {
        if (!TinyItemCountersConfig.shrinkItemCount) return;

        String text = countText != null ? countText : (stack.getCount() != 1 ? String.valueOf(stack.getCount()) : null);
        if (text == null) {
            ci.cancel();
            return;
        }

        int guiScale = tinyItemCounters$getGuiScale();
        float scale = tinyItemCounters$computeScale(guiScale);

        float anchorX = x + 17;
        float anchorY = y + 17;

        int drawX = Math.round(anchorX) - textRenderer.getWidth(text);
        int drawY = Math.round(anchorY) - textRenderer.fontHeight;

        Matrix3x2fStack matrices = getMatrices();
        matrices.pushMatrix();
        matrices.translate(anchorX, anchorY);
        matrices.scale(scale, scale);
        matrices.translate(-anchorX, -anchorY);

        ((DrawContext) (Object) this).drawText(textRenderer, text, drawX, drawY, 0xFFFFFFFF, true);

        matrices.popMatrix();
        ci.cancel();
    }

    /**
     * Push a 2/3 uniform scale anchored to the bottom-right corner of the slot
     * (x+16, y+16). Only active when the item has a durability bar.
     */
    @Inject(method = "drawStackOverlay(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/item/ItemStack;IILjava/lang/String;)V", at = @At("HEAD"))
    private void tinyItemCounters$pushBarScale(TextRenderer textRenderer, ItemStack stack, int x, int y, String countText,
            CallbackInfo ci) {
        if (!stack.isItemBarVisible()) return;
        if (!TinyItemCountersConfig.shrinkDurabilityBar) return;

        float scale = 2f / 3f;
        float anchorX = x + 16;
        float anchorY = y + 16;

        Matrix3x2fStack matrices = getMatrices();
        matrices.pushMatrix();
        matrices.translate(anchorX, anchorY);
        matrices.scale(scale, scale);
        matrices.translate(-anchorX, -anchorY);
    }

    @Inject(method = "drawStackOverlay(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/item/ItemStack;IILjava/lang/String;)V", at = @At("RETURN"))
    private void tinyItemCounters$popBarScale(TextRenderer textRenderer, ItemStack stack, int x, int y, String countText,
            CallbackInfo ci) {
        if (!stack.isItemBarVisible()) return;
        if (!TinyItemCountersConfig.shrinkDurabilityBar) return;
        getMatrices().popMatrix();
    }
}
