package com.github.themrpuffin.villagerenhanced.client;

import com.github.themrpuffin.villagerenhanced.dialogue.DialogueOption;
import com.github.themrpuffin.villagerenhanced.dialogue.DialogueOptionEntry;
import com.github.themrpuffin.villagerenhanced.network.ChooseDialogueOptionPayload;
import com.github.themrpuffin.villagerenhanced.network.CloseDialoguePayload;
import com.github.themrpuffin.villagerenhanced.network.OpenDialoguePayload;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * The dialogue window: a greeting plus the options the server offered.
 *
 * <p>Everything drawn here arrives in the {@link OpenDialoguePayload}. The screen makes no
 * decisions about what a villager can do.
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
    private static final int GREETING_Y = 75;

    /** Packed ARGB. -1 is opaque white; 0xFFAAAAAA is vanilla's secondary-text grey. */
    private static final int WHITE = -1;
    private static final int GREY = 0xFFAAAAAA;

    private final OpenDialoguePayload dialogue;
    private final Component subtitle;
    private final Component greeting;

    /** Set when the server closed this screen, so {@link #removed()} does not answer back. */
    private boolean suppressCloseMessage;

    /**
     * @param dialogue   the server's description of this conversation
     * @param playerName the player's name, so the greeting can address them
     */
    public VillagerDialogueScreen(OpenDialoguePayload dialogue, Component playerName) {
        super(dialogue.villagerName());
        this.dialogue = dialogue;

        // Values are passed as arguments so translators can reorder them; languages do not all
        // arrange sentences the way English does.
        this.subtitle = Component.translatable(
                "villagerenhanced.dialogue.subtitle",
                dialogue.professionName(),
                dialogue.villagerLevel());
        this.greeting = Component.translatable("villagerenhanced.dialogue.greeting", playerName);
    }

    /**
     * Called when the screen opens and again on resize, so it must be safe to run repeatedly —
     * which it is, because the parent clears the widget list first.
     */
    @Override
    protected void init() {
        super.init();

        int x = (this.width - BUTTON_WIDTH) / 2;
        int y = this.height / 2;

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
            case LEAVE -> Component.translatable("villagerenhanced.dialogue.option.leave");
        };
    }

    private static Tooltip disabledReasonFor(DialogueOption option) {
        return switch (option) {
            case TRADE -> Tooltip.create(Component.translatable("villagerenhanced.dialogue.no_trades"));
            case LEAVE -> null;
        };
    }

    /**
     * Tells the server which option was clicked.
     *
     * <p>Note it does not close the screen for TRADE. The server decides whether trading is
     * allowed, and opening the merchant screen replaces this one automatically; closing
     * optimistically would leave the player staring at nothing if the server refused.
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
        graphics.centeredText(this.font, this.greeting, this.width / 2, GREETING_Y, GREY);
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
