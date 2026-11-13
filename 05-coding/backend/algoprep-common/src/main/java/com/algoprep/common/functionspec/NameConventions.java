package com.algoprep.common.functionspec;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/** Shared, locale-independent naming and exact DD reserved-word tables. */
public final class NameConventions {
  private static final Pattern IDENTIFIER = Pattern.compile("^[A-Za-z][A-Za-z0-9_]{0,63}$");
  public static final Map<Language, Set<String>> RESERVED_WORDS =
      Map.of(
          Language.JAVA,
              words(
                  "abstract assert boolean break byte case catch char class const continue default do double else enum extends final finally float for goto if implements import instanceof int interface long native new package private protected public return short static strictfp super switch synchronized this throw throws transient try void volatile while true false null var record yield"),
          Language.CPP,
              words(
                  "alignas alignof and and_eq asm auto bitand bitor bool break case catch char char8_t char16_t char32_t class compl concept const consteval constexpr constinit const_cast continue co_await co_return co_yield decltype default delete do double dynamic_cast else enum explicit export extern false float for friend goto if inline int long mutable namespace new noexcept not not_eq nullptr operator or or_eq private protected public register reinterpret_cast requires return short signed sizeof static static_assert static_cast struct switch template this thread_local throw true try typedef typeid typename union unsigned using virtual void volatile wchar_t while xor xor_eq"),
          Language.PYTHON,
              words(
                  "False None True and as assert async await break class continue def del elif else except finally for from global if import in is lambda nonlocal not or pass raise return try while with yield"));

  private NameConventions() {}

  public static boolean isIdentifier(String name) {
    return name != null && IDENTIFIER.matcher(name).matches();
  }

  public static boolean hasReservedPrefix(String name) {
    return name != null && name.toLowerCase(Locale.ROOT).startsWith("aph");
  }

  public static String forLanguage(
      String rawName, Language language, Map<String, String> overrides) {
    if (overrides != null) {
      for (var entry : overrides.entrySet()) {
        if (language.name().equalsIgnoreCase(entry.getKey())) {
          return entry.getValue();
        }
      }
    }
    String separated =
        rawName
            .replaceAll("([A-Z]+)([A-Z][a-z])", "$1_$2")
            .replaceAll("([a-z0-9])([A-Z])", "$1_$2");
    var words =
        Arrays.stream(separated.split("_+"))
            .filter(s -> !s.isEmpty())
            .map(s -> s.toLowerCase(Locale.ROOT))
            .toList();
    if (language == Language.PYTHON) {
      return String.join("_", words);
    }
    StringBuilder result = new StringBuilder(words.getFirst());
    for (String word : words.subList(1, words.size())) {
      result.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
    }
    return result.toString();
  }

  private static Set<String> words(String value) {
    return Arrays.stream(value.split(" ")).collect(Collectors.toUnmodifiableSet());
  }
}
