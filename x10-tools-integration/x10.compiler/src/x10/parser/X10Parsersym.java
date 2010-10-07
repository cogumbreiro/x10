/*
 *  This file is part of the X10 project (http://x10-lang.org).
 *
 *  This file is licensed to You under the Eclipse Public License (EPL);
 *  You may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *      http://www.opensource.org/licenses/eclipse-1.0.php
 *
 *  (C) Copyright IBM Corporation 2006-2010.
 */

package x10.parser;

public interface X10Parsersym {
    public final static int
      TK_IntegerLiteral = 21,
      TK_LongLiteral = 22,
      TK_FloatingPointLiteral = 23,
      TK_DoubleLiteral = 24,
      TK_CharacterLiteral = 25,
      TK_StringLiteral = 26,
      TK_MINUS_MINUS = 38,
      TK_OR = 73,
      TK_MINUS = 36,
      TK_MINUS_EQUAL = 88,
      TK_NOT = 33,
      TK_NOT_EQUAL = 74,
      TK_REMAINDER = 63,
      TK_REMAINDER_EQUAL = 89,
      TK_AND = 75,
      TK_AND_AND = 80,
      TK_AND_EQUAL = 90,
      TK_LPAREN = 1,
      TK_RPAREN = 15,
      TK_MULTIPLY = 61,
      TK_MULTIPLY_EQUAL = 91,
      TK_COMMA = 19,
      TK_DOT = 35,
      TK_DIVIDE = 64,
      TK_DIVIDE_EQUAL = 92,
      TK_COLON = 46,
      TK_SEMICOLON = 17,
      TK_QUESTION = 93,
      TK_AT = 14,
      TK_LBRACKET = 2,
      TK_RBRACKET = 44,
      TK_XOR = 76,
      TK_XOR_EQUAL = 94,
      TK_LBRACE = 40,
      TK_OR_OR = 82,
      TK_OR_EQUAL = 95,
      TK_RBRACE = 41,
      TK_TWIDDLE = 42,
      TK_PLUS = 37,
      TK_PLUS_PLUS = 39,
      TK_PLUS_EQUAL = 96,
      TK_LESS = 65,
      TK_LEFT_SHIFT = 66,
      TK_LEFT_SHIFT_EQUAL = 97,
      TK_RIGHT_SHIFT = 67,
      TK_RIGHT_SHIFT_EQUAL = 98,
      TK_UNSIGNED_RIGHT_SHIFT = 68,
      TK_UNSIGNED_RIGHT_SHIFT_EQUAL = 99,
      TK_LESS_EQUAL = 69,
      TK_EQUAL = 43,
      TK_EQUAL_EQUAL = 58,
      TK_GREATER = 70,
      TK_GREATER_EQUAL = 71,
      TK_ELLIPSIS = 124,
      TK_RANGE = 108,
      TK_ARROW = 34,
      TK_DARROW = 100,
      TK_SUBTYPE = 47,
      TK_SUPERTYPE = 72,
      TK_abstract = 49,
      TK_as = 101,
      TK_assert = 109,
      TK_async = 102,
      TK_at = 6,
      TK_ateach = 81,
      TK_atomic = 4,
      TK_await = 7,
      TK_break = 110,
      TK_case = 77,
      TK_catch = 111,
      TK_class = 48,
      TK_clocked = 3,
      TK_continue = 112,
      TK_def = 103,
      TK_default = 78,
      TK_do = 104,
      TK_else = 123,
      TK_extends = 105,
      TK_false = 27,
      TK_final = 50,
      TK_finally = 113,
      TK_finish = 45,
      TK_for = 106,
      TK_goto = 125,
      TK_here = 8,
      TK_if = 114,
      TK_implements = 115,
      TK_import = 79,
      TK_in = 62,
      TK_instanceof = 83,
      TK_interface = 84,
      TK_native = 51,
      TK_new = 18,
      TK_next = 9,
      TK_null = 28,
      TK_offer = 10,
      TK_offers = 116,
      TK_operator = 117,
      TK_package = 107,
      TK_private = 52,
      TK_property = 85,
      TK_protected = 53,
      TK_public = 54,
      TK_resume = 11,
      TK_return = 118,
      TK_self = 29,
      TK_static = 55,
      TK_struct = 59,
      TK_super = 20,
      TK_switch = 119,
      TK_this = 16,
      TK_throw = 120,
      TK_transient = 56,
      TK_true = 30,
      TK_try = 121,
      TK_type = 5,
      TK_val = 12,
      TK_var = 57,
      TK_when = 122,
      TK_while = 86,
      TK_EOF_TOKEN = 87,
      TK_IDENTIFIER = 13,
      TK_SlComment = 126,
      TK_MlComment = 127,
      TK_DocComment = 128,
      TK_UnsignedIntegerLiteral = 31,
      TK_UnsignedLongLiteral = 32,
      TK_ErrorId = 60,
      TK_ERROR_TOKEN = 129;

    public final static String orderedTerminalSymbols[] = {
                 "",
                 "LPAREN",
                 "LBRACKET",
                 "clocked",
                 "atomic",
                 "type",
                 "at",
                 "await",
                 "here",
                 "next",
                 "offer",
                 "resume",
                 "val",
                 "IDENTIFIER",
                 "AT",
                 "RPAREN",
                 "this",
                 "SEMICOLON",
                 "new",
                 "COMMA",
                 "super",
                 "IntegerLiteral",
                 "LongLiteral",
                 "FloatingPointLiteral",
                 "DoubleLiteral",
                 "CharacterLiteral",
                 "StringLiteral",
                 "false",
                 "null",
                 "self",
                 "true",
                 "UnsignedIntegerLiteral",
                 "UnsignedLongLiteral",
                 "NOT",
                 "ARROW",
                 "DOT",
                 "MINUS",
                 "PLUS",
                 "MINUS_MINUS",
                 "PLUS_PLUS",
                 "LBRACE",
                 "RBRACE",
                 "TWIDDLE",
                 "EQUAL",
                 "RBRACKET",
                 "finish",
                 "COLON",
                 "SUBTYPE",
                 "class",
                 "abstract",
                 "final",
                 "native",
                 "private",
                 "protected",
                 "public",
                 "static",
                 "transient",
                 "var",
                 "EQUAL_EQUAL",
                 "struct",
                 "ErrorId",
                 "MULTIPLY",
                 "in",
                 "REMAINDER",
                 "DIVIDE",
                 "LESS",
                 "LEFT_SHIFT",
                 "RIGHT_SHIFT",
                 "UNSIGNED_RIGHT_SHIFT",
                 "LESS_EQUAL",
                 "GREATER",
                 "GREATER_EQUAL",
                 "SUPERTYPE",
                 "OR",
                 "NOT_EQUAL",
                 "AND",
                 "XOR",
                 "case",
                 "default",
                 "import",
                 "AND_AND",
                 "ateach",
                 "OR_OR",
                 "instanceof",
                 "interface",
                 "property",
                 "while",
                 "EOF_TOKEN",
                 "MINUS_EQUAL",
                 "REMAINDER_EQUAL",
                 "AND_EQUAL",
                 "MULTIPLY_EQUAL",
                 "DIVIDE_EQUAL",
                 "QUESTION",
                 "XOR_EQUAL",
                 "OR_EQUAL",
                 "PLUS_EQUAL",
                 "LEFT_SHIFT_EQUAL",
                 "RIGHT_SHIFT_EQUAL",
                 "UNSIGNED_RIGHT_SHIFT_EQUAL",
                 "DARROW",
                 "as",
                 "async",
                 "def",
                 "do",
                 "extends",
                 "for",
                 "package",
                 "RANGE",
                 "assert",
                 "break",
                 "catch",
                 "continue",
                 "finally",
                 "if",
                 "implements",
                 "offers",
                 "operator",
                 "return",
                 "switch",
                 "throw",
                 "try",
                 "when",
                 "else",
                 "ELLIPSIS",
                 "goto",
                 "SlComment",
                 "MlComment",
                 "DocComment",
                 "ERROR_TOKEN"
             };

    public final static int numTokenKinds = orderedTerminalSymbols.length;
    public final static boolean isValidForParser = true;
}
