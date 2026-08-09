package com.github.themrpuffin.villagerenhanced.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.github.themrpuffin.villagerenhanced.dialogue.DialogueOption;
import com.github.themrpuffin.villagerenhanced.dialogue.DialogueOptionEntry;
import com.github.themrpuffin.villagerenhanced.network.ChooseDialogueOptionPayload;
import com.github.themrpuffin.villagerenhanced.network.CloseDialoguePayload;
import com.github.themrpuffin.villagerenhanced.network.OpenDialoguePayload;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.TextAlignment;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

import org.jspecify.annotations.Nullable;
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
    /** Vertical gap between buttons; spacing is height plus this. */
    private static final int BUTTON_GAP = 4;
    private static final int BUTTON_SPACING = BUTTON_HEIGHT + BUTTON_GAP;

    /** Floors for the shrink-to-fit path below. Smaller than this and the labels stop being legible. */
    private static final int MIN_BUTTON_HEIGHT = 14;
    private static final int MIN_BUTTON_SPACING = MIN_BUTTON_HEIGHT + 2;

    private static final int TITLE_Y = 30;
    private static final int SUBTITLE_Y = 44;
    private static final int SEPARATOR_Y = 58;
    private static final int PANEL_TOP = 68;

    /** Vanilla's standard text line height. */
    private static final int LINE_HEIGHT = 9;
    /** Inset of the speech panel from the screen edges. */
    private static final int PANEL_MARGIN_X = 30;
    /** Inset of the speech from the panel edges. */
    private static final int PANEL_PADDING = 8;
    /** Gap between the bottom of the speech panel and the first button. */
    private static final int PANEL_TO_BUTTONS = 14;
    /** How close the buttons may be pulled to the panel when vertical space is short. */
    private static final int MIN_PANEL_TO_BUTTONS = 4;
    /** Space left below the last button. */
    private static final int BOTTOM_MARGIN = 8;

    /** Ticks between lines appearing; twenty ticks is one second. */
    private static final int REVEAL_TICKS_PER_LINE = 5;
    /** Quiet enough to read as punctuation rather than an interruption. */
    private static final float VOICE_VOLUME = 0.2F;

    /**
     * Shortest the speech panel is allowed to be, in lines. Keeps the panel and the buttons from
     * jumping around as the player moves between a one-line greeting and a four-line rumour list.
     */
    private static final int MIN_BODY_LINES = 2;

    /**
     * Packed ARGB.
     *
     * <p>The hierarchy is the point: the villager's <b>speech is the brightest content</b> on the
     * screen, with their profession and level deliberately dimmer. Previously both were the same
     * grey, so the words the villager was actually saying read as metadata and disappeared into
     * the header.
     */
    private static final int NAME_COLOUR = -1;
    private static final int SPEECH_COLOUR = 0xFFEDEDED;
    private static final int META_COLOUR = 0xFF9A9A9A;
    /** Translucent black, so the world stays faintly visible behind the speech. */
    private static final int PANEL_COLOUR = 0x66000000;
    private static final int SEPARATOR_COLOUR = 0x33FFFFFF;

    /** The server's description of the current page. Replaced wholesale by {@link #update}. */
    private OpenDialoguePayload dialogue;

    private Component subtitle;

    /** Built in {@link #init()}, because wrapping needs to know the screen width. */
    private MultiLineLabel body = MultiLineLabel.EMPTY;

    /** Bottom edge of the speech panel, computed in {@link #init()} from the wrapped line count. */
    private int panelBottom = PANEL_TOP;

    /**
     * How many of the server's lines are currently shown.
     *
     * <p>The reveal is <b>per line, not per character</b>. Character-by-character would mean
     * truncating styled {@code Component}s, which loses their styling — and that would undo the
     * speech colouring and the item colours in the belongings list. It also suits the content:
     * most pages are a single sentence, where a character crawl is just a delay, while the
     * rumour list is several separate statements that land nicely one at a time.
     */
    private int revealedLines;

    /** Ticks remaining before the next line appears. */
    private int revealDelay;

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
        // A new page is a new thing being said, so it is spoken again from the start.
        this.revealedLines = 0;
        this.revealDelay = 0;
        this.rebuildWidgets();

        // rebuildWidgets does not re-narrate, so without this the narrator would announce the
        // greeting and then stay silent through every page the player navigated to.
        this.triggerImmediateNarration(true);
    }

    /** Is this screen showing a conversation with the given villager? */
    public boolean isFor(int villagerId) {
        return this.dialogue.villagerId() == villagerId;
    }

    /** Width available to wrapped speech: the panel, less its padding on both sides. */
    private int bodyWidth() {
        return this.width - PANEL_MARGIN_X * 2 - PANEL_PADDING * 2;
    }

    /**
     * The server's lines, coloured as speech.
     *
     * <p>Wrapped text takes its colour from the Component's own style — neither {@code
     * visitLines} nor the collector's {@code Parameters} carries one — so it is applied here.
     * Setting it on the root style leaves any child that specifies its own colour untouched,
     * which is what keeps item colours intact in the belongings list.
     */
    private Component[] styledBodyLines() {
        return this.dialogue.bodyLines().stream()
                .map(line -> (Component) line.copy().withColor(SPEECH_COLOUR))
                .toArray(Component[]::new);
    }

    /** Rebuilds the drawn label from however many lines have been revealed so far. */
    private void rebuildVisibleBody() {
        Component[] all = styledBodyLines();
        Component[] shown = Arrays.copyOf(all, Math.min(this.revealedLines, all.length));
        // create() returns EMPTY for an empty array, so the opening frame draws nothing.
        this.body = MultiLineLabel.create(this.font, bodyWidth(), shown);
    }

    /**
     * Advances the reveal, one line at a time.
     *
     * <p>The pitch shifts slightly per line so a multi-line answer does not sound like the same
     * grunt repeated. Derived from the line index rather than a random source, so a given page
     * always sounds the same.
     */
    @Override
    public void tick() {
        super.tick();

        if (this.revealedLines >= this.dialogue.bodyLines().size()) {
            return;
        }
        if (--this.revealDelay > 0) {
            return;
        }

        this.revealDelay = REVEAL_TICKS_PER_LINE;
        this.revealedLines++;
        this.rebuildVisibleBody();

        float pitch = 0.9F + (this.revealedLines % 3) * 0.1F;
        Minecraft.getInstance().getSoundManager().play(
                SimpleSoundInstance.forUI(SoundEvents.VILLAGER_AMBIENT, pitch, VOICE_VOLUME));
    }

    /**
     * What the narrator reads for this screen.
     *
     * <p>The default is the title alone, which here would be the villager's name and nothing
     * else — leaving a screen-reader user with no idea what was said. This adds the profession
     * line, the speech, and the reason for each unavailable option.
     *
     * <p>Two deliberate choices:
     *
     * <p><b>The full speech, not the typewriter's current progress.</b> Someone relying on the
     * narrator should not have to wait for an animation to catch up with itself.
     *
     * <p><b>Unavailable options are described here</b>, because they cannot be reached any other
     * way. Focus navigation skips inactive widgets, so a keyboard user can never land on a
     * greyed button to hear its tooltip. Greying out is meant to advertise what is possible and
     * what it would take; without this, that only worked for players who can see it.
     */
    @Override
    public Component getNarrationMessage() {
        List<Component> parts = new ArrayList<>();
        parts.add(this.title);
        parts.add(this.subtitle);
        parts.addAll(this.dialogue.bodyLines());

        for (DialogueOptionEntry entry : this.dialogue.options()) {
            if (entry.enabled()) {
                continue;
            }
            Component reason = disabledReason(entry.option());
            if (reason != null) {
                parts.add(Component.translatable(
                        "villagerenhanced.narration.unavailable", labelFor(entry.option()), reason));
            }
        }

        return CommonComponents.joinForNarration(parts.toArray(Component[]::new));
    }

    /** Shows the rest immediately, for anyone who reads faster than the villager talks. */
    private void revealAll() {
        int total = this.dialogue.bodyLines().size();
        if (this.revealedLines < total) {
            this.revealedLines = total;
            this.rebuildVisibleBody();
        }
    }

    // Input still does its normal job -- skipping the reveal is a side effect, so a click that
    // lands on a button both finishes the text and presses the button.
    @Override
    public boolean keyPressed(KeyEvent event) {
        this.revealAll();
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        this.revealAll();
        return super.mouseClicked(event, doubleClick);
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

        // The panel is sized from the COMPLETE text, not from what is currently revealed, so it
        // does not grow line by line and shove the buttons down while the villager is talking.
        int lines = Math.max(
                MultiLineLabel.create(this.font, bodyWidth(), styledBodyLines()).getLineCount(),
                MIN_BODY_LINES);
        this.panelBottom = PANEL_TOP + PANEL_PADDING * 2 + lines * LINE_HEIGHT;

        this.rebuildVisibleBody();

        int x = (this.width - BUTTON_WIDTH) / 2;
        int count = this.dialogue.options().size();
        int bottomLimit = this.height - BOTTOM_MARGIN;

        int spacing = BUTTON_SPACING;
        int buttonHeight = BUTTON_HEIGHT;
        int y = this.panelBottom + PANEL_TO_BUTTONS;

        // The greeting can offer eight options, and at full size that block is taller than a
        // short screen allows -- 1080p at GUI scale 4 leaves roughly 270 usable pixels. Rather
        // than let the last button clip off the bottom, which would leave Escape as the only way
        // out, close the gap to the panel and shrink the buttons until the whole block fits.
        if (y + count * spacing > bottomLimit) {
            y = this.panelBottom + MIN_PANEL_TO_BUTTONS;
            int available = bottomLimit - y;
            spacing = Math.max(MIN_BUTTON_SPACING, available / count);
            buttonHeight = Math.max(MIN_BUTTON_HEIGHT, spacing - BUTTON_GAP);
        }

        // One button per option the server sent, in its order. Adding an option server-side
        // makes it appear here with no client change beyond a label.
        for (DialogueOptionEntry entry : this.dialogue.options()) {
            this.addRenderableWidget(this.buildOptionButton(entry, x, y, buttonHeight));
            y += spacing;
        }
    }

    private Button buildOptionButton(DialogueOptionEntry entry, int x, int y, int buttonHeight) {
        Component reason = entry.enabled() ? null : disabledReason(entry.option());
        Button button = Button.builder(labelFor(entry.option()), b -> this.choose(entry))
                .bounds(x, y, BUTTON_WIDTH, buttonHeight)
                // Tooltip only when unusable, so hovering explains the refusal.
                .tooltip(reason == null ? null : Tooltip.create(reason))
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
            case RUMOURS -> Component.translatable("villagerenhanced.dialogue.option.rumours");
            case ASK_NAME -> Component.translatable("villagerenhanced.dialogue.option.ask_name");
            case TOPICS -> Component.translatable("villagerenhanced.dialogue.option.topics");
            case ASK_ABOUT_WORK -> Component.translatable("villagerenhanced.dialogue.option.ask_about_work");
            case SHOW_BELONGINGS -> Component.translatable("villagerenhanced.dialogue.option.show_belongings");
            case BACK -> Component.translatable("villagerenhanced.dialogue.option.back");
            case LEAVE -> Component.translatable("villagerenhanced.dialogue.option.leave");
        };
    }

    /** Why an option is unavailable, or null for options that are never refused. */
    private static @Nullable Component disabledReason(DialogueOption option) {
        return switch (option) {
            case TRADE -> Component.translatable("villagerenhanced.dialogue.no_trades");
            case GIFT -> Component.translatable("villagerenhanced.dialogue.no_gift");
            case ASK_NAME -> Component.translatable("villagerenhanced.dialogue.no_name");
            case RUMOURS -> Component.translatable("villagerenhanced.dialogue.no_rumours");
            case SHOW_BELONGINGS -> Component.translatable("villagerenhanced.dialogue.no_belongings");
            case VIEW_REPUTATION, ASK_ABOUT_WORK, TOPICS, BACK, LEAVE -> null;
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

        int left = PANEL_MARGIN_X;
        int right = this.width - PANEL_MARGIN_X;

        // Header: who this is, then what they do, the latter dimmer so it recedes behind the
        // name and, more importantly, behind the speech below.
        // In 26.2 these are text() and centeredText(); drawString() and drawCenteredString()
        // no longer exist.
        graphics.centeredText(this.font, this.title, this.width / 2, TITLE_Y, NAME_COLOUR);
        graphics.centeredText(this.font, this.subtitle, this.width / 2, SUBTITLE_Y, META_COLOUR);

        // A rule under the header, so "who they are" and "what they are saying" are visibly
        // separate regions rather than one stack of centred lines.
        graphics.fill(left, SEPARATOR_Y, right, SEPARATOR_Y + 1, SEPARATOR_COLOUR);

        // The speech panel, drawn before the speech so the text sits on top of it.
        graphics.fill(left, PANEL_TOP, right, this.panelBottom, PANEL_COLOUR);

        // Left-aligned inside the panel. Centred text reads as a heading; prose does not.
        // Wrapped text goes through the text collector rather than a direct draw call.
        ActiveTextCollector textRenderer = graphics.textRenderer();
        this.body.visitLines(
                TextAlignment.LEFT,
                left + PANEL_PADDING,
                PANEL_TOP + PANEL_PADDING,
                LINE_HEIGHT,
                textRenderer);
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
