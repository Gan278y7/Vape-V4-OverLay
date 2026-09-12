package gg.vape.mapping;

import gg.vape.Vape;
import gg.vape.module.render.TextReplaceV2;
import gg.vape.wrapper.impl.ForgeVersion;
import javassist.CannotCompileException;
import javassist.CtBehavior;

public class ScoreObjectiveDisplayNameMappingTask
extends JavassistMappingTask {
    public ScoreObjectiveDisplayNameMappingTask() {
        super(MappedClasses.Y);
    }

    @Override
    public void transform() {
        if (Vape.INSTANCE.getMappings() == null || Vape.INSTANCE.getMappings().scoreObjective == null) {
            return;
        }
        MappingMethod mappingMethod = Vape.INSTANCE.getMappings().scoreObjective.getDisplayNameMethod();
        if (mappingMethod == null) {
            return;
        }
        CtBehavior ctBehavior = this.F(mappingMethod);
        if (ctBehavior == null) {
            return;
        }
        try {
            ScoreObjectiveDisplayNameMappingTask.p(TextReplaceV2.class);
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

