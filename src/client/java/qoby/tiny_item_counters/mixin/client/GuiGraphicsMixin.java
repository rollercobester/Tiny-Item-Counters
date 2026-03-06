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
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import qoby.tiny_item_counters.TinyItemCountersConfig;

@Mixin(GuiGraphics.class)
public abstract class GuiGraphicsMixin {

    @Shadow
    public abstract PoseStack pose();

    /**
     * While the durability bar scale transform is active, the drawString redirect
     * must pop it before drawing text (which applies its own transform) and then
     * re-push it after, so the pop at RETURN restores cleanly.
     */
    @Unique
    private boolean tinyItemCounters$barScaleActive = false;

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

        float scale = 2f / 3f;

        float anchorX = x + 16;
        float anchorY = y + 16;

        PoseStack pose = pose();
        pose.pushPose();
        pose.translate(anchorX, anchorY, 0);
        pose.scale(scale, scale, 1f);
        pose.translate(-anchorX, -anchorY, 0);
        tinyItemCounters$barScaleActive = true;
    }

    @Inject(method = "renderItemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V", at = @At("RETURN"))
    private void tinyItemCounters$popBarScale(Font font, ItemStack stack, int x, int y, String countText,
            CallbackInfo ci) {
        if (!tinyItemCounters$barScaleActive)
            return;
        pose().popPose();
        tinyItemCounters$barScaleActive = false;
    }

    /**
     * Redirect the drawString call that renders the count text.
     * If the bar scale transform is currently active (item has a durability bar),
     * pop it first so the text transform works in unscaled space, apply the text
     * scale, draw, then push a plain pose so RETURN's popPose still balances.
     */
    @Redirect(method = "renderItemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;IIIZ)I"))
    private int tinyItemCounters$redirectCountDraw(GuiGraphics instance, Font font, String text, int x, int y,
            int color, boolean shadow) {
        int guiScale = tinyItemCounters$getGuiScale();
        float scale = tinyItemCounters$computeScale(guiScale);

        if (!TinyItemCountersConfig.shrinkItemCount) {
            if (tinyItemCounters$barScaleActive)
                pose().popPose();
            int result = instance.drawString(font, text, x, y, color, shadow);
            if (tinyItemCounters$barScaleActive)
                pose().pushPose();
            return result;
        }

        float anchorX = x + font.width(text) - 2;
        float anchorY = y + font.lineHeight - 3;

        PoseStack pose = pose();

        // Bar fills are already done; pop bar transform before drawing text.
        if (tinyItemCounters$barScaleActive)
            pose.popPose();

        pose.pushPose();
        pose.translate(anchorX, anchorY, 0);
        pose.scale(scale, scale, 1f);
        pose.translate(-anchorX, -anchorY, 0);

        int result = instance.drawString(font, text, x, y, color, shadow);

        pose.popPose();

        // Push a plain pose to replace the one we popped, so RETURN's popPose balances.
        if (tinyItemCounters$barScaleActive)
            pose.pushPose();

        return result;
    }
}
