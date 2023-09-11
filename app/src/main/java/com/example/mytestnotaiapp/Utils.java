package com.example.mytestnotaiapp;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * A utility class containing various static methods for common string manipulation tasks.
 */
public class Utils {

    /**
     * A constant representing the index value when a search operation does not find a match.
     */
    private static final int INDEX_NOT_FOUND = -1;

    /**
     * A constant representing an empty String.
     */
    private static final String EMPTY = "";


    /**
     * Abbreviates a given String within specified character limits,
     * specified by 'lower' and 'upper'. It appends the 'appendToEnd' String if the result is
     * truncated. The 'lower' limit represents the starting position, and 'upper' represents
     * the maximum length of the result. If 'upper' is -1, there is no maximum length.
     *
     * @param str         The input String to be abbreviated.
     * @param lower       The starting position from which to abbreviate 'str'.
     * @param upper       The maximum length for the abbreviated String, or -1 for no maximum length.
     * @param appendToEnd The String to append to the result if it is truncated.
     * @return The abbreviated String, conforming to the specified character limits.
     * If 'str' is empty or null, it returns 'str' as it is.
     * @throws IllegalArgumentException if 'upper' is less than -1 or if 'upper' is less than 'lower'.
     */
    public static String abbreviate(final String str, int lower, int upper, final String appendToEnd) {
        isTrue(upper >= -1, "upper value cannot be less than -1");
        isTrue(upper >= lower || upper == -1, "upper value is less than lower value");
        if (isEmpty(str)) {
            return str;
        }

        if (lower > str.length()) {
            lower = str.length();
        }

        if (upper == -1 || upper > str.length()) {
            upper = str.length();
        }

        final StringBuilder result = new StringBuilder();
        final int index = indexOf(str, " ", lower);
        if (index == -1) {
            result.append(str, 0, upper);
            if (upper != str.length()) {
                result.append(defaultString(appendToEnd));
            }
        } else {
            result.append(str, 0, Math.min(index, upper));
            result.append(defaultString(appendToEnd));
        }

        return result.toString();
    }

    /**
     * Generates initials from a given String using specified delimiters.
     * Initials are composed of the first character of each word separated by delimiters. If 'str'
     * is empty or null, it returns 'str' as is. If 'delimiters' is an empty array, it returns an
     * empty String. Delimiters are optional, and by default, whitespace characters are used as
     * delimiters.
     *
     * @param str        The input String from which to generate initials.
     * @param delimiters An optional array of delimiters used to separate words. If not provided,
     *                   whitespace characters are used as delimiters.
     * @return The generated new String with delimiters.
     */
    public static String initials(final String str, final char... delimiters) {
        if (isEmpty(str)) {
            return str;
        }
        if (delimiters != null && delimiters.length == 0) {
            return EMPTY;
        }
        final Set<Integer> delimiterSet = generateDelimiterSet(delimiters);
        final int strLen = str.length();
        final int[] newCodePoints = new int[strLen / 2 + 1];
        int count = 0;
        boolean lastWasGap = true;
        for (int i = 0; i < strLen; ) {
            final int codePoint = str.codePointAt(i);

            if (delimiterSet.contains(codePoint) || delimiters == null && Character.isWhitespace(codePoint)) {
                lastWasGap = true;
            } else if (lastWasGap) {
                newCodePoints[count++] = codePoint;
                lastWasGap = false;
            }

            i += Character.charCount(codePoint);
        }
        return new String(newCodePoints, 0, count);
    }

    /**
     * This method swaps the case (upper to lower and lower to upper) of characters in the input 'str'.
     * It preserves the case of the first character and handles title case characters.
     * If 'str' is empty or null, it returns 'str' as is.
     *
     * @param str The input String in which to swap the case of characters.
     * @return A new String with the case of characters swapped.
     */
    public static String swapCase(final String str) {
        if (isEmpty(str)) {
            return str;
        }
        final int strLen = str.length();
        final int[] newCodePoints = new int[strLen];
        int outOffset = 0;
        boolean whitespace = true;
        for (int index = 0; index < strLen; ) {
            final int oldCodepoint = str.codePointAt(index);
            final int newCodePoint;
            if (Character.isUpperCase(oldCodepoint) || Character.isTitleCase(oldCodepoint)) {
                newCodePoint = Character.toLowerCase(oldCodepoint);
                whitespace = false;
            } else if (Character.isLowerCase(oldCodepoint)) {
                if (whitespace) {
                    newCodePoint = Character.toTitleCase(oldCodepoint);
                    whitespace = false;
                } else {
                    newCodePoint = Character.toUpperCase(oldCodepoint);
                }
            } else {
                whitespace = Character.isWhitespace(oldCodepoint);
                newCodePoint = oldCodepoint;
            }
            newCodePoints[outOffset++] = newCodePoint;
            index += Character.charCount(newCodePoint);
        }
        return new String(newCodePoints, 0, outOffset);
    }

    /**
     * This method takes an input string 'str' and wraps it so that each line does not exceed
     * the specified 'wrapLength'. It also allows for custom line breaks using 'newLineStr'.
     * Long words can be wrapped or broken based on the 'wrapLongWords' parameter, and wrapping
     * can occur at specified characters defined by 'wrapOn'.
     *
     * @param str           The input string to be wrapped.
     * @param wrapLength    The maximum line width before wrapping.
     * @param newLineStr    The string to use for line breaks. Defaults to the system's line separator.
     * @param wrapLongWords If true, long words are wrapped; if false, they may be broken.
     * @param wrapOn        The characters at which to wrap lines, such as spaces or punctuation.
     *                      Defaults to space character ' ' if blank.
     * @return The wrapped string.
     */
    public static String wrap(final String str,
                              int wrapLength,
                              String newLineStr,
                              final boolean wrapLongWords,
                              String wrapOn) {
        if (str == null) {
            return null;
        }
        if (newLineStr == null) {
            newLineStr = System.lineSeparator();
        }
        if (wrapLength < 1) {
            wrapLength = 1;
        }
        if (isBlank(wrapOn)) {
            wrapOn = " ";
        }
        final Pattern patternToWrapOn = Pattern.compile(wrapOn);
        final int inputLineLength = str.length();
        int offset = 0;
        final StringBuilder wrappedLine = new StringBuilder(inputLineLength + 32);
        int matcherSize = -1;

        while (offset < inputLineLength) {
            int spaceToWrapAt = -1;
            Matcher matcher = patternToWrapOn.matcher(str.substring(offset,
                    Math.min((int) Math.min(Integer.MAX_VALUE, offset + wrapLength + 1L), inputLineLength)));
            if (matcher.find()) {
                if (matcher.start() == 0) {
                    matcherSize = matcher.end();
                    if (matcherSize != 0) {
                        offset += matcher.end();
                        continue;
                    }
                    offset += 1;
                }
                spaceToWrapAt = matcher.start() + offset;
            }

            if (inputLineLength - offset <= wrapLength) {
                break;
            }

            while (matcher.find()) {
                spaceToWrapAt = matcher.start() + offset;
            }

            if (spaceToWrapAt >= offset) {
                wrappedLine.append(str, offset, spaceToWrapAt);
                wrappedLine.append(newLineStr);
                offset = spaceToWrapAt + 1;
            } else if (wrapLongWords) {
                if (matcherSize == 0) {
                    offset--;
                }
                wrappedLine.append(str, offset, wrapLength + offset);
                wrappedLine.append(newLineStr);
                offset += wrapLength;
                matcherSize = -1;
            } else {
                matcher = patternToWrapOn.matcher(str.substring(offset + wrapLength));
                if (matcher.find()) {
                    matcherSize = matcher.end() - matcher.start();
                    spaceToWrapAt = matcher.start() + offset + wrapLength;
                }

                if (spaceToWrapAt >= 0) {
                    if (matcherSize == 0 && offset != 0) {
                        offset--;
                    }
                    wrappedLine.append(str, offset, spaceToWrapAt);
                    wrappedLine.append(newLineStr);
                    offset = spaceToWrapAt + 1;
                } else {
                    if (matcherSize == 0 && offset != 0) {
                        offset--;
                    }
                    wrappedLine.append(str, offset, str.length());
                    offset = inputLineLength;
                    matcherSize = -1;
                }
            }
        }

        if (matcherSize == 0 && offset < inputLineLength) {
            offset--;
        }

        wrappedLine.append(str, offset, str.length());

        return wrappedLine.toString();
    }


    /**
     * This method takes an array of character delimiters and converts them into a set of
     * integer code points. The resulting set is useful for identifying delimiter characters
     * when processing strings.
     *
     * @param delimiters An array of character delimiters to convert into a set of code points.
     * @return A Set of Integer code points representing the delimiters.
     */
    private static Set<Integer> generateDelimiterSet(final char[] delimiters) {
        final Set<Integer> delimiterHashSet = new HashSet<>();
        if (delimiters == null || delimiters.length == 0) {
            if (delimiters == null) {
                delimiterHashSet.add(Character.codePointAt(new char[]{' '}, 0));
            }

            return delimiterHashSet;
        }

        for (int index = 0; index < delimiters.length; index++) {
            delimiterHashSet.add(Character.codePointAt(delimiters, index));
        }
        return delimiterHashSet;
    }

    /**
     * Checks whether a given boolean expression is true, and if not, throws an IllegalArgumentException
     * with the provided error message.
     *
     * @param expression The boolean expression to evaluate.
     * @param message    The error message to include in the exception if the expression is false.
     * @throws IllegalArgumentException if the 'expression' is false.
     */
    private static void isTrue(final boolean expression, final String message) {
        if (!expression) {
            throw new IllegalArgumentException(message);
        }
    }


    /**
     * Calculates the length of a CharSequence.
     *
     * @param cs The CharSequence whose length is to be determined.
     * @return The length of the CharSequence if it is not null, otherwise, returns 0.
     */
    private static int length(final CharSequence cs) {
        return cs == null ? 0 : cs.length();
    }

    /**
     * Checks whether a CharSequence is null or empty.
     *
     * @param cs The CharSequence to check for null or emptiness.
     * @return true if the CharSequence is null or has a length of 0, otherwise, returns false.
     */
    private static boolean isEmpty(final CharSequence cs) {
        return cs == null || cs.length() == 0;
    }

    /**
     * Checks whether a CharSequence is null, empty, or consists only of whitespace characters.
     *
     * @param cs The CharSequence to check for null, emptiness, or whitespace characters.
     * @return true if the CharSequence is null, empty, or consists only of whitespace characters,
     * otherwise, returns false.
     */
    private static boolean isBlank(final CharSequence cs) {
        final int strLen = length(cs);
        if (strLen == 0) {
            return true;
        }
        for (int i = 0; i < strLen; i++) {
            if (!Character.isWhitespace(cs.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns the default String if the input String is null.*
     *
     * @param str The input String to be checked for null.
     * @return The default String value if 'str' is null, otherwise, 'str' itself.
     */
    private static String defaultString(final String str) {
        return Objects.toString(str, EMPTY);
    }

    /**
     * Finds the index of the first occurrence of a subsequence within a CharSequence
     * starting from a specified position.
     * This method searches for the first occurrence of the 'searchSeq' within the 'seq'
     * starting from the 'startPos' index. It returns the index of the first occurrence if found,
     * or 'INDEX_NOT_FOUND' if either 'seq' or 'searchSeq' is null, or if the subsequence is not found.
     *
     * @param seq       The input CharSequence in which to search for 'searchSeq'.
     * @param searchSeq The subsequence to search for within 'seq'.
     * @param startPos  The starting index from which to begin the search within 'seq'.
     * @return The index of the first occurrence of 'searchSeq' within 'seq', or 'INDEX_NOT_FOUND'
     */
    private static int indexOf(final CharSequence seq, final CharSequence searchSeq, final int startPos) {
        if (seq == null || searchSeq == null) {
            return INDEX_NOT_FOUND;
        }
        if (seq instanceof String) {
            return ((String) seq).indexOf(searchSeq.toString(), startPos);
        }
        if (seq instanceof StringBuilder) {
            return ((StringBuilder) seq).indexOf(searchSeq.toString(), startPos);
        }
        if (seq instanceof StringBuffer) {
            return ((StringBuffer) seq).indexOf(searchSeq.toString(), startPos);
        }
        return seq.toString().indexOf(searchSeq.toString(), startPos);
    }
}