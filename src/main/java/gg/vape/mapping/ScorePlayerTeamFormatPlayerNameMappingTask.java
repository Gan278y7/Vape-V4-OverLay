package gg.vape.mapping;

import gg.vape.Vape;
import gg.vape.mapping.MappedClasses;
import gg.vape.mapping.MappingMethod;
import gg.vape.module.render.TextReplaceV2;
import gg.vape.wrapper.impl.ForgeVersion;
import javassist.CannotCompileException;
import javassist.CtBehavior;

public class ScorePlayerTeamFormatPlayerNameMappingTask
extends JavassistMappingTask {
    public ScorePlayerTeamFormatPlayerNameMappingTask() {
        super(MappedClasses.u6);
    }

    @Override
    public void transform() {
        if (Vape.INSTANCE.getMappings() == null || Vape.INSTANCE.getMappings().scorePlayerTeam == null) {
            return;
        }
        MappingMethod mappingMethod = Vape.INSTANCE.getMappings().scorePlayerTeam.getFormatPlayerNameMethod();
        if (mappingMethod == null) {
            return;
        }
        CtBehavior ctBehavior = this.F(mappingMethod);
        if (ctBehavior == null) {
            return;
        }
        try {
            ScorePlayerTeamFormatPlayerNameMappingTask.p(TextReplaceV2.class);
            if (ForgeVersion.MC_1_16_5.d()) {
                ctBehavior.insertAfter("$_ = " + TextReplaceV2.class.getName() + ".replaceScoreboardComponent($_);");
            } else {
                ctBehavior.insertAfter("$_ = " + TextReplaceV2.class.getName() + ".replaceScoreboardText($_);");
            }
        }
        catch (CannotCompileException e) {
            Vape.logThrowable(e);
        }
    }
}

