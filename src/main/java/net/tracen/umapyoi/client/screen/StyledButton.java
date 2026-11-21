package net.tracen.umapyoi.client.screen;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.tracen.umapyoi.Umapyoi;

/** Buttons drawn with the mod's own GUI art: green bar, small green button, page arrows. */
public class StyledButton extends AbstractButton {
    private static final ResourceLocation GODDESS = ResourceLocation.fromNamespaceAndPath(Umapyoi.MODID,
            "textures/gui/three_goddess.png");
    private static final ResourceLocation BLESSING = ResourceLocation.fromNamespaceAndPath(Umapyoi.MODID,
            "textures/gui/three_goddess_blessing.png");

    public enum Style {
        /** The statue's green bar (17 tall, any width up to 68). */
        BIG,
        /** Flat green button. */
        SMALL,
        /** 10x10 page arrows. */
        ARROW_LEFT, ARROW_RIGHT
    }

    private final Style style;
    private final Supplier<Component> label;
    private final Runnable action;
    private BooleanSupplier enabled = () -> true;

    public StyledButton(int x, int y, int width, int height, Style style, Supplier<Component> label, Runnable action) {
        super(x, y, width, height, Component.empty());
        this.style = style;
        this.label = label;
        this.action = action;
    }

    public StyledButton enabledWhen(BooleanSupplier enabled) {
        this.enabled = enabled;
        return this;
    }

    public void refresh() {
        this.active = this.enabled.getAsBoolean();
    }

    @Override
    public void onPress() {
        this.action.run();
    }

    @Override
    public Component getMessage() {
        return this.label.get();
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int x = this.getX(), y = this.getY();
        boolean hover = this.isHoveredOrFocused() && this.active;
        Font font = Minecraft.getInstance().font;
        Component text = this.label.get();
        switch (this.style) {
            case ARROW_LEFT, ARROW_RIGHT -> graphics.blit(BLESSING, x, y,
                    180 + (this.style == Style.ARROW_LEFT ? 20 : 0) + (hover ? 10 : 0), 0, 10, 10, 256, 128);
            case BIG -> {
                int v = this.active ? 4 : 23;
                graphics.blit(GODDESS, x, y, 181, v, this.width - 6, this.height);
                graphics.blit(GODDESS, x + this.width - 6, y, 181 + 62, v, 6, this.height);
                int color = this.active ? (hover ? 0xFFFFA0 : 0xFFFFFF) : 0xB8C8A0;
                graphics.drawString(font, text, x + (this.width - font.width(text)) / 2, y + 4, color, this.active);
            }
            case SMALL -> {
                int body = !this.active ? 0xFF5D7F2A : hover ? 0xFFA6E61E : 0xFF8FD40A;
                graphics.fill(x, y, x + this.width, y + this.height, 0xFF3F7A06);
                graphics.fill(x + 1, y + 1, x + this.width - 1, y + this.height - 1, body);
                graphics.drawString(font, text, x + (this.width - font.width(text)) / 2, y + (this.height - 8) / 2,
                        this.active ? 0xFFFFFF : 0xB8C8A0, false);
            }
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        this.defaultButtonNarrationText(output);
    }
}
