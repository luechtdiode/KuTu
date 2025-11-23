package ch.seidel.kutu.data

object NameCodec {

  def encode(text: String): String = nameAccentNormalizerMap
    .keys
    .foldLeft(
      convertVowelMutations(
        cleanFromNonChars(
          cleanFromRepeatedChars(
            text.toUpperCase))))
    {(acc, key) =>
      acc.replaceAll(key, nameAccentNormalizerMap(key))
    }

  private val vowelMutationsMap = Map(
   192 -> "A", // - uppercase A, grave accent
   193 -> "A", // - uppercase A, acute accent
   194 -> "A", // - uppercase A, circumflex accent
   195 -> "A", // - uppercase A, tilde
   196 -> "AE", // - uppercase A, umlaut
   199 -> "C", // - uppercase C, cedilla
   200 -> "E", // - uppercase E, grave accent
   201 -> "E", // - uppercase E, acute accent
   202 -> "E", // - uppercase E, circumflex accent
   203 -> "E", // - uppercase E, umlaut
   204 -> "I", // - uppercase I, grave accent
   205 -> "I", // - uppercase I, acute accent
   206 -> "I", // - uppercase I, circumflex accent
   207 -> "I", // - uppercase I, umlaut
   209 -> "N", // - uppercase N, tilde
   210 -> "O", // - uppercase O, grave accent
   211 -> "O", // - uppercase O, acute accent
   212 -> "O", // - uppercase O, circumflex accent
   213 -> "O", // - uppercase O, tilde
   214 -> "OE", // - uppercase O, umlaut
   217 -> "U", // - uppercase U, grave accent
   218 -> "U", // - uppercase U, acute accent
   219 -> "U", // - uppercase U, circumflex accent
   220 -> "UE", // - uppercase U, umlaut
   223 -> "ss", // - lowercase sharps, German
   224 -> "a", // - lowercase a, grave accent
   225 -> "a", // - lowercase a, acute accent
   226 -> "a", // - lowercase a, circumflex accent
   227 -> "a", // - lowercase a, tilde
   228 -> "ae", // - lowercase a, umlaut
   231 -> "c", // - lowercase c, cedilla
   232 -> "e", // - lowercase e, grave accent
   233 -> "e", // - lowercase e, acute accent
   234 -> "e", // - lowercase e, circumflex accent
   235 -> "e", // - lowercase e, umlaut
   236 -> "i", // - lowercase i, grave accent
   237 -> "i", // - lowercase i, acute accent
   238 -> "i", // - lowercase i, circumflex accent
   239 -> "i", // - lowercase i, umlaut
   236 -> "i",
   237 -> "i",
   238 -> "i",
   239 -> "i",
   241 -> "n", // - lowercase n, tilde
   242 -> "o", // - lowercase o, grave accent
   243 -> "o", // - lowercase o, acute accent
   244 -> "o", // - lowercase o, circumflex accent
   245 -> "o", // - lowercase o, tilde
   246 -> "oe", // - lowercase o, umlaut
   249 -> "u", // - lowercase u, grave accent
   250 -> "u", // - lowercase u, acute accent
   251 -> "u", // - lowercase u, circumflex accent
   252 -> "ue")

  private val ignoredChars = Set(
    32, // Space       " "
    33, //             "!"
    34, //             """
    35, //             "#"
    36, //             "$"
    37, //             "%"
    38, // Ampersand   "&"
    39, // Hochkomma   "'"
    40, //             "("
    41, //             ")"
    42, //             "*"
    43, //             "+"
    44, // Komma       ","
    45, // Bindestrich "-"
    46, // Dot         "."
    47, // Slash       "/"
    58, // Doppelpunkt ":"
    59, // Strichpunkt ";"
    60, //             "<"
    61, //             "="
    62, //             ">"
    63, //             "?"
    91, //             "["
    92, // Backslash   "\"
    93, //             "]"
    95, //             "_"
    96, //             "`"
    136) //             "^"


  private val nameAccentNormalizerMap = Map(
    "J" -> "I",
    "Y" -> "I",
    "AI" -> "EI",
    "OU" -> "U",
    "DT" -> "T",
    "IE" -> "I",
    "AH" -> "A",
    "EH" -> "E",
    "IH" -> "I",
    "OH" -> "O",
    "UH" -> "U",
    "PH" -> "F",
    "TH" -> "T",
    "TZ" -> "Z",
    "CK" -> "K")

  private def convertVowelMutations(text: String) = text.toCharArray.map(char => vowelMutationsMap.getOrElse(char, char)).mkString("")

  private def cleanFromNonChars(text: String) = text.toCharArray.filter(char => !ignoredChars.contains(char)).mkString("")

  private def cleanFromRepeatedChars(text: String) = text.foldLeft(""){(acc, char) =>
    if acc.nonEmpty && acc.last == char then acc else acc :+ char
  }
}
