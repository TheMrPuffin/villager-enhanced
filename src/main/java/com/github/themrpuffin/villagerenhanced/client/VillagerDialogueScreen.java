package com.github.themrpuffin.villagerenhanced.client;

import com.github.themrpuffin.villagerenhanced.dialogue.DialogueOption;
import com.github.themrpuffin.villagerenhanced.dialogue.DialogueOptionEntry;
import com.github.themrpuffin.villagerenhanced.network.ChooseDialogueOptionPayload;
import com.github.themrpuffin.villagerenhanced.network.CloseDialoguePayload;
import com.github.themrpuffin.villagerenhanced.network.OpenDialoguePayload;

import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.TextAlignment;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * The dialogue window: what the villager is saying, plus the options the server offered.
 *
 * <p>Everything drawn here arrives in an {@link OpenDialoguePayload}, including the speech
 * itself. The screen makes no decisions about what a villager can say or do.
 *
 * <p><b>Rendering in 26.2 differs from older Minecraft versions.</b> Screens no longer have a
 * {@code render(GuiGraphics, ...)} method that draws immediately. The game runs two phases:
 * every screen first <i>describes</i> what it wants drawn via {@code extractRenderState}, then
 * that description goes to the GPU in one batch — which is what makes the Vulkan-capable
 * renderer possible. In practice: put drawing in {@code extractRenderState}, and never retain
 * the graphics object.
 */
public class VillagerDialogueScreen extends Screen {

    private static final int BUTTON_WIDTH = 200;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_SPACING = BUTTON_HEIGHT + 4;

    private static final int TITLE_Y = 40;
    private static final int SUBTITLE_Y = 55;
    private static final int BODY_Y = 78;
    /** Vanilla's standard text line height. */
    private static final int LINE_HEIGHT = 9;
    /** Left and right margin for wrapped body text. */
    private static final int BODY_MARGIN = 40;

    /** Packed ARGB. -1 is opaque white; 0xFFAAAAAA is vanilla's secondary-text grey. */
    private static final int WHITE = -1;
    private static final int GREY = 0xFFAAAAAA;

    /** The server's description of the current page. Replaced wholesale by {@link #update}. */
    private OpenDialoguePayload dialogue;

    private Component subtitle;

    /** Built in {@link #init()}, because wrapping needs to know the screen width. */
    private MultiLineLabel body = MultiLineLabel.EMPTY;

    /** Set when the server closed this screen, so {@link #removed()} does not answer back. */
    private boolean suppressCloseMessage;

    public VillagerDialogueScreen(OpenDialoguePayload dialogue) {
        super(dialogue.villagerName());
        this.dialogue = dialogue;
        this.subtitle = buildSubtitle(dialogue);
    }

    /**
     * Swaps in a new page of the same conversation.
     *
     * <p>Deliberately <b>not</b> done by opening a replacement screen. {@code Gui#setScreen}
     * calls {@code removed()} on the outgoing screen, which would send a
     * {@code CloseDialoguePayload} and end the very session the new page belongs to. Updating
     * in place and rebuilding the widgets avoids that entirely.
     */
    public void update(OpenDialoguePayload payload) {
        this.dialogue = payload;
        this.subtitle = buildSubtitle(payload);
        this.rebuildWidgets();
    }

    /** Is this screen showing a conversation with the given villager? */
    public boolean isFor(int villagerId) {
        return this.dialogue.villagerId() == villagerId;
    }

    private static Component buildSubtitle(OpenDialoguePayload payload) {
        // Values are passed as arguments so translators can reorder them; languages do not all
        // arrange sentences the way English does.
        return Component.translatable(
                "villagerenhanced.dialogue.subtitle",
                payload.professionName(),
                payload.villagerLevel());
    }

    /**
     * Called when the screen opens, on resize, and on every {@link #update}, so it must be safe
     * to run repeatedly — which it is, because the parent clears the widget list first.
     */
    @Override
    protected void init() {
        super.init();

        this.body = MultiLineLabel.create(this.font, this.dialogue.body(), this.width - BODY_MARGIN * 2);

        int x = (this.width - BUTTON_WIDTH) / 2;
        // Keep the buttons below the speech however many lines it wrapped to, but never higher
        // than the middle of the screen, so short pages still look centred.
        int y = Math.max(this.height / 2, BODY_Y + this.body.getLineCount() * LINE_HEIGHT + 12);

        // One button per option the server sent, in its order. Adding an option server-side
        // makes it appear here with no client change beyond a label.
        for (DialogueOptionEntry entry : this.dialogue.options()) {
            this.addRenderableWidget(this.buildOptionButton(entry, x, y));
            y += BUTTON_SPACING;
        }
    }

    private Button buildOptionButton(DialogueOptionEntry entry, int x, int y) {
        Button button = Button.builder(labelFor(entry.option()), b -> this.choose(entry))
                .bounds(x, y, BUTTON_WIDTH, BUTTON_HEIGHT)
                // Tooltip only when unusable, so hovering explains the refusal.
                .tooltip(entry.enabled() ? null : disabledReasonFor(entry.option()))
                .build();

        // Greys the label and stops clicks reaching the handler.
        button.active = entry.enabled();
        return button;
    }

    private static Component labelFor(DialogueOption option) {
        return switch (option) {
            case TRADE -> Component.translatable("villagerenhanced.dialogue.option.trade");
            case GIFT -> Component.translatable("villagerenhanced.dialogue.option.gift");
            case VIEW_REPUTATION -> Component.translatable("villagerenhanced.dialogue.option.view_reputation");
            case BACK -> Component.translatable("villagerenhanced.dialogue.option.back");
            case LEAVE -> Component.translatable("villagerenhanced.dialogue.option.leave");
        };
    }

    private static Tooltip disabledReasonFor(DialogueOption option) {
        return switch (option) {
            case TRADE -> Tooltip.create(Component.translatable("villagerenhanced.dialogue.no_trades"));
            case GIFT -> Tooltip.create(Component.translatable("villagerenhanced.dialogue.no_gift"));
            case VIEW_REPUTATION, BACK, LEAVE -> null;
        };
    }

    /**
     * Tells the server which option was clicked.
     *
     * <p>Only LEAVE closes the screen here. Everything else is the server's decision: TRADE is
     * replaced by the merchant screen if allowed, and the navigation options come back as an
     * {@link #update}. Closing optimistically would leave the player staring at nothing if the
     * server refused.
     */
    private void choose(DialogueOptionEntry entry) {
        ClientPacketDistributor.sendToServer(
                new ChooseDialogueOptionPayload(this.dialogue.villagerId(), entry.option()));

        if (entry.option() == DialogueOption.LEAVE) {
            this.onClose();
        }
    }

    /**
     * Tells the server the window has gone, however it went.
     *
     * <p>{@code removed} covers Leave, Escape and replacement alike — a dialogue marks its
     * villager busy, so every exit must report back or the villager stays locked for everyone.
     *
     * <p>Sending this after a trade handoff is harmless: the server drops the session before
     * opening the merchant screen, so it arrives to find nothing.
     */
    @Override
    public void removed() {
        super.removed();
        if (!this.suppressCloseMessage) {
            ClientPacketDistributor.sendToServer(CloseDialoguePayload.INSTANCE);
        }
    }

    /** Closes without reporting back, because the server is the one closing it. */
    public void closeFromServer() {
        this.suppressCloseMessage = true;
        this.onClose();
    }

    /**
     * Calling {@code super} first matters: the parent lets every widget added with
     * {@code addRenderableWidget} describe itself. Skip it and the buttons vanish.
     */
    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractRenderState(graphics, mouseX, mouseY, a);

        // In 26.2 these are text() and centeredText(); the older drawString() and
        // drawCenteredString() no longer exist.
        graphics.centeredText(this.font, this.title, this.width / 2, TITLE_Y, WHITE);
        graphics.centeredText(this.font, this.subtitle, this.width / 2, SUBTITLE_Y, GREY);

        // Wrapped text goes through the text collector rather than a direct draw call.
        ActiveTextCollector textRenderer = graphics.textRenderer();
        this.body.visitLines(TextAlignment.CENTER, this.width / 2, BODY_Y, LINE_HEIGHT, textRenderer);
    }

    /**
     * Screens pause single-player by default. A dialogue must not: the villager needs to keep
     * ticking, and the server keeps a session alive underneath.
     */
    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /**
     * Selects the in-world container backdrop — the translucent gradient used by the inventory
     * and merchant screens — rather than the opaque, blurred pause-menu background that
     * {@code Screen} uses by default. {@code AbstractContainerScreen} does the same, which is
     * what keeps this visually consistent with the trade screen it hands off to.
     */
    @Override
    public boolean isInGameUi() {
        return true;
    }
}
