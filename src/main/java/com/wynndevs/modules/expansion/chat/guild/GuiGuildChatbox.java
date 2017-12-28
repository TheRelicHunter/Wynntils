package com.wynndevs.modules.expansion.chat.guild;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.util.ITabCompleter;
import net.minecraft.util.TabCompleter;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import javax.annotation.Nullable;
import java.io.IOException;

public class GuiGuildChatbox extends GuiScreen implements ITabCompleter {
    private static final Logger _LOGGER = LogManager.getLogger();
    private final String guildChatStr = "/g ";
    /**
     * Chat entry field
     */
    protected GuiTextField inputField;
    private String historyBuffer = "";
    /**
     * keeps position of which chat message you will select when you press up, (does not increase for duplicated
     * messages sent immediately after each other)
     */
    private int sentHistoryCursor = -1;
    private TabCompleter tabCompleter;
    /**
     * is the text that appears when you press the chat key and the input box appears pre-filled
     */
    private String defaultInputFieldText = "";
    private GuildChat chat;

    public GuiGuildChatbox(GuildChat chat) {
        this.chat = chat;
        this.mc = Minecraft.getMinecraft();
    }

    public GuiGuildChatbox(GuildChat chat, String defaultText) {
        this(chat);
        this.defaultInputFieldText = defaultText;
    }

    /**
     * Adds the buttons (and other controls) to the screen in question. Called when the GUI is displayed and when the
     * window resizes, the buttonList is cleared beforehand.
     */
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        this.sentHistoryCursor = this.chat.getChat().getSentMessages().size();
        this.inputField = new GuiTextField(0, this.fontRenderer, this.mc.fontRenderer.getStringWidth(this.guildChatStr) + 7, this.height - 12, this.width - 4, 12);
        this.inputField.setMaxStringLength(97);
        this.inputField.setEnableBackgroundDrawing(false);
        this.inputField.setFocused(true);
        this.inputField.setText(this.defaultInputFieldText);
        this.inputField.setCanLoseFocus(false);
        this.tabCompleter = new GuiChat.ChatTabCompleter(this.inputField);
    }

    /**
     * Called when the screen is unloaded. Used to disable keyboard repeat events
     */
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
        this.chat.getChat().resetScroll();
        this.mc.ingameGUI.getChatGUI().resetScroll();
    }

    /**
     * Called from the main game loop to update the screen.
     */
    public void updateScreen() {
        this.inputField.updateCursorCounter();
    }

    /**
     * Fired when a key is typed (except F11 which toggles full screen). This is the equivalent of
     * KeyListener.keyTyped(KeyEvent e). Args : character (character on the key), keyCode (lwjgl Keyboard key code)
     */
    protected void keyTyped(char typedChar, int keyCode) {
        this.tabCompleter.resetRequested();

        if (keyCode == 15) {
            this.tabCompleter.complete();
        } else {
            this.tabCompleter.resetDidComplete();
        }

        if (keyCode == 1) {
            this.mc.displayGuiScreen(null);
        } else if (keyCode != 28 && keyCode != 156) {
            if (keyCode == 200) {
                this.getSentHistory(-1);
            } else if (keyCode == 208) {
                this.getSentHistory(1);
            } else if (keyCode == 201) {
                this.mc.ingameGUI.getChatGUI().scroll(this.mc.ingameGUI.getChatGUI().getLineCount() - 1);
            } else if (keyCode == 209) {
                this.mc.ingameGUI.getChatGUI().scroll(-this.mc.ingameGUI.getChatGUI().getLineCount() + 1);
            } else {
                this.inputField.textboxKeyTyped(typedChar, keyCode);
            }
        } else {
            String s = this.inputField.getText().trim();


            if (!s.isEmpty()) {
                this.sendChatMessage("/g " + s, false);
                this.chat.getChat().addToSentMessages(s);
            }

            this.mc.displayGuiScreen(null);
        }
    }

    @Override
    public void sendChatMessage(String msg, boolean addToChat) {
        if (addToChat) {
            this.chat.getChat().addToSentMessages(msg);
        }
        if (net.minecraftforge.client.ClientCommandHandler.instance.executeCommand(mc.player, msg) != 0) return;

        this.mc.player.sendChatMessage(msg);
    }

    /**
     * Handles mouse input.
     */
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int i = Mouse.getEventDWheel();

        if (i != 0) {
            if (i > 1) {
                i = 1;
            }

            if (i < -1) {
                i = -1;
            }

            if (!isShiftKeyDown()) {
                i *= 7;
            }

            this.chat.getChat().scroll(i);
        }
    }

    /**
     * Called when the mouse is clicked. Args : mouseX, mouseY, clickedButton
     */
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (mouseButton == 0) {
            ITextComponent itextcomponent = this.chat.getChat().getChatComponent(Mouse.getX(), Mouse.getY());

            if (itextcomponent != null && this.handleComponentClick(itextcomponent)) {
                return;
            }
        }

        this.inputField.mouseClicked(mouseX, mouseY, mouseButton);
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    /**
     * Sets the text of the chat
     */
    protected void setText(String newChatText, boolean shouldOverwrite) {
        if (shouldOverwrite) {
            this.inputField.setText(newChatText);
        } else {
            this.inputField.writeText(newChatText);
        }
    }

    /**
     * input is relative and is applied directly to the sentHistoryCursor so -1 is the previous message, 1 is the next
     * message from the current cursor position
     */
    public void getSentHistory(int msgPos) {
        int i = this.sentHistoryCursor + msgPos;
        int j = this.chat.getChat().getSentMessages().size();
        i = MathHelper.clamp(i, 0, j);

        if (i != this.sentHistoryCursor) {
            if (i == j) {
                this.sentHistoryCursor = j;
                this.inputField.setText(this.historyBuffer);
            } else {
                if (this.sentHistoryCursor == j) {
                    this.historyBuffer = this.inputField.getText();
                }

                this.inputField.setText(this.chat.getChat().getSentMessages().get(i));
                this.sentHistoryCursor = i;
            }
        }
    }

    /**
     * Draws the screen and all the components in it.
     */
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawRect(mc.fontRenderer.getStringWidth(this.guildChatStr) + 5, this.height - 14, this.width - 2, this.height - 2, Integer.MIN_VALUE);
        drawRect(3, this.height - 14, mc.fontRenderer.getStringWidth(this.guildChatStr) + 4, height - 2, Integer.MIN_VALUE);
        this.drawString(mc.fontRenderer, this.guildChatStr, 5, this.height - 12, 0xAAAAAA);
        this.inputField.drawTextBox();
        ITextComponent itextcomponent = this.chat.getChat().getChatComponent(Mouse.getX(), Mouse.getY());

        if (itextcomponent != null && itextcomponent.getStyle().getHoverEvent() != null) {
            this.handleComponentHover(itextcomponent, mouseX, mouseY);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    /**
     * Returns true if this GUI should pause the game when it is displayed in single-player
     */
    public boolean doesGuiPauseGame() {
        return false;
    }

    /**
     * Sets the list of tab completions, as long as they were previously requested.
     */
    public void setCompletions(String... newCompletions) {
        this.tabCompleter.setCompletions(newCompletions);
    }

    @SideOnly(Side.CLIENT)
    public static class ChatTabCompleter extends TabCompleter {
        /**
         * The instance of the Minecraft client
         */
        private final Minecraft clientInstance = Minecraft.getMinecraft();

        public ChatTabCompleter(GuiTextField p_i46749_1_) {
            super(p_i46749_1_, false);
        }

        /**
         * Called when tab key pressed. If it's the first time we tried to complete this string, we ask the server
         * for completions. When the server responds, this method gets called again (via setCompletions).
         */
        public void complete() {
            super.complete();

            if (this.completions.size() > 1) {
                StringBuilder stringbuilder = new StringBuilder();

                for (String s : this.completions) {
                    if (stringbuilder.length() > 0) {
                        stringbuilder.append(", ");
                    }

                    stringbuilder.append(s);
                }

                this.clientInstance.ingameGUI.getChatGUI().printChatMessageWithOptionalDeletion(new TextComponentString(stringbuilder.toString()), 1);
            }
        }

        @Nullable
        public BlockPos getTargetBlockPos() {
            BlockPos blockpos = null;

            if (this.clientInstance.objectMouseOver != null && this.clientInstance.objectMouseOver.typeOfHit == RayTraceResult.Type.BLOCK) {
                blockpos = this.clientInstance.objectMouseOver.getBlockPos();
            }

            return blockpos;
        }
    }

}