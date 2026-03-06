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
import qoby.tiny_item_counters.TinyItemCountersConfig;

@Mixin(GuiGraphics.class)
public abstract class GuiGraphicsMixin {

    @Shadow
    public abstract PoseStack pose();

    @Shadow
    public abstract int drawString(Font font, String text, int x, int y, int color, boolean shadow);

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

    @Unique
    private static int tinyItemCounters$getGuiScale() {
        int guiScale = (int) Math.round(Minecraft.getInstance().getWindow().getGuiScale());
        if (guiScale <= 0)
            guiScale = Minecraft.getInstance().options.guiScale().get();
        if (guiScale <= 0)
            guiScale = 2; // fallback
        return guiScale;
    }

    /**
     * Cancel vanilla's renderItemCount and redraw the text scaled.
     * Vanilla: pose.translate(0,0,200), drawString at (x+19-2-font.width(text),
     * y+9).
     * All coordinates are in GUI pixels (1 GUI px = guiScale screen px).
     * Changing drawX/drawY by ±1 moves by exactly 1 GUI pixel.
     */
    @Inject(method = "renderItemCount(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V", at = @At("HEAD"), cancellable = true)
    private void tinyItemCounters$renderCountScaled(Font font, ItemStack stack, int x, int y, String countText,
            CallbackInfo ci) {
        if (!TinyItemCountersConfig.shrinkItemCount)
            return;

        // Mirror vanilla's early-out: nothing to draw
        String text = countText != null ? countText : (stack.getCount() != 1 ? String.valueOf(stack.getCount()) : null);
        if (text == null)
            return;

        int guiScale = tinyItemCounters$getGuiScale();
        float scale = tinyItemCounters$computeScale(guiScale);

        // Scale pivot: fixed to slot bottom-right. Do not adjust for positioning.
        float anchorX = x + 17;
        float anchorY = y + 17;

        // Draw position: tune these to reposition. Each ±1 = exactly 1 GUI pixel.
        int drawX = Math.round(anchorX) - font.width(text) - 2;
        int drawY = Math.round(anchorY) - font.lineHeight - 1;

        PoseStack pose = pose();
        pose.pushPose();
        pose.translate(0, 0, 200f);
        pose.translate(anchorX, anchorY, 0f);
        pose.scale(scale, scale, 1f);
        pose.translate(-anchorX, -anchorY, 0f);

        drawString(font, text, drawX, drawY, 0xFFFFFF, true);

        pose.popPose();
        ci.cancel();
    }

    /**
     * Push a 2/3 uniform scale anchored to the bottom-right corner of the slot
     * (x+16, y+16). Only active when the item has a durability bar.
     */
    @Inject(method = "renderItemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V", at = @At("HEAD"))
    private void tinyItemCounters$pushBarScale(Font font, ItemStack stack, int x, int y, String countText,
            CallbackInfo ci) {
        if (!stack.isBarVisible())
            return;
        if (!TinyItemCountersConfig.shrinkDurabilityBar)
            return;

        float anchorX = x + 16;
        float anchorY = y + 16;

        PoseStack pose = pose();
        pose.pushPose();
        pose.translate(anchorX, anchorY, 0);
        pose.scale(2f / 3f, 2f / 3f, 1f);
        pose.translate(-anchorX, -anchorY, 0);
    }

    @Inject(method = "renderItemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V", at = @At("RETURN"))
    private void tinyItemCounters$popBarScale(Font font, ItemStack stack, int x, int y, String countText,
            CallbackInfo ci) {
        if (!stack.isBarVisible())
            return;
        if (!TinyItemCountersConfig.shrinkDurabilityBar)
            return;
        pose().popPose();
    }
}
