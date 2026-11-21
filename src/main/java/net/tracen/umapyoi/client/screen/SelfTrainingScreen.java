package net.tracen.umapyoi.client.screen;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.tracen.umapyoi.Umapyoi;
import net.tracen.umapyoi.career.CareerManager;
import net.tracen.umapyoi.career.SkillOffer;
import net.tracen.umapyoi.container.SelfTrainingContainer;
import net.tracen.umapyoi.item.data.DataComponentsTypeRegistry;
import net.tracen.umapyoi.registry.UmaSkillRegistry;
import net.tracen.umapyoi.registry.skills.UmaSkill;
import net.tracen.umapyoi.registry.training.SupportContainer;
import net.tracen.umapyoi.trait.PersonalityTrait;
import net.tracen.umapyoi.trait.UmaTrait;
import net.tracen.umapyoi.trait.UmaTraits;
import net.tracen.umapyoi.utils.UmaSoulUtils;

/** Self-training terminal: main page (cards, soul, grades), personality page, skill page. */
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class SelfTrainingScreen extends AbstractContainerScreen<SelfTrainingContainer> {
    private static final ResourceLocation MAIN = ResourceLocation.fromNamespaceAndPath(Umapyoi.MODID,
            "textures/gui/self_training_main.png");
    private static final ResourceLocation PAGE = ResourceLocation.fromNamespaceAndPath(Umapyoi.MODID,
            "textures/gui/self_training_page.png");
    private static final ResourceLocation SNAP = ResourceLocation.fromNamespaceAndPath(Umapyoi.MODID,
            "textures/gui/training_snap.png");

    private static final int DARK = 0x404040, GRAY = 0x8A8A8A, GREEN = 0x3C9A14, RED = 0xC03030;
    private static final int LIST_X = 8, LIST_W = 151, ROW_H = 20;
    private static final int POSITIVE_Y = 62, POSITIVE_ROWS = 4, SKILL_Y = 25, SKILL_ROWS = 6;
    private static final String[] STATS = { "speed", "stamina", "strength", "guts", "wisdom" };

    private StyledButton personalityButton, skillButton, startButton, backButton;
    private int positiveScroll, skillScroll;
    private int[] guaranteed = new int[5];

    public SelfTrainingScreen(SelfTrainingContainer menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 270;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        int x = this.leftPos, y = this.topPos;
        this.personalityButton = this.addRenderableWidget(new StyledButton(x + 40, y + 151, 38, 14,
                StyledButton.Style.SMALL, () -> Component.translatable("gui.umapyoi.self_training.personality"),
                () -> this.showPage(SelfTrainingContainer.PAGE_PERSONALITY)));
        this.skillButton = this.addRenderableWidget(new StyledButton(x + 82, y + 151, 38, 14,
                StyledButton.Style.SMALL, () -> Component.translatable("gui.umapyoi.self_training.skill"),
                () -> this.showPage(SelfTrainingContainer.PAGE_SKILL)));
        this.startButton = this.addRenderableWidget(new StyledButton(x + 124, y + 150, 46, 17,
                StyledButton.Style.BIG, () -> Component.translatable("gui.umapyoi.self_training.start"),
                () -> this.send(SelfTrainingContainer.BUTTON_START)).enabledWhen(this::canStart));
        this.backButton = this.addRenderableWidget(new StyledButton(x + 163, y + 1, 10, 10,
                StyledButton.Style.ARROW_LEFT, Component::empty, () -> this.showPage(SelfTrainingContainer.PAGE_MAIN)));
        this.updateWidgets();
    }

    private ItemStack soul() {
        return this.menu.inventory.getStackInSlot(0);
    }

    private boolean hasSoul() {
        return CareerManager.isUndecided(this.soul());
    }

    private boolean canStart() {
        return this.hasSoul() && !UmaTraits.of(this.soul()).positives().isEmpty();
    }

    private void send(int id) {
        if (this.minecraft != null && this.minecraft.gameMode != null)
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
    }

    private void showPage(int page) {
        this.menu.page = page;
        this.updateWidgets();
    }

    private void updateWidgets() {
        if (this.startButton == null)
            return;
        boolean main = this.menu.page == SelfTrainingContainer.PAGE_MAIN;
        this.personalityButton.visible = main;
        this.skillButton.visible = main;
        this.startButton.visible = main;
        this.backButton.visible = !main;
        this.personalityButton.active = this.hasSoul();
        this.skillButton.active = this.hasSoul();
        this.startButton.refresh();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (!this.hasSoul() && this.menu.page != SelfTrainingContainer.PAGE_MAIN)
            this.menu.page = SelfTrainingContainer.PAGE_MAIN;
        this.updateWidgets();
        // live "guaranteed" preview: worst-case auto-training with the cards in the slots
        ItemStack soul = this.soul();
        if (this.hasSoul() && this.minecraft != null && this.minecraft.level != null) {
            int[] growth = CareerManager.simulateWorstCase(this.minecraft.level, this.menu.inventory, soul).growth();
            int[] current = UmaSoulUtils.getProperty(soul).toArray();
            int[] max = UmaSoulUtils.getMaxProperty(soul).toArray();
            for (int i = 0; i < 5; i++)
                this.guaranteed[i] = Math.min(max[i], current[i] + CareerManager.baseReward(growth[i]));
        }
    }

    // ---------------------------------------------------------------- background

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos, y = this.topPos;
        if (this.menu.page != SelfTrainingContainer.PAGE_MAIN) {
            graphics.blit(PAGE, x, y, 0, 0, this.imageWidth, this.imageHeight, 256, 272);
            if (this.menu.page == SelfTrainingContainer.PAGE_PERSONALITY)
                this.renderPersonalityBg(graphics, x, y);
            else
                this.renderSkillBg(graphics, x, y);
            return;
        }
        graphics.blit(MAIN, x, y, 0, 0, this.imageWidth, this.imageHeight, 256, 272);
        boolean[] types = new boolean[7];
        for (int i = 1; i < 7; i++) {
            ItemStack card = this.menu.inventory.getStackInSlot(i);
            if (!(card.getItem() instanceof SupportContainer support) || this.minecraft == null)
                continue;
            int u = switch (support.getSupportLevel(this.minecraft.level, card)) {
                case SR -> 198;
                case SSR -> 225;
                default -> 171;
            };
            graphics.blit(SNAP, x + 7, y + 14 + (i - 1) * 26, u, 205, 26, 26);
            int type = support.getSupportType(this.minecraft.level, card).ordinal();
            if (type < 7)
                types[type] = true;
        }
        for (int t = 0; t < 7; t++)
            if (types[t])
                graphics.blit(SNAP, x + 40 + t * 18, y + 104, 3 + t * 23, 219, 12, 12);
    }

    private static void box(GuiGraphics graphics, int x, int y, int w, int h, int fill, int border) {
        graphics.fill(x, y, x + w, y + h, border);
        graphics.fill(x + 1, y + 1, x + w - 1, y + h - 1, fill);
    }

    private void renderList(GuiGraphics graphics, int x, int y, int rows, int count, int scroll, int selected) {
        box(graphics, x + LIST_X, y, LIST_W, rows * ROW_H + 2, 0xFFF4F4F4, 0xFFB8B8B8);
        for (int r = 0; r < rows && scroll + r < count; r++) {
            if (scroll + r == selected)
                box(graphics, x + LIST_X + 1, y + 1 + r * ROW_H, LIST_W - 2, ROW_H, 0xFFFFF3C4, 0xFFE6B04A);
        }
        int track = rows * ROW_H + 2;
        box(graphics, x + LIST_X + LIST_W + 2, y, 7, track, 0xFFD0D0D0, 0xFFA0A0A0);
        int thumb = count <= rows ? track : Math.max(8, track * rows / count);
        int thumbY = count <= rows ? 0 : (track - thumb) * scroll / (count - rows);
        box(graphics, x + LIST_X + LIST_W + 2, y + thumbY, 7, thumb, 0xFF8A8A8A, 0xFF5A5A5A);
    }

    private void renderPersonalityBg(GuiGraphics graphics, int x, int y) {
        box(graphics, x + 8, y + 25, 160, 22, 0xFFF6E3E3, 0xFFC89A9A);
        List<PersonalityTrait> positives = PersonalityTrait.positives();
        int selected = positives.indexOf(this.chosenPositive());
        this.renderList(graphics, x, y + POSITIVE_Y, POSITIVE_ROWS, positives.size(), this.positiveScroll, selected);
    }

    private void renderSkillBg(GuiGraphics graphics, int x, int y) {
        ResourceLocation chosen = this.soul().get(DataComponentsTypeRegistry.CAREER_SKILL);
        int selected = chosen == null ? -1 : SkillOffer.SKILLS.indexOf(chosen);
        this.renderList(graphics, x, y + SKILL_Y, SKILL_ROWS, SkillOffer.SKILLS.size(), this.skillScroll, selected);
    }

    @Nullable
    private PersonalityTrait chosenPositive() {
        List<PersonalityTrait> list = UmaTraits.of(this.soul()).positives();
        return list.isEmpty() ? null : list.get(0);
    }

    // ---------------------------------------------------------------- labels

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        Component title = switch (this.menu.page) {
            case SelfTrainingContainer.PAGE_PERSONALITY -> Component.translatable("gui.umapyoi.self_training.personality");
            case SelfTrainingContainer.PAGE_SKILL -> Component.translatable("gui.umapyoi.self_training.skill");
            default -> this.title;
        };
        graphics.drawString(this.font, title, (this.imageWidth - this.font.width(title)) / 2, 3, 0xFFFFFF);
        graphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, DARK, false);
        switch (this.menu.page) {
            case SelfTrainingContainer.PAGE_PERSONALITY -> this.renderPersonalityLabels(graphics);
            case SelfTrainingContainer.PAGE_SKILL -> this.renderSkillLabels(graphics);
            default -> this.renderMainLabels(graphics);
        }
    }

    /** Grade letter for a stat value; plain number outside the grade table (e.g. set by commands). */
    private static String grade(int value) {
        if (value < 0 || value > 39)
            return String.valueOf(value);
        return ChatFormatting.stripFormatting(Component.translatable("umastatus.level." + value).getString());
    }

    private static Component none() {
        return Component.translatable("gui.umapyoi.self_training.none");
    }

    private void renderMainLabels(GuiGraphics graphics) {
        ItemStack soul = this.soul();
        if (!this.hasSoul()) {
            graphics.drawString(this.font, Component.translatable("gui.umapyoi.self_training.put_soul"), 62, 19, GRAY, false);
            return;
        }
        graphics.drawString(this.font, soul.getHoverName(), 62, 15, DARK, false);
        graphics.drawString(this.font, Component.translatable("gui.umapyoi.self_training.status",
                UmaSoulUtils.getPhysique(soul)), 62, 25, GRAY, false);

        int[] current = UmaSoulUtils.getProperty(soul).toArray();
        int[] max = UmaSoulUtils.getMaxProperty(soul).toArray();
        int[] columns = this.columns(current, max);
        graphics.drawString(this.font, Component.translatable("gui.umapyoi.self_training.stat"), 40, 40, GRAY, false);
        graphics.drawString(this.font, Component.translatable("gui.umapyoi.self_training.current"), columns[0], 40, GRAY, false);
        graphics.drawString(this.font, Component.translatable("gui.umapyoi.self_training.guaranteed"), columns[1], 40, GRAY, false);
        for (int i = 0; i < 5; i++) {
            int y = 51 + i * 10;
            graphics.drawString(this.font, Component.translatable("gui.umapyoi.self_training." + STATS[i]), 40, y, DARK, false);
            graphics.drawString(this.font, grade(current[i]) + " (" + grade(max[i]) + ")", columns[0] + 2, y, DARK, false);
            graphics.drawString(this.font, grade(this.guaranteed[i]), columns[1] + 2, y, GREEN, false);
        }

        UmaTraits traits = UmaTraits.of(soul);
        MutableComponent personality = Component.translatable("gui.umapyoi.self_training.personality_line");
        List<PersonalityTrait> shown = new ArrayList<>(traits.negatives());
        shown.addAll(traits.positives());
        if (shown.isEmpty())
            personality.append(none().copy().withColor(GRAY));
        for (int i = 0; i < shown.size(); i++) {
            if (i > 0)
                personality.append(" ");
            PersonalityTrait trait = shown.get(i);
            personality.append(trait.displayName().copy().withColor(trait.positive() ? GREEN : RED));
        }
        graphics.drawString(this.font, personality, 40, 124, DARK, false);

        ResourceLocation skill = soul.get(DataComponentsTypeRegistry.CAREER_SKILL);
        Component skillName = skill != null && UmaSkillRegistry.REGISTRY.containsKey(skill)
                ? UmaSkillRegistry.REGISTRY.get(skill).getDescription()
                : Component.translatable("gui.umapyoi.self_training.not_chosen").withColor(GRAY);
        graphics.drawString(this.font, Component.translatable("gui.umapyoi.self_training.skill_line").append(skillName),
                40, 134, DARK, false);
    }

    /** x of the "current (cap)" and "guaranteed" columns, from the widths of the texts in this language. */
    private int[] columns(int[] current, int[] max) {
        int names = this.font.width(Component.translatable("gui.umapyoi.self_training.stat"));
        for (String stat : STATS)
            names = Math.max(names, this.font.width(Component.translatable("gui.umapyoi.self_training." + stat)));
        int currentX = 40 + names + 6;
        int currentW = this.font.width(Component.translatable("gui.umapyoi.self_training.current"));
        for (int i = 0; i < 5; i++)
            currentW = Math.max(currentW, this.font.width(grade(current[i]) + " (" + grade(max[i]) + ")") + 2);
        int guaranteedX = Math.max(128, currentX + currentW + 6);
        return new int[] { currentX, Math.min(guaranteedX, this.imageWidth - 24) };
    }

    private void renderPersonalityLabels(GuiGraphics graphics) {
        UmaTraits traits = UmaTraits.of(this.soul());
        graphics.drawString(this.font, Component.translatable("gui.umapyoi.self_training.negative"), 9, 15, DARK, false);
        List<PersonalityTrait> negatives = traits.negatives();
        if (negatives.isEmpty()) {
            graphics.drawString(this.font, none(), 12, 32, GRAY, false);
        } else if (negatives.size() == 1) {
            graphics.drawString(this.font, negatives.get(0).displayName(), 12, 27, RED, false);
            graphics.drawString(this.font, this.trim(negatives.get(0).description(), 152), 12, 37, GRAY, false);
        } else {
            graphics.drawString(this.font, this.trim(joined(negatives.stream().map(PersonalityTrait::displayName).toList()), 152),
                    12, 27, RED, false);
            graphics.drawString(this.font, Component.translatable("gui.umapyoi.self_training.hover_for_more"), 12, 37, GRAY, false);
        }

        graphics.drawString(this.font, Component.translatable("gui.umapyoi.self_training.positive"), 9, 52, DARK, false);
        List<PersonalityTrait> positives = PersonalityTrait.positives();
        for (int r = 0; r < POSITIVE_ROWS && this.positiveScroll + r < positives.size(); r++) {
            PersonalityTrait trait = positives.get(this.positiveScroll + r);
            boolean excluded = traits.isExcluded(trait);
            int ry = POSITIVE_Y + 1 + r * ROW_H;
            graphics.drawString(this.font, trait.displayName(), LIST_X + 4, ry + 1, excluded ? GRAY : DARK, false);
            graphics.drawString(this.font, this.trim(trait.description(), LIST_W - 8), LIST_X + 4, ry + 11, GRAY, false);
        }

        List<Component> uma = traits.umaEntries().stream().map(UmaTrait.Entry::displayName).toList();
        graphics.drawString(this.font, Component.translatable("gui.umapyoi.self_training.uma_traits")
                .append(uma.isEmpty() ? none() : joined(uma)), 9, 150, DARK, false);
        List<Component> likes = traits.likedItems().stream().map(Item::getDescription).toList();
        List<Component> dislikes = traits.dislikedItems().stream().map(Item::getDescription).toList();
        Component foods = Component.translatable("gui.umapyoi.self_training.likes")
                .append(likes.isEmpty() ? none() : joined(likes)).append("  ")
                .append(Component.translatable("gui.umapyoi.self_training.dislikes"))
                .append(dislikes.isEmpty() ? none() : joined(dislikes));
        graphics.drawString(this.font, this.trim(foods, 160), 9, 160, DARK, false);
    }

    private void renderSkillLabels(GuiGraphics graphics) {
        graphics.drawString(this.font, Component.translatable("gui.umapyoi.self_training.offered_skills"), 9, 15, DARK, false);
        List<ResourceLocation> skills = SkillOffer.SKILLS;
        for (int r = 0; r < SKILL_ROWS && this.skillScroll + r < skills.size(); r++) {
            ResourceLocation id = skills.get(this.skillScroll + r);
            if (!UmaSkillRegistry.REGISTRY.containsKey(id))
                continue;
            UmaSkill skill = UmaSkillRegistry.REGISTRY.get(id);
            int ry = SKILL_Y + 1 + r * ROW_H;
            boolean available = SkillOffer.isAvailable(this.soul(), id);
            graphics.drawString(this.font, skill.getDescription(), LIST_X + 4, ry + 1, available ? DARK : GRAY, false);
            graphics.drawString(this.font, this.trim(skill.getDescriptionDetail(), LIST_W - 8), LIST_X + 4, ry + 11, GRAY, false);
        }
        ResourceLocation chosen = this.soul().get(DataComponentsTypeRegistry.CAREER_SKILL);
        Component name = chosen != null && UmaSkillRegistry.REGISTRY.containsKey(chosen)
                ? UmaSkillRegistry.REGISTRY.get(chosen).getDescription()
                : Component.translatable("gui.umapyoi.self_training.not_chosen");
        graphics.drawString(this.font, Component.translatable("gui.umapyoi.self_training.chosen", name), 9, 154, DARK, false);
    }

    private static Component joined(List<Component> parts) {
        MutableComponent out = Component.empty();
        for (int i = 0; i < parts.size(); i++) {
            if (i > 0)
                out.append(Component.translatable("gui.umapyoi.self_training.separator"));
            out.append(parts.get(i));
        }
        return out;
    }

    /** Cuts a line that would overflow its box, with an ellipsis; the tooltip shows the full text. */
    private FormattedCharSequence trim(Component text, int width) {
        if (this.font.width(text) <= width)
            return text.getVisualOrderText();
        return this.font.split(Component.empty().append(this.font.substrByWidth(text, width - 6).getString()).append("…"),
                width + 10).get(0);
    }

    // ---------------------------------------------------------------- tooltips and clicks

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        int mx = mouseX - this.leftPos, my = mouseY - this.topPos;
        List<Component> tip = this.tooltipAt(mx, my);
        if (!tip.isEmpty()) {
            List<FormattedCharSequence> lines = new ArrayList<>();
            for (Component line : tip)
                lines.addAll(this.font.split(line, 170));
            graphics.renderTooltip(this.font, lines, mouseX, mouseY);
        } else {
            this.renderTooltip(graphics, mouseX, mouseY);
        }
    }

    private List<Component> tooltipAt(int mx, int my) {
        List<Component> tip = new ArrayList<>();
        if (!this.hasSoul())
            return tip;
        UmaTraits traits = UmaTraits.of(this.soul());
        switch (this.menu.page) {
            case SelfTrainingContainer.PAGE_MAIN -> {
                ItemStack soul = this.soul();
                int gx = this.columns(UmaSoulUtils.getProperty(soul).toArray(), UmaSoulUtils.getMaxProperty(soul).toArray())[1];
                if (mx >= gx && mx < 170 && my >= 38 && my < 100) {
                    tip.add(Component.translatable("gui.umapyoi.self_training.guaranteed").withStyle(ChatFormatting.GOLD));
                    tip.add(Component.translatable("gui.umapyoi.self_training.guaranteed.tip1"));
                    tip.add(Component.translatable("gui.umapyoi.self_training.guaranteed.tip2").withStyle(ChatFormatting.GRAY));
                    tip.add(Component.translatable("gui.umapyoi.self_training.guaranteed.tip3").withStyle(ChatFormatting.GRAY));
                } else if (mx >= 124 && mx < 170 && my >= 150 && my < 167 && !this.canStart()) {
                    tip.add(Component.translatable("gui.umapyoi.self_training.need_positive"));
                }
            }
            case SelfTrainingContainer.PAGE_PERSONALITY -> {
                if (mx >= 8 && mx < 168 && my >= 25 && my < 47)
                    for (PersonalityTrait t : traits.negatives()) {
                        tip.add(t.displayName().copy().withColor(RED));
                        tip.add(t.description().copy().withStyle(ChatFormatting.GRAY));
                    }
                int row = this.rowAt(mx, my, POSITIVE_Y, POSITIVE_ROWS);
                List<PersonalityTrait> positives = PersonalityTrait.positives();
                if (row >= 0 && this.positiveScroll + row < positives.size()) {
                    PersonalityTrait t = positives.get(this.positiveScroll + row);
                    tip.add(t.displayName().copy().withColor(GREEN));
                    tip.add(t.description().copy().withStyle(ChatFormatting.GRAY));
                    if (traits.isExcluded(t))
                        traits.negatives().stream().filter(n -> n.excludes() == t).findFirst().ifPresent(n -> tip.add(
                                Component.translatable("gui.umapyoi.self_training.conflict", n.displayName())
                                        .withStyle(ChatFormatting.RED)));
                }
                if (my >= 148 && my < 158)
                    for (UmaTrait.Entry e : traits.umaEntries()) {
                        tip.add(e.displayName().copy().withStyle(e.trait().positive() ? ChatFormatting.GREEN : ChatFormatting.RED));
                        tip.add(e.description().copy().withStyle(ChatFormatting.GRAY));
                    }
            }
            case SelfTrainingContainer.PAGE_SKILL -> {
                int row = this.rowAt(mx, my, SKILL_Y, SKILL_ROWS);
                if (row >= 0 && this.skillScroll + row < SkillOffer.SKILLS.size()) {
                    ResourceLocation id = SkillOffer.SKILLS.get(this.skillScroll + row);
                    if (UmaSkillRegistry.REGISTRY.containsKey(id)) {
                        UmaSkill skill = UmaSkillRegistry.REGISTRY.get(id);
                        tip.add(skill.getDescription().copy().withStyle(ChatFormatting.GOLD));
                        tip.add(skill.getDescriptionDetail().copy().withStyle(ChatFormatting.GRAY));
                    }
                }
            }
            default -> {
            }
        }
        return tip;
    }

    private int rowAt(int mx, int my, int listY, int rows) {
        if (mx < LIST_X + 1 || mx >= LIST_X + LIST_W - 1 || my < listY + 1 || my >= listY + 1 + rows * ROW_H)
            return -1;
        return (my - listY - 1) / ROW_H;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && this.hasSoul()) {
            int mx = (int) mouseX - this.leftPos, my = (int) mouseY - this.topPos;
            if (this.menu.page == SelfTrainingContainer.PAGE_PERSONALITY) {
                int row = this.rowAt(mx, my, POSITIVE_Y, POSITIVE_ROWS);
                List<PersonalityTrait> positives = PersonalityTrait.positives();
                if (row >= 0 && this.positiveScroll + row < positives.size()) {
                    int index = this.positiveScroll + row;
                    if (!UmaTraits.of(this.soul()).isExcluded(positives.get(index)))
                        this.send(SelfTrainingContainer.BUTTON_POSITIVE + index);
                    return true;
                }
            } else if (this.menu.page == SelfTrainingContainer.PAGE_SKILL) {
                int row = this.rowAt(mx, my, SKILL_Y, SKILL_ROWS);
                if (row >= 0 && this.skillScroll + row < SkillOffer.SKILLS.size()) {
                    int index = this.skillScroll + row;
                    if (SkillOffer.isAvailable(this.soul(), SkillOffer.SKILLS.get(index)))
                        this.send(SelfTrainingContainer.BUTTON_SKILL + index);
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int step = scrollY > 0 ? -1 : scrollY < 0 ? 1 : 0;
        if (step != 0 && this.menu.page == SelfTrainingContainer.PAGE_PERSONALITY) {
            this.positiveScroll = Math.max(0, Math.min(PersonalityTrait.positives().size() - POSITIVE_ROWS,
                    this.positiveScroll + step));
            return true;
        }
        if (step != 0 && this.menu.page == SelfTrainingContainer.PAGE_SKILL) {
            this.skillScroll = Math.max(0, Math.min(Math.max(0, SkillOffer.SKILLS.size() - SKILL_ROWS),
                    this.skillScroll + step));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }
}
