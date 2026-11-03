package qoby.tiny_item_counters.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3x2fStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import qoby.tiny_item_counters.TinyItemCountersConfig;

// 26.1+: GuiGraphics was renamed to GuiGraphicsExtractor; unobfuscated so @Shadow resolves at compile time
@Mixin(GuiGraphicsExtractor.class)
public abstract class GuiGraphicsMixin {

    // Mojang name: pose() (was getMatrices() in Yarn 1.21.6)
    @Shadow
    public abstract Matrix3x2fStack pose();

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

    /**
     * Half a GUI pixel is a whole screen pixel only when the GUI scale is even.
     * On odd scales it lands between pixels and the glyphs stretch, so snap to the
     * nearest screen pixel.
     */
    @Unique
    private static float tinyItemCounters$snapToScreenPixel(float guiPixels, int guiScale) {
        return Math.round(guiPixels * guiScale) / (float) guiScale;
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
     * 26.1 itemCount draws at (x + 17 - width, y + 9). Cancel that and redraw
     * scaled
     * around the text's bottom-right so the corner stays where vanilla put it.
     * The durability-bar scale is applied only inside itemBar, so it does not move
     * this text.
     */
    @Inject(method = "itemCount(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V", at = @At("HEAD"), cancellable = true)
    private void tinyItemCounters$renderCountScaled(Font font, ItemStack stack, int x, int y, String countText,
            CallbackInfo ci) {
        if (!TinyItemCountersConfig.shrinkItemCount)
            return;

        String text = countText != null ? countText : (stack.getCount() != 1 ? String.valueOf(stack.getCount()) : null);
        if (text == null) {
            ci.cancel();
            return;
        }

        int guiScale = tinyItemCounters$getGuiScale();
        float scale = tinyItemCounters$computeScale(guiScale);

        int textWidth = font.width(text);
        // Inside the scale: 1 unit moves `scale` GUI pixels (0.5 at GUI scale 2).
        int drawX = x + 16 - textWidth;
        int drawY = y + 8;

        float anchorX = drawX + textWidth;
        float anchorY = drawY + font.lineHeight;

        // GUI pixels, outside the text scale. Snapped so odd GUI scales stay on a real
        // pixel.
        float nudgeX = tinyItemCounters$snapToScreenPixel(0f, guiScale);
        float nudgeY = tinyItemCounters$snapToScreenPixel(-0.5f, guiScale);

        Matrix3x2fStack matrices = pose();
        matrices.pushMatrix();
        matrices.translate(nudgeX, nudgeY);
        matrices.translate(anchorX, anchorY);
        matrices.scale(scale, scale);
        matrices.translate(-anchorX, -anchorY);

        ((GuiGraphicsExtractor) (Object) this).text(font, text, drawX, drawY, 0xFFFFFFFF, true);

        matrices.popMatrix();
        ci.cancel();
    }

    /**
     * Scale only the durability bar. itemDecorations also draws the count, so
     * scaling
     * the whole method shifts the number.
     */
    @Inject(method = "itemBar(Lnet/minecraft/world/item/ItemStack;II)V", at = @At("HEAD"))
    private void tinyItemCounters$pushBarScale(ItemStack stack, int x, int y, CallbackInfo ci) {
        if (!stack.isBarVisible())
            return;
        if (!TinyItemCountersConfig.shrinkDurabilityBar)
            return;

        float scale = 2f / 3f;
        float anchorX = x + 16;
        float anchorY = y + 16;

        Matrix3x2fStack matrices = pose();
        matrices.pushMatrix();
        matrices.translate(anchorX, anchorY);
        matrices.scale(scale, scale);
        matrices.translate(-anchorX, -anchorY);
        tinyItemCounters$barScaleActive = true;
    }

    @Inject(method = "itemBar(Lnet/minecraft/world/item/ItemStack;II)V", at = @At("RETURN"))
    private void tinyItemCounters$popBarScale(ItemStack stack, int x, int y, CallbackInfo ci) {
        if (!tinyItemCounters$barScaleActive)
            return;
        pose().popMatrix();
        tinyItemCounters$barScaleActive = false;
    }
}
