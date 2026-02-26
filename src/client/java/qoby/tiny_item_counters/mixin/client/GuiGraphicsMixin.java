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

    /**
     * Offset scales with (1 - scale) - more shrink needs more offset to reach corner.
     */
    @Unique
    private static void tinyItemCounters$computeOffsets(float scale, float[] out) {
        float shrink = 1f - scale;
        out[0] = 14f * shrink;
        out[1] = 5f * shrink;
    }

    /**
     * 1.21.6+: drawStackOverlay (was renderItemDecorations). DrawContext uses Matrix3x2fStack.
     */
    @Inject(method = "drawStackOverlay(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/item/ItemStack;IILjava/lang/String;)V", at = @At("HEAD"))
    private void tinyItemCounters$pushScale(TextRenderer textRenderer, ItemStack stack, int x, int y, String countText,
            CallbackInfo ci) {
        int guiScale = MinecraftClient.getInstance().getWindow().getScaleFactor();
        if (guiScale <= 0) guiScale = MinecraftClient.getInstance().options.getGuiScale().getValue();
        if (guiScale <= 0) guiScale = 2; // fallback

        float scale = tinyItemCounters$computeScale(guiScale);
        float[] offsets = new float[2];
        tinyItemCounters$computeOffsets(scale, offsets);
        float offsetX = offsets[0];
        float offsetY = offsets[1];

        int textWidth = textRenderer.getWidth(countText);
        int textHeight = textRenderer.fontHeight;
        float centerX = x + textWidth + offsetX;
        float centerY = y + textHeight + offsetY;
        float backX = x + textWidth;
        float backY = y + textHeight;

        Matrix3x2fStack matrices = getMatrices();
        matrices.pushMatrix();
        matrices.translate(centerX, centerY);
        matrices.scale(scale, scale);
        matrices.translate(-backX, -backY);
    }

    @Inject(method = "drawStackOverlay(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/item/ItemStack;IILjava/lang/String;)V", at = @At("RETURN"))
    private void tinyItemCounters$popScale(TextRenderer textRenderer, ItemStack stack, int x, int y, String countText,
            CallbackInfo ci) {
        getMatrices().popMatrix();
    }
}
