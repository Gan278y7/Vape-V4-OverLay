package gg.vape.module.render.hud;

import gg.vape.value.StringMapValue;

public class ScoreboardTextReplacementModule
extends HudModule {
    public final StringMapValue textReplacements = (StringMapValue)StringMapValue.create(this, "Replace scoreboard text", "Find text", "Replace with").setBase64Encoded(true);

    public ScoreboardTextReplacementModule() {
        super("Scoreboard text replacement", HudModuleGroup.HUD, "user");
        this.addValue(this.textReplacements);
        this.setSuffix("Replaces text shown on the scoreboard without enabling the scoreboard HUD");
    }
}
