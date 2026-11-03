package qoby.tiny_item_counters.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Font;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3x2fStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import qoby.tiny_item_counters.TinyItemCountersConfig;

// 26.1+: unobfuscated Mojang names — no refmap needed, @Shadow resolves at compile time
@Mixin(DrawContext.class)
public abstract class GuiGraphicsMixin {

    // Mojang name: pose() (was getMatrices() in Yarn 1.21.6)
    @Shadow
    public abstract Matrix3x2fStack pose();

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
        // 26.1 Mojang: Minecraft.getInstance().getWindow().getGuiScale() returns double
        int guiScale = (int) Math.round(Minecraft.getInstance().getWindow().getGuiScale());
        if (guiScale <= 0)
            guiScale = Minecraft.getInstance().options.guiScale().get();
        if (guiScale <= 0)
            guiScale = 2; // fallback
        return guiScale;
    }

    /**
     * 26.1+: itemCount() is the Mojang name for drawStackCount() (Yarn 1.21.6).
     * Cancels vanilla draw and redraws scaled.
     * Anchor is fixed at slot bottom-right (x+17, y+17); drawX/drawY are derived from it.
     */
    @Inject(method = "itemCount(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V", at = @At("HEAD"), cancellable = true)
    private void tinyItemCounters$renderCountScaled(Font font, ItemStack stack, int x, int y, String countText,
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

        int drawX = Math.round(anchorX) - font.width(text);
        int drawY = Math.round(anchorY) - font.lineHeight;

        Matrix3x2fStack matrices = pose();
        matrices.pushMatrix();
        matrices.translate(anchorX, anchorY);
        matrices.scale(scale, scale);
        matrices.translate(-anchorX, -anchorY);

        // 26.1 Mojang: text() (was drawText() in Yarn 1.21.6); returns void
        ((DrawContext) (Object) this).text(font, text, drawX, drawY, 0xFFFFFFFF, true);

        matrices.popMatrix();
        ci.cancel();
    }

    /**
     * 26.1+: itemDecorations() is the Mojang name for drawStackOverlay() (Yarn 1.21.6).
     * Push a 2/3 uniform scale anchored to the bottom-right of the slot (x+16, y+16).
     * Only active when the item has a durability bar.
     */
    @Inject(method = "itemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V", at = @At("HEAD"))
    private void tinyItemCounters$pushBarScale(Font font, ItemStack stack, int x, int y, String countText,
            CallbackInfo ci) {
        // 26.1 Mojang: isBarVisible() (was isItemBarVisible() in Yarn 1.21.6)
        if (!stack.isBarVisible()) return;
        if (!TinyItemCountersConfig.shrinkDurabilityBar) return;

        float scale = 2f / 3f;
        float anchorX = x + 16;
        float anchorY = y + 16;

        Matrix3x2fStack matrices = pose();
        matrices.pushMatrix();
        matrices.translate(anchorX, anchorY);
        matrices.scale(scale, scale);
        matrices.translate(-anchorX, -anchorY);
    }

    @Inject(method = "itemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V", at = @At("RETURN"))
    private void tinyItemCounters$popBarScale(Font font, ItemStack stack, int x, int y, String countText,
            CallbackInfo ci) {
        if (!stack.isBarVisible()) return;
        if (!TinyItemCountersConfig.shrinkDurabilityBar) return;
        pose().popMatrix();
    }
}
