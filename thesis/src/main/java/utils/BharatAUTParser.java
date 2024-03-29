/* Hacked from Automatalib's InternalAUT Parser
 */
package utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import net.automatalib.automata.fsa.impl.compact.CompactNFA;
import net.automatalib.commons.util.IOUtil;
import net.automatalib.words.Alphabet;
import net.automatalib.words.impl.Alphabets;

/**
 * @author frohme
 */
class BharatAUTParser {

    private int initialState;
    private final Set<String> alphabetSymbols = new HashSet<>();
    private final Map<Integer, Map<String, Integer>> transitionMap = new HashMap<>();

    private final InputStream inputStream;

    private char[] currentLineContent;
    private int currentLine;
    private int currentPos;

    BharatAUTParser(InputStream is) {
        this.inputStream = is;
    }

    public <I> CompactNFA<I> parse(
            Function<String, I> inputTransformer) throws IOException {
        try (Reader isr = IOUtil.asUTF8Reader(inputStream);
                BufferedReader bisr = new BufferedReader(isr)) {

            // parsing
            parseHeader(bisr);
            while (parseTransition(bisr)) {
            }

            // automaton construction
            final Map<String, I> inputMap = alphabetSymbols.stream().collect(
                    Collectors.toMap(Function.identity(), inputTransformer));
            final Alphabet<I> alphabet = Alphabets
                    .fromCollection(inputMap.values());

            final CompactNFA<I> result = new CompactNFA<>(alphabet,
                    transitionMap.size());

            for (int i = 0; i < transitionMap.size(); i++) {
                result.addState();
            }

            for (final Map.Entry<Integer, Map<String, Integer>> outgoing : transitionMap
                    .entrySet()) {
                final Integer src = outgoing.getKey();
                for (final Map.Entry<String, Integer> targets : outgoing
                        .getValue().entrySet()) {
                    final String input = targets.getKey();
                    final Integer dest = targets.getValue();
                    result.addTransition(src, inputMap.get(input), dest);
                }
            }
            result.setInitial(initialState, true);

            return result;
        }
    }

    private void parseHeader(BufferedReader reader) throws IOException {
        final String line = reader.readLine();

        if (line == null) {
            throw new IllegalArgumentException(
                    buildErrorMessage("Missing description"));
        }

        currentLineContent = line.toCharArray();
        currentPos = 0;

        shiftToNextNonWhitespace();
        verifyDesAndShift();
        verifyLBracketAndShift();
        initialState = parseNumberAndShift();
        verifyCommaAndShift();
        parseNumberAndShift(); // ignore number of states
        verifyCommaAndShift();
        parseNumberAndShift(); // ignore number of transitions
        verifyRBracketAndShift();
    }

    private boolean parseTransition(BufferedReader reader) throws IOException {
        final String line = reader.readLine();

        if (line == null) {
            return false;
        }

        currentLineContent = line.toCharArray();
        currentLine++;
        currentPos = 0;

        final int start;
        final String label;
        final int dest;

        shiftToNextNonWhitespace();
        verifyLBracketAndShift();
        start = parseNumberAndShift();
        verifyCommaAndShift();
        label = parseLabelAndShift();
        verifyCommaAndShift();
        dest = parseNumberAndShift();
        verifyRBracketAndShift();

        alphabetSymbols.add(label);
        transitionMap.computeIfAbsent(start, k -> new HashMap<>()).put(label,
                dest);

        return true;
    }

    private void verifyDesAndShift() {

        if (currentLineContent[currentPos] != 'd'
                || currentLineContent[currentPos + 1] != 'e'
                || currentLineContent[currentPos + 2] != 's') {
            throw new IllegalArgumentException(
                    buildErrorMessage("Missing 'des' keyword"));
        }

        currentPos += 3;
        shiftToNextNonWhitespace();
    }

    private void verifyLBracketAndShift() {
        verifySymbolAndShift('(');
    }

    private void verifyRBracketAndShift() {
        verifySymbolAndShift(')');
    }

    private void verifyCommaAndShift() {
        verifySymbolAndShift(',');
    }

    private void verifySymbolAndShift(char symbol) {

        if (currentLineContent[currentPos] != symbol) {
            throw new IllegalArgumentException(
                    buildErrorMessage("Expected: " + symbol));
        }

        currentPos++;
        shiftToNextNonWhitespace();
    }

    private void shiftToNextNonWhitespace() {
        for (int i = currentPos; i < currentLineContent.length; i++) {
            switch (currentLineContent[i]) {
            case ' ':
            case '\t':
            case '\r': // probably already filtered by readline
            case '\n': // probably already filtered by readline
                break;
            default:
                currentPos = i;
                return;
            }
        }
    }

    private int parseNumberAndShift() {

        final StringBuilder sb = new StringBuilder();

        char sym = currentLineContent[currentPos];

        while (Character.isDigit(sym)) {
            sb.append(sym);
            currentPos++;
            sym = currentLineContent[currentPos];
        }

        // forward pointer
        shiftToNextNonWhitespace();
        return Integer.parseInt(sb.toString());
    }

    private String parseLabelAndShift() {

        if (currentLineContent[currentPos] == '"') {
            return parseQuotedLabelAndShift();
        } else {
            return parseNormalLabelAndShift();
        }
    }

    private String parseQuotedLabelAndShift() {
        int openingIndex = currentPos;
        int closingIndex = currentLineContent.length - 1;

        // find terminating "
        while (currentLineContent[closingIndex--] != '"') {
        }

        // skip terminating " as well
        currentPos = closingIndex + 2;
        shiftToNextNonWhitespace();

        return new String(currentLineContent, openingIndex + 1,
                closingIndex - openingIndex);
    }

    private String parseNormalLabelAndShift() {

        final char firstChar = currentLineContent[currentPos];

        if (currentLineContent[currentPos] == '*') {
            currentPos++;
            shiftToNextNonWhitespace();
            return "*";
        } else if (Character.isLetter(firstChar)) {
            int startIdx = currentPos;

            while (isValidIdentifier()) {
                currentPos++;
            }

            int endIdx = currentPos;

            shiftToNextNonWhitespace();
            return new String(currentLineContent, startIdx, endIdx - startIdx);
        } else {
            throw new IllegalArgumentException(
                    buildErrorMessage("Invalid unquoted label"));
        }
    }

    private boolean isValidIdentifier() {
        final char currentChar = currentLineContent[currentPos];
        return Character.isLetterOrDigit(currentChar) || currentChar == '_';
    }

    private String buildErrorMessage(String desc) {
        return "In line " + currentLine + ", col " + currentPos + ": " + desc;
    }

}