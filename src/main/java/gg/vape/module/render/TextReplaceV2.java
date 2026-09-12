package gg.vape.module.render;

import gg.vape.Vape;
import gg.vape.module.Category;
import gg.vape.module.Mod;
import gg.vape.value.StringMapValue;
import gg.vape.wrapper.impl.ITextComponent;
import gg.vape.wrapper.impl.TextComponentString;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TextReplaceV2 extends Mod {
    public final StringMapValue textReplacements = (StringMapValue) StringMapValue.create(
            this, "Replace scoreboard text", "Find text", "Replace with").setBase64Encoded(true);

    public TextReplaceV2() {
        super("Text Replace V2", -7926107, Category.RENDER, "Replaces text shown on the scoreboard");
        this.addValue(this.textReplacements);
        this.setSuffix("Replaces text shown on the scoreboard");
    }

    public static boolean isFormattingChar(char c) {
        return (c >= '0' && c <= '9')
                || (c >= 'a' && c <= 'f')
                || (c >= 'A' && c <= 'F')
                || (c >= 'k' && c <= 'o')
                || (c >= 'K' && c <= 'O')
                || c == 'r' || c == 'R';
    }

    public static String translateColorCodes(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        char[] chars = input.toCharArray();
        for (int i = 0; i < chars.length - 1; ++i) {
            if (chars[i] == '&' && isFormattingChar(chars[i + 1])) {
                chars[i] = '\u00a7';
                chars[i + 1] = Character.toLowerCase(chars[i + 1]);
            }
        }
        return new String(chars);
    }

    public static boolean hasFormattingCodes(String input) {
        if (input == null || input.length() < 2) {
            return false;
        }
        for (int i = 0; i < input.length() - 1; ++i) {
            char c = input.charAt(i);
            if ((c == '&' || c == '\u00a7') && isFormattingChar(input.charAt(i + 1))) {
                return true;
            }
        }
        return false;
    }

    public static String stripFormatting(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); ++i) {
            char c = input.charAt(i);
            if ((c == '&' || c == '\u00a7') && i + 1 < input.length() && isFormattingChar(input.charAt(i + 1))) {
                ++i;
                continue;
            }
            sb.append(c);
        }
        return sb.toString();
    }

    public String applySingleReplacement(String input, String findText, String replaceWith) {
        if (input == null || input.isEmpty() || findText == null || findText.isEmpty()) {
            return input;
        }

        String normalizedInput = Normalizer.normalize(input, Normalizer.Form.NFC);
        String normalizedFind = Normalizer.normalize(findText, Normalizer.Form.NFC);
        String normalizedReplace = translateColorCodes(
                replaceWith == null ? "" : Normalizer.normalize(replaceWith, Normalizer.Form.NFC));

        if (hasFormattingCodes(normalizedFind)) {
            String targetFind = translateColorCodes(normalizedFind);
            String inputToSearch = translateColorCodes(normalizedInput);
            String inputLower = inputToSearch.toLowerCase(Locale.ROOT);
            String findLower = targetFind.toLowerCase(Locale.ROOT);

            int index = inputLower.indexOf(findLower);
            if (index < 0) {
                return normalizedInput;
            }

            StringBuilder sb = new StringBuilder();
            int lastIndex = 0;
            while (index >= 0) {
                sb.append(normalizedInput, lastIndex, index);
                sb.append(normalizedReplace);
                lastIndex = index + findLower.length();
                index = inputLower.indexOf(findLower, lastIndex);
            }
            sb.append(normalizedInput.substring(lastIndex));
            return sb.toString();
        }

        int inputLen = normalizedInput.length();
        StringBuilder visibleText = new StringBuilder();
        List<Integer> rawIndexMap = new ArrayList<>();

        for (int i = 0; i < inputLen; ++i) {
            char c = normalizedInput.charAt(i);
            if ((c == '&' || c == '\u00a7') && i + 1 < inputLen && isFormattingChar(normalizedInput.charAt(i + 1))) {
                ++i;
                continue;
            }
            rawIndexMap.add(i);
            visibleText.append(c);
        }
        rawIndexMap.add(inputLen);

        String visibleStr = visibleText.toString();
        String visibleLower = visibleStr.toLowerCase(Locale.ROOT);
        String findLower = normalizedFind.toLowerCase(Locale.ROOT);

        int matchIndex = visibleLower.indexOf(findLower);
        if (matchIndex < 0) {
            return normalizedInput;
        }

        StringBuilder result = new StringBuilder();
        int lastRawIndex = 0;

        while (matchIndex >= 0) {
            int rawStart = rawIndexMap.get(matchIndex);
            int rawEnd = rawIndexMap.get(matchIndex + findLower.length());

            result.append(normalizedInput, lastRawIndex, rawStart);
            result.append(normalizedReplace);

            lastRawIndex = rawEnd;
            matchIndex = visibleLower.indexOf(findLower, matchIndex + findLower.length());
        }

        result.append(normalizedInput.substring(lastRawIndex));
        return result.toString();
    }

    public String applyReplacements(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        Map<String, String> replacements = this.textReplacements.getValue();
        if (replacements == null || replacements.isEmpty()) {
            return text;
        }

        String result = text;
        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            String find = entry.getKey();
            String replace = entry.getValue();
            result = this.applySingleReplacement(result, find, replace);
        }
        return result;
    }

    public static String replaceScoreboardText(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        if (Vape.INSTANCE == null || Vape.INSTANCE.getModManager() == null) {
            return text;
        }
        TextReplaceV2 module = Vape.INSTANCE.getModManager().getMod(TextReplaceV2.class);
        if (module == null || !module.isEnabled()) {
            return text;
        }
        return module.applyReplacements(text);
    }

    public static Object replaceScoreboardComponent(Object componentObj) {
        if (componentObj == null) {
            return null;
        }
        if (componentObj instanceof String) {
            return replaceScoreboardText((String) componentObj);
        }
        if (Vape.INSTANCE == null || Vape.INSTANCE.getModManager() == null) {
            return componentObj;
        }
        TextReplaceV2 module = Vape.INSTANCE.getModManager().getMod(TextReplaceV2.class);
        if (module == null || !module.isEnabled()) {
            return componentObj;
        }

        try {
            ITextComponent component = new ITextComponent(componentObj);
            if (component.isNull()) {
                return componentObj;
            }
            String formatted = component.getFormattedText();
            if (formatted == null || formatted.isEmpty()) {
                return componentObj;
            }
            String replaced = module.applyReplacements(formatted);
            if (formatted.equals(replaced)) {
                return componentObj;
            }
            return TextComponentString.create(replaced).getObject();
        } catch (Exception e) {
            return componentObj;
        }
    }
}

