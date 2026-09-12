package gg.vape.module.render.hud;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import gg.vape.Vape;
import gg.vape.event.impl.EventScoreboardScores;
import gg.vape.module.render.hud.HudModule;
import gg.vape.module.render.hud.HudModuleGroup;
import gg.vape.module.render.hud.ScoreboardVisibleScorePredicate;
import gg.vape.ui.click.frame.impl.hud.ScoreboardHudFrame;
import gg.vape.utils.TimerUtil;
import gg.vape.utils.Vec3d;
import gg.vape.utils.render.GuiRenderPrimitives;
import gg.vape.value.BooleanValue;
import gg.vape.wrapper.impl.FontRenderer;
import gg.vape.wrapper.impl.ForgeVersion;
import gg.vape.wrapper.impl.GlStateManager;
import gg.vape.wrapper.impl.MatrixStack;
import gg.vape.wrapper.impl.Minecraft;
import gg.vape.wrapper.impl.Score;
import gg.vape.wrapper.impl.ScoreObjective;
import gg.vape.wrapper.impl.ScorePlayerTeam;
import gg.vape.wrapper.impl.Scoreboard;
import gg.vape.wrapper.impl.TextComponent;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import org.lwjgl.opengl.GL11;

public class ScoreboardHudModule
extends HudModule {
    private final TimerUtil objectiveTimer = new TimerUtil();
    public final BooleanValue showScoreNumbers = BooleanValue.create(this, "Show score numbers", false);
    private ScoreObjective objective;

    @Override
    public void onEnable() {
        EventScoreboardScores.setLocked(true);
    }

    public void updateObjective(ScoreObjective objective) {
        this.objective = objective;
        this.objectiveTimer.reset();
    }

    @Override
    public void onDisable() {
        EventScoreboardScores.setLocked(false);
    }

    public ScoreboardHudModule() {
        super("Scoreboard", HudModuleGroup.HUD, "scoreboard", ScoreboardHudFrame.class);
        this.addValue(this.showScoreNumbers);
        this.setSuffix("Allows you to edit the Minecraft scoreboard");
    }

    private Map<String, String> getTextReplacements() {
        ScoreboardTextReplacementModule module = Vape.INSTANCE.getModManager().getMod(ScoreboardTextReplacementModule.class);
        if (module == null) {
            return java.util.Collections.emptyMap();
        }
        return module.textReplacements.getValue();
    }

    private String stripFormattingCodes(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        StringBuilder visibleTextBuilder = new StringBuilder();
        for (int index = 0; index < input.length(); ++index) {
            char character = input.charAt(index);
            if (character == '&' || character == '\u00a7') {
                ++index;
                continue;
            }
            visibleTextBuilder.append(character);
        }
        return visibleTextBuilder.toString().toLowerCase();
    }

    private int getRawIndexForVisibleIndex(String input, int visibleIndex) {
        int visibleCount = 0;
        for (int rawIndex = 0; rawIndex < input.length(); ++rawIndex) {
            char character = input.charAt(rawIndex);
            if (character == '&' || character == '\u00a7') {
                if (rawIndex + 1 < input.length()) {
                    ++rawIndex;
                }
                continue;
            }
            if (visibleCount == visibleIndex) {
                return rawIndex;
            }
            ++visibleCount;
        }
        return input.length();
    }

    private String replaceScoreText(String searchText, String formattedText, String replacement) {
        if (searchText == null || searchText.isEmpty() || formattedText == null || formattedText.isEmpty()) {
            return formattedText;
        }
        String normalizedReplacement = replacement == null ? "" : replacement.replace('&', '\u00a7');
        String visibleSearchText = this.stripFormattingCodes(searchText);
        String visibleFormattedText = this.stripFormattingCodes(formattedText);
        int visibleStart = visibleFormattedText.indexOf(visibleSearchText.toLowerCase());
        if (visibleStart < 0) {
            return formattedText;
        }
        int rawStart = this.getRawIndexForVisibleIndex(formattedText, visibleStart);
        int rawEnd = this.getRawIndexForVisibleIndex(formattedText, visibleStart + visibleSearchText.length());
        StringBuilder replacedText = new StringBuilder();
        replacedText.append(formattedText, 0, rawStart);
        replacedText.append(normalizedReplacement);
        replacedText.append(formattedText.substring(rawEnd));
        return replacedText.toString();
    }

    public Vec3d renderScoreboard(double x, double y, boolean drawBackground) {
        boolean gamePaused = false;
        if (Minecraft.i() != null) {
            gamePaused = Minecraft.V();
        }
        if (this.objective == null) {
            return new Vec3d(0.0, 0.0, 0.0);
        }
        if (gamePaused || this.objectiveTimer.hasTimeElapsed(10000L)) {
            this.objective = null;
            return new Vec3d(0.0, 0.0, 0.0);
        }
        boolean blendWasEnabled = GL11.glIsEnabled(3042);
        if (blendWasEnabled) {
            GlStateManager.disableBlend();
        }
        boolean includeScoreNumbers = this.showScoreNumbers.getEffectiveValue();
        FontRenderer fontRenderer = Minecraft.getFontRenderer();
        Scoreboard scoreboard = this.objective.getScoreboard();
        EventScoreboardScores.setLocked(false);
        Collection<Score> scores = scoreboard.getPlayerScores(this.objective);
        EventScoreboardScores.setLocked(true);
        ArrayList<Score> visibleScores = Lists.newArrayList(
                Iterables.filter(scores, new ScoreboardVisibleScorePredicate()));
        scores = visibleScores.size() > 15
                ? Lists.newArrayList(Iterables.skip(visibleScores, scores.size() - 15))
                : visibleScores;
        int scoreboardWidth = fontRenderer.getStringWidth(this.objective.getDisplayNameText());
        for (Score score : scores) {
            ScorePlayerTeam team = scoreboard.getPlayersTeam(score.getOwner());
            String scoreLine = ScorePlayerTeam.formatPlayerName(team, score.getOwner()) + ":";
            if (includeScoreNumbers) {
                scoreLine += " \u00a7c" + score.getScore();
            }
            scoreboardWidth = Math.max(scoreboardWidth, fontRenderer.getStringWidth(scoreLine));
        }
        int contentHeight = scores.size() * fontRenderer.getFontHeight();
        int bottom = (int)(y + contentHeight) + 8;
        int left = (int)x + 1;
        int rowIndex = 0;
        double renderedHeight = 0.0;
        Map<String, String> replacements = this.getTextReplacements();
        for (Score score : scores) {
            ++rowIndex;
            ScorePlayerTeam team = scoreboard.getPlayersTeam(score.getOwner());
            String playerName = ScorePlayerTeam.formatPlayerName(team, score.getOwner());
            for (Map.Entry<String, String> replacement : replacements.entrySet()) {
                playerName = this.replaceScoreText(
                        replacement.getKey(), playerName, replacement.getValue());
            }
            String scoreText = "\u00a7c" + score.getScore();
            int rowY = bottom - rowIndex * fontRenderer.getFontHeight();
            if (drawBackground) {
                float backgroundX = left - 2;
                float backgroundY = rowY;
                float backgroundWidth = (float)(left + scoreboardWidth) - backgroundX;
                float backgroundHeight = fontRenderer.getFontHeight();
                GuiRenderPrimitives.y(backgroundX, backgroundY, backgroundWidth,
                        backgroundHeight, new Color(0x50000000, true));
            }
            renderedHeight += fontRenderer.getFontHeight();
            if (ForgeVersion.MC_1_16_5.d()) {
                fontRenderer.J(MatrixStack.A(), new TextComponent(team, score.getOwner()), left, rowY, -1);
            } else {
                fontRenderer.drawString(playerName, left, rowY, 0x20FFFFFF);
            }
            if (includeScoreNumbers) {
                fontRenderer.drawString(scoreText,
                        left + scoreboardWidth - fontRenderer.getStringWidth(scoreText), rowY, 3648127);
            }
            GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
            if (rowIndex == scores.size()) {
                String title = this.objective.getDisplayNameText();
                for (Map.Entry<String, String> replacement : replacements.entrySet()) {
                    title = this.replaceScoreText(
                            replacement.getKey(), title, replacement.getValue());
                }
                if (drawBackground) {
                    GuiRenderPrimitives.C(left - 2, rowY - fontRenderer.getFontHeight() - 1,
                            scoreboardWidth + 2.0, fontRenderer.getFontHeight(),
                            new Color(0x60000000, true));
                    GuiRenderPrimitives.C(left - 2, rowY - 1, scoreboardWidth + 2.0, 1.0,
                            new Color(0x50000000, true));
                }
                if (ForgeVersion.MC_1_16_5.d()) {
                    fontRenderer.J(MatrixStack.A(), this.objective.getDisplayNameComponent(),
                            left + scoreboardWidth / 2 - fontRenderer.getStringWidth(title) / 2,
                            rowY - fontRenderer.getFontHeight(), -1);
                } else {
                    fontRenderer.drawString(title,
                            left + scoreboardWidth / 2 - fontRenderer.getStringWidth(title) / 2,
                            rowY - fontRenderer.getFontHeight(), 0x20FFFFFF);
                }
            }
            GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
        }
        if (blendWasEnabled) {
            GlStateManager.enableBlend();
        }
        return new Vec3d(scoreboardWidth, renderedHeight + 5.0, 0.0);
    }
}
