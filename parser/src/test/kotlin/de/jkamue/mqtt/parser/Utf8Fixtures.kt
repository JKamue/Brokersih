package de.jkamue.mqtt.parser

object Utf8Fixtures {
    // @formatter:off
    /** Every entry MUST be rejected as a Malformed Packet. [MQTT-1.5.4-1] / [MQTT-1.5.4-2] */
    val malformed = mapOf(
        "lone continuation byte"  to "0003418042",        // 41 80 42
        "truncated 3-byte seq"    to "000341e282",        // 41 E2 82
        "truncated 4-byte seq"    to "000441f09f98",      // 41 F0 9F 98
        "overlong slash"          to "0002c0af",          // C0 AF
        "overlong NUL"            to "0002c080",          // C0 80  modified-UTF-8
        "CESU-8 U+D800"           to "0003eda080",        // ED A0 80
        "CESU-8 U+DFFF"           to "0003edbfbf",        // ED BF BF
        "CESU-8 surrogate pair"   to "0006eda080edb080",  // ED A0 80 ED B0 80
        "above U+10FFFF"          to "0004f5808080",      // F5 80 80 80
        "five-byte sequence"      to "0005f888808080",    // F8 88 80 80 80
        "U+0000 embedded"         to "0003410042",        // 41 00 42
        "U+0000 alone"            to "000100",            // 00
    )

    /** SHOULD NOT be sent; our receiver WILL treat as Malformed Packet. */
    val disallowed = mapOf(
        "U+0001 control"          to "0003410142",
        "U+001F control"          to "0003411f42",
        "U+007F delete"           to "0003417f42",
        "U+0085 NEL"              to "000441c28542",
        "U+009F control"          to "000441c29f42",
        "U+FDD0 noncharacter"     to "000541efb79042",
        "U+FDEF noncharacter"     to "000541efb7af42",
        "U+FFFE noncharacter"     to "000541efbfbe42",
        "U+FFFF noncharacter"     to "000541efbfbf42",
        "U+1FFFE noncharacter"    to "000641f09fbfbe42",
        "U+10FFFF noncharacter"   to "000641f48fbfbf42",
    )

    /** MUST be accepted; name to (hex to expected decoded value). */
    val valid = mapOf(
        "plain topic path"        to ("000b746f7069632f6c6576656c" to "topic/level"),
        "U+0020 space"            to ("0003412042" to "A B"),
        "two-byte umlauts"        to ("0006c3a4c3b6c3bc" to "äöü"),
        "three-byte japanese"     to ("0009e697a5e69cace8aa9e" to "日本語"),
        "U+1F600 emoji"           to ("0004f09f9880" to "\uD83D\uDE00"),
        "BOM not stripped"        to ("0004efbbbf41" to "\uFEFFA"),   // [MQTT-1.5.4-3]
        "ZWNBSP mid-string"       to ("000541efbbbf42" to "A\uFEFFB"),
        "23 bytes of 09AZaz"      to ("0017303132333435363738396162636465666768696a6b6c6d" to "0123456789abcdefghijklm"),
        // Assigned CJK Ext B ideograph, F0 A0 8F BE on the wire. Its low surrogate is \uDFFE, yet
        // it is NOT a noncharacter: that needs cp and 0xFFFE == 0xFFFE (high surrogate too).
        "U+203FE CJK ideograph"   to ("000641f0a08fbe42" to "A\uD840\uDFFEB"),
    )
    // @formatter:on
}
