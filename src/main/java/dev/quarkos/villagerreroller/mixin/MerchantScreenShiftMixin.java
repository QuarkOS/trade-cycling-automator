package dev.quarkos.villagerreroller.mixin;

import dev.quarkos.villagerreroller.picker.GuiDraw;
import dev.quarkos.villagerreroller.picker.TradePickerUi;
//? if <1.20 {
/*import com.mojang.blaze3d.vertex.PoseStack;
*///?} else {
import net.minecraft.client.gui.GuiGraphics;
//?}
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * MerchantScreen paints its background, trade icons, and experience bar from
 * {@code (width - imageWidth) / 2} every frame, and ignores {@code leftPos}.
 * Moving {@code leftPos} therefore slid the title, slots, and offer buttons
 * while the texture and the green experience bar stayed centered.
 * These redirects make that centering math follow {@code leftPos} and {@code topPos}.
 * The handler argument is {@link MerchantScreen} because the field read's owner
 * in bytecode is MerchantScreen, not Screen.
 */
@Mixin(MerchantScreen.class)
public abstract class MerchantScreenShiftMixin extends AbstractContainerScreen<MerchantMenu> {
    @Unique
    private boolean villagerReroller$compactClick;

    static {
        TradePickerUi.merchantBackgroundFollowsLeftPos = true;
    }

    protected MerchantScreenShiftMixin(MerchantMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"))
    private void rememberCompactClick(CallbackInfoReturnable<Boolean> callback) {
        villagerReroller$compactClick = TradePickerUi.compactShown();
    }

    // The release of Back belongs to the compact panel, even after the inventory reappears.
    //? if <1.21.9 {
    /*@Override
    protected boolean hasClickedOutside(double mouseX, double mouseY, int left, int top, int button) {
        return !villagerReroller$compactClick && !TradePickerUi.compactShown()
                && super.hasClickedOutside(mouseX, mouseY, left, top, button);
    }
    *///?} else {
    @Override
    protected boolean hasClickedOutside(double mouseX, double mouseY, int left, int top) {
        return !villagerReroller$compactClick && !TradePickerUi.compactShown()
                && super.hasClickedOutside(mouseX, mouseY, left, top);
    }
    //?}

    //? if >=26.1 {
    /*@Inject(method = "extractContents", at = @At("RETURN"))
    *///?} elif >=1.21.9 {
    @Inject(method = "renderContents", at = @At("RETURN"))
    //?} else {
    /*@Inject(method = "render", at = @At("RETURN"))
    *///?}
    //? if <1.20 {
    /*private void drawModTooltip(PoseStack graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
    *///?} else {
    private void drawModTooltip(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
    //?}
        TradePickerUi.offerButtonTooltip(GuiDraw.of(graphics), mouseX, mouseY);
        GuiDraw.drawTooltip(graphics);
    }

    //? if >=26.1 {
    /*@Redirect(method = {"extractBackground", "extractContents", "mouseClicked"}, at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/screens/inventory/MerchantScreen;width:I"))
    private int alignedWidth(MerchantScreen ignored) {
        return this.leftPos * 2 + this.imageWidth;
    }

    @Redirect(method = {"extractBackground", "extractContents", "mouseClicked"}, at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/screens/inventory/MerchantScreen;height:I"))
    private int alignedHeight(MerchantScreen ignored) {
        return this.topPos * 2 + this.imageHeight;
    }
    *///?} elif >=1.21.9 {
    @Redirect(method = {"renderBg", "renderContents", "mouseClicked"}, at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/screens/inventory/MerchantScreen;width:I"))
    private int alignedWidth(MerchantScreen ignored) {
        return this.leftPos * 2 + this.imageWidth;
    }

    @Redirect(method = {"renderBg", "renderContents", "mouseClicked"}, at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/screens/inventory/MerchantScreen;height:I"))
    private int alignedHeight(MerchantScreen ignored) {
        return this.topPos * 2 + this.imageHeight;
    }
    //?} else {
    /*@Redirect(method = {"renderBg", "render", "mouseClicked"}, at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/screens/inventory/MerchantScreen;width:I"))
    private int alignedWidth(MerchantScreen ignored) {
        return this.leftPos * 2 + this.imageWidth;
    }

    @Redirect(method = {"renderBg", "render", "mouseClicked"}, at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/screens/inventory/MerchantScreen;height:I"))
    private int alignedHeight(MerchantScreen ignored) {
        return this.topPos * 2 + this.imageHeight;
    }
    *///?}
}
