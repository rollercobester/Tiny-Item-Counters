package qoby.tiny_item_counters.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiGraphics.class)
public abstract class GuiGraphicsMixin {

    @Shadow
    public abstract PoseStack pose();

    /**
     * Clean scale = ceil(guiScale/2) / guiScale. Smallest clean scale above 50%.
     * Examples: 2->50%, 3->66%, 4->50%, 5->60%, 6->50%, 7->57%
     */
    @Unique
    private static float tinyItemCounters$computeScale(int guiScale) {
        if (guiScale <= 0)
            return 0.5f;
        int half = (int) Math.ceil(guiScale / 2.0);
        return (float) half / guiScale;
    }

    /**
     * Offset scales with (1 - scale) - more shrink needs more offset to reach
     * corner.
     */
    @Unique
    private static void tinyItemCounters$computeOffsets(float scale, float[] out) {
        float shrink = 1f - scale;
        out[0] = 14f * shrink;
        out[1] = 5f * shrink;
    }

    /**
     * 1.21 & 1.21.1: Count is drawn inside renderItemDecorations (no separate
     * renderItemCount).
     * 1.21.2+: renderItemDecorations calls renderItemCount; we inject here to cover
     * all versions.
     */
    @Inject(method = "renderItemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V", at = @At("HEAD"))
    private void tinyItemCounters$pushScale(Font font, ItemStack stack, int x, int y, String countText,
            CallbackInfo ci) {
        // Use Window's scale factor - it's correct on load and when Auto; Options can
        // be stale
        int guiScale = (int) Math.round(Minecraft.getInstance().getWindow().getGuiScale());
        if (guiScale <= 0)
            guiScale = Minecraft.getInstance().options.guiScale().get();
        if (guiScale <= 0)
            guiScale = 2; // fallback

        float scale = tinyItemCounters$computeScale(guiScale);
        float[] offsets = new float[2];
        tinyItemCounters$computeOffsets(scale, offsets);
        float offsetX = offsets[0];
        float offsetY = offsets[1];

        int textWidth = font.width(countText);
        int textHeight = font.lineHeight;
        int centerX = Math.round(x + textWidth + offsetX);
        int centerY = Math.round(y + textHeight + offsetY);
        int backX = x + textWidth;
        int backY = y + textHeight;

        PoseStack pose = pose();
        pose.pushPose();
        pose.translate(centerX, centerY, 0);
        pose.scale(scale, scale, 1f);
        pose.translate(-backX, -backY, 0);
    }

    @Inject(method = "renderItemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V", at = @At("RETURN"))
    private void tinyItemCounters$popScale(Font font, ItemStack stack, int x, int y, String countText,
            CallbackInfo ci) {
        pose().popPose();
    }
}
