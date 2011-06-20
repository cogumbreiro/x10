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
/**************************************************************************
 * WARNING!  THIS JAVA FILE IS AUTO-GENERATED FROM x10/parser/X10Lexer.gi *
 **************************************************************************/

package x10.parser;

public class X10Lexerprs implements lpg.runtime.ParseTable, X10Lexersym {
    public final static int ERROR_SYMBOL = 0;
    public final int getErrorSymbol() { return ERROR_SYMBOL; }

    public final static int SCOPE_UBOUND = 0;
    public final int getScopeUbound() { return SCOPE_UBOUND; }

    public final static int SCOPE_SIZE = 0;
    public final int getScopeSize() { return SCOPE_SIZE; }

    public final static int MAX_NAME_LENGTH = 0;
    public final int getMaxNameLength() { return MAX_NAME_LENGTH; }

    public final static int NUM_STATES = 82;
    public final int getNumStates() { return NUM_STATES; }

    public final static int NT_OFFSET = 102;
    public final int getNtOffset() { return NT_OFFSET; }

    public final static int LA_STATE_OFFSET = 1111;
    public final int getLaStateOffset() { return LA_STATE_OFFSET; }

    public final static int MAX_LA = 2;
    public final int getMaxLa() { return MAX_LA; }

    public final static int NUM_RULES = 430;
    public final int getNumRules() { return NUM_RULES; }

    public final static int NUM_NONTERMINALS = 56;
    public final int getNumNonterminals() { return NUM_NONTERMINALS; }

    public final static int NUM_SYMBOLS = 158;
    public final int getNumSymbols() { return NUM_SYMBOLS; }

    public final static int SEGMENT_SIZE = 8192;
    public final int getSegmentSize() { return SEGMENT_SIZE; }

    public final static int START_STATE = 431;
    public final int getStartState() { return START_STATE; }

    public final static int IDENTIFIER_SYMBOL = 0;
    public final int getIdentifier_SYMBOL() { return IDENTIFIER_SYMBOL; }

    public final static int EOFT_SYMBOL = 99;
    public final int getEoftSymbol() { return EOFT_SYMBOL; }

    public final static int EOLT_SYMBOL = 103;
    public final int getEoltSymbol() { return EOLT_SYMBOL; }

    public final static int ACCEPT_ACTION = 680;
    public final int getAcceptAction() { return ACCEPT_ACTION; }

    public final static int ERROR_ACTION = 681;
    public final int getErrorAction() { return ERROR_ACTION; }

    public final static boolean BACKTRACK = false;
    public final boolean getBacktrack() { return BACKTRACK; }

    public final int getStartSymbol() { return lhs(0); }
    public final boolean isValidForParser() { return X10Lexersym.isValidForParser; }


    public interface IsNullable {
        public final static byte isNullable[] = {0,
            0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,1,1,0,0,0,
            0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,1,0,0,0,0,
            0,0,0,0,0,0,0,0
        };
    };
    public final static byte isNullable[] = IsNullable.isNullable;
    public final boolean isNullable(int index) { return isNullable[index] != 0; }

    public interface ProsthesesIndex {
        public final static byte prosthesesIndex[] = {0,
            40,41,47,43,44,45,21,46,27,29,
            31,42,55,24,25,26,30,32,35,48,
            50,2,3,4,5,6,7,8,9,10,
            11,12,13,14,15,16,17,18,19,20,
            22,23,28,33,34,36,37,38,39,49,
            51,52,53,56,1,54
        };
    };
    public final static byte prosthesesIndex[] = ProsthesesIndex.prosthesesIndex;
    public final int prosthesesIndex(int index) { return prosthesesIndex[index]; }

    public interface IsKeyword {
        public final static byte isKeyword[] = {0,
            0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,
            0,0
        };
    };
    public final static byte isKeyword[] = IsKeyword.isKeyword;
    public final boolean isKeyword(int index) { return isKeyword[index] != 0; }

    public interface BaseCheck {
        public final static byte baseCheck[] = {0,
            1,3,3,3,1,1,1,1,1,1,
            1,1,1,1,1,1,1,1,1,1,
            1,1,1,1,1,1,1,1,1,1,
            1,1,1,1,1,1,1,1,1,1,
            1,1,1,2,2,2,2,2,2,2,
            2,3,2,2,2,2,2,2,2,2,
            3,3,4,2,2,3,1,3,2,2,
            2,2,3,3,3,3,3,3,2,3,
            3,2,1,2,2,2,3,3,2,2,
            5,3,2,2,0,1,2,1,2,2,
            0,2,0,2,1,2,1,2,2,2,
            3,2,3,3,1,1,1,1,1,1,
            1,1,1,1,1,1,1,1,1,1,
            1,1,1,1,1,1,1,1,1,1,
            1,1,1,1,1,1,1,1,1,1,
            1,1,1,1,1,1,1,1,1,1,
            1,1,1,1,1,1,1,1,1,1,
            1,1,1,1,1,1,1,1,1,1,
            1,1,1,1,1,1,1,1,1,1,
            1,1,1,1,1,1,1,1,1,1,
            1,1,1,1,1,1,1,1,1,2,
            3,1,1,1,1,1,1,1,1,1,
            1,1,1,1,1,1,1,1,2,1,
            2,2,1,1,1,1,1,1,1,1,
            1,1,1,1,1,1,1,1,1,1,
            1,1,1,1,1,1,1,1,1,1,
            1,1,1,1,1,1,1,1,1,1,
            1,1,1,1,1,1,1,1,1,1,
            1,1,1,1,1,1,1,1,1,1,
            1,1,1,1,1,1,1,1,1,1,
            1,1,1,1,1,1,1,1,1,1,
            1,1,1,1,1,1,1,1,1,1,
            1,1,1,1,1,1,1,1,1,1,
            1,1,1,1,1,1,1,1,1,1,
            1,1,1,1,1,1,1,1,1,1,
            1,1,1,1,1,1,1,1,1,1,
            1,1,1,1,1,1,1,1,1,1,
            1,1,1,1,1,1,1,1,1,1,
            1,1,1,1,1,1,1,1,1,1,
            1,1,1,1,6,2,1,1,1,1,
            1,1,1,6,2,1,1,1,1,1,
            1,1,6,2,2,2,2,2,2,2,
            2,2,2,1,2,2,2,2,2,3
        };
    };
    public final static byte baseCheck[] = BaseCheck.baseCheck;
    public final int baseCheck(int index) { return baseCheck[index]; }
    public final static byte rhs[] = baseCheck;
    public final int rhs(int index) { return rhs[index]; };

    public interface BaseAction {
        public final static char baseAction[] = {
            22,22,22,22,22,22,22,22,22,22,
            22,22,22,22,22,22,22,22,22,22,
            22,22,22,22,22,22,22,22,22,22,
            22,22,22,22,22,22,22,22,22,22,
            22,22,22,22,22,22,22,22,22,22,
            22,22,22,22,22,22,22,22,22,22,
            22,22,22,22,22,22,22,27,27,28,
            29,30,31,32,32,33,33,34,34,35,
            35,35,35,36,36,36,36,36,36,36,
            37,38,44,44,44,44,45,45,39,46,
            46,24,24,25,25,7,7,42,42,43,
            43,43,17,17,17,12,12,12,12,12,
            4,4,4,4,4,5,5,5,5,5,
            5,5,5,5,5,5,5,5,5,5,
            5,5,5,5,5,5,5,5,5,5,
            5,6,6,6,6,6,6,6,6,6,
            6,6,6,6,6,6,6,6,6,6,
            6,6,6,6,6,6,6,1,1,1,
            1,1,1,1,1,1,1,8,8,8,
            8,8,8,8,8,3,3,3,3,3,
            3,3,3,3,3,3,3,2,2,20,
            20,20,10,10,11,11,14,14,15,15,
            16,16,9,9,18,18,41,41,40,40,
            23,23,23,50,50,50,50,50,50,50,
            50,50,50,50,50,50,50,50,50,50,
            50,50,50,50,50,50,50,50,50,50,
            50,50,21,21,21,21,21,21,21,21,
            21,21,21,21,21,21,21,21,21,21,
            21,21,21,21,21,21,21,21,21,21,
            51,51,51,51,51,51,51,51,51,51,
            51,51,51,51,51,51,51,51,51,51,
            51,51,51,51,51,51,51,51,52,52,
            52,52,52,52,52,52,52,52,52,52,
            52,52,52,52,52,52,52,52,52,52,
            52,52,52,52,52,52,53,53,53,53,
            53,53,53,53,53,53,53,53,53,53,
            53,53,53,53,53,53,53,53,53,53,
            53,53,53,53,19,19,19,19,56,56,
            47,47,47,47,47,47,47,47,49,49,
            49,49,49,49,49,49,49,26,26,26,
            26,26,26,26,26,26,48,48,48,48,
            48,48,48,48,48,13,13,13,13,13,
            13,13,13,13,22,22,22,22,22,22,
            54,308,669,646,1138,668,668,668,472,809,
            566,566,566,670,410,104,229,1296,104,104,
            104,1351,106,433,559,304,730,104,491,6,
            7,8,9,10,11,12,13,14,15,16,
            17,434,1108,106,540,1,94,533,1213,94,
            94,94,82,89,1126,424,100,94,552,582,
            631,1304,519,104,94,104,94,608,665,475,
            532,665,665,665,405,640,639,526,509,102,
            665,444,102,102,102,832,108,108,108,1441,
            524,102,1145,665,103,92,1538,644,92,92,
            92,1142,1162,205,100,78,92,100,100,100,
            1135,80,87,92,1158,92,73,75,77,665,
            79,86,786,515,515,515,102,531,621,707,
            232,102,1180,231,231,231,855,584,584,584,
            878,588,588,588,901,595,595,595,1396,106,
            100,1143,1185,100,924,599,599,599,947,603,
            603,603,81,88,515,970,607,607,607,993,
            404,404,404,1016,611,611,611,1039,615,615,
            615,1062,395,395,395,1085,413,413,413,1283,
            451,1261,578,1340,646,1216,451,202,578,1233,
            646,1272,578,519,1362,653,1377,655,578,1549,
            1553,653,1171,655,1450,414,1407,106,76,74,
            396,508,211,1418,106,1429,106,751,1535,1314,
            1211,1536,1212,1537,1552,746,1194,1556,1200,1440,
            1201,1472,1481,1490,1499,1508,1517,1526,1540,681,
            681
        };
    };
    public final static char baseAction[] = BaseAction.baseAction;
    public final int baseAction(int index) { return baseAction[index]; }
    public final static char lhs[] = baseAction;
    public final int lhs(int index) { return lhs[index]; };

    public interface TermCheck {
        public final static byte termCheck[] = {0,
            0,1,2,3,4,5,6,7,8,9,
            10,11,12,13,14,15,16,17,18,19,
            20,21,22,23,24,25,26,27,28,29,
            30,31,32,33,34,35,36,37,38,39,
            40,41,42,43,44,45,46,47,48,49,
            50,51,52,53,54,55,56,57,58,59,
            60,61,62,63,64,65,66,67,68,69,
            70,71,72,73,74,75,76,77,78,79,
            80,81,82,83,84,85,86,87,88,89,
            90,91,92,93,94,95,96,97,98,0,
            100,101,0,1,2,3,4,5,6,7,
            8,9,10,11,12,13,14,15,16,17,
            18,19,20,21,22,23,24,25,26,27,
            28,29,30,31,32,33,34,35,36,37,
            38,39,40,41,42,43,44,45,46,47,
            48,49,50,51,52,53,54,55,56,57,
            58,59,60,61,62,63,64,65,66,67,
            68,69,70,71,72,73,74,75,76,77,
            78,79,80,81,82,83,84,85,86,87,
            88,89,90,91,92,93,94,95,96,97,
            98,0,100,101,0,1,2,3,4,5,
            6,7,8,9,10,11,12,13,14,15,
            16,17,18,19,20,21,22,23,24,25,
            26,27,28,29,30,31,32,33,34,35,
            36,37,38,39,40,41,42,43,44,45,
            46,47,48,49,50,51,52,53,54,55,
            56,57,58,59,60,61,62,63,64,65,
            66,67,68,69,70,71,72,73,74,75,
            76,77,78,79,80,81,82,83,84,85,
            86,87,88,89,90,91,92,93,94,95,
            96,97,98,0,0,0,102,0,1,2,
            3,4,5,6,7,8,9,10,11,12,
            13,14,15,16,17,18,19,20,21,22,
            23,24,25,26,27,28,29,30,31,32,
            33,34,35,36,37,38,39,40,41,42,
            43,0,45,46,47,48,49,50,51,52,
            53,54,55,56,57,58,59,60,61,62,
            63,64,65,66,67,68,69,70,71,72,
            73,74,75,76,77,78,79,80,81,82,
            83,84,85,86,87,88,89,90,91,92,
            93,94,95,96,97,0,0,100,101,0,
            1,2,3,4,5,6,7,8,9,10,
            11,12,13,14,15,16,17,18,19,20,
            21,22,23,24,25,26,27,28,29,30,
            31,32,33,34,35,36,37,38,39,40,
            41,42,43,44,45,46,47,48,49,50,
            51,52,53,54,55,56,57,58,59,60,
            61,62,63,64,65,66,67,68,69,70,
            71,72,73,74,75,76,77,78,79,80,
            81,82,83,84,85,86,87,88,89,90,
            91,92,93,94,95,96,97,98,0,1,
            2,3,4,5,6,7,8,9,10,11,
            12,13,14,15,16,17,18,19,20,21,
            22,23,24,25,26,27,28,29,30,31,
            32,33,34,35,36,37,38,39,40,41,
            42,43,44,45,46,47,48,49,50,51,
            52,53,54,55,56,57,58,59,60,61,
            62,63,64,65,66,67,68,69,70,71,
            72,73,74,75,76,77,78,79,80,81,
            82,83,84,85,86,87,88,89,90,91,
            92,93,94,95,96,97,98,0,1,2,
            3,4,5,6,7,8,9,10,11,12,
            13,14,15,16,17,18,19,20,21,22,
            23,24,25,26,27,28,29,30,31,32,
            33,34,35,36,37,0,39,40,41,42,
            43,44,45,46,47,48,49,50,51,52,
            53,54,55,56,57,58,59,60,61,62,
            63,64,65,66,67,68,69,70,71,72,
            73,74,75,76,77,78,79,80,81,82,
            83,84,85,86,87,88,89,90,91,92,
            93,94,95,96,97,98,0,1,2,3,
            4,5,6,7,8,9,10,11,12,13,
            14,15,16,17,18,19,20,21,22,0,
            24,25,26,27,28,29,30,31,0,33,
            34,35,0,0,99,0,0,0,42,43,
            0,0,23,47,48,49,50,51,52,53,
            54,55,56,57,58,59,60,61,62,63,
            64,65,66,67,68,69,70,71,72,73,
            74,75,32,38,78,0,1,2,3,4,
            5,6,7,8,9,10,11,12,13,14,
            15,16,17,18,19,20,21,22,0,1,
            2,3,4,5,6,7,8,9,10,11,
            12,13,14,15,16,17,18,19,20,21,
            22,0,1,2,3,4,5,6,7,8,
            9,10,11,12,13,14,15,16,17,18,
            19,20,21,22,0,1,2,3,4,5,
            6,7,8,9,10,11,12,13,14,15,
            16,17,18,19,20,21,22,0,1,2,
            3,4,5,6,7,8,9,10,11,12,
            13,14,15,16,17,18,19,20,21,22,
            0,1,2,3,4,5,6,7,8,9,
            10,11,12,13,14,15,16,17,18,19,
            20,21,22,0,1,2,3,4,5,6,
            7,8,9,10,11,12,13,14,15,16,
            17,18,19,20,21,22,0,1,2,3,
            4,5,6,7,8,9,10,11,12,13,
            14,15,16,17,18,19,20,21,22,0,
            1,2,3,4,5,6,7,8,9,10,
            11,12,13,14,15,16,17,18,19,20,
            21,22,0,1,2,3,4,5,6,7,
            8,9,10,11,12,13,14,15,16,17,
            18,19,20,21,22,0,1,2,3,4,
            5,6,7,8,9,10,11,12,13,14,
            15,16,17,18,19,20,21,22,0,1,
            2,3,4,5,6,7,8,9,10,11,
            12,13,14,15,16,17,18,19,20,21,
            22,0,1,2,3,4,5,6,7,8,
            9,10,11,12,13,14,15,16,17,18,
            19,20,21,22,0,1,2,3,4,5,
            6,7,8,9,10,11,12,13,14,15,
            16,17,18,19,20,21,22,0,1,2,
            3,4,5,6,7,8,9,10,11,12,
            13,14,0,16,17,0,1,2,3,4,
            5,6,7,8,0,0,11,0,0,0,
            15,0,0,36,0,11,12,13,14,24,
            16,17,11,12,13,14,0,0,33,34,
            35,0,37,38,39,23,0,23,0,44,
            0,1,2,3,4,5,6,7,8,0,
            23,11,45,46,0,15,0,26,27,28,
            29,30,31,0,24,11,12,13,14,0,
            0,0,23,33,34,35,99,37,38,39,
            0,0,0,76,44,0,1,2,3,4,
            5,6,7,8,80,81,11,0,0,36,
            15,0,0,23,23,23,79,100,101,24,
            0,0,32,0,32,0,0,0,33,34,
            35,41,37,38,39,23,77,0,0,44,
            0,1,2,3,4,5,6,7,8,9,
            10,0,1,2,3,4,5,6,7,8,
            9,10,0,1,2,3,4,5,6,7,
            8,9,10,82,83,0,0,0,99,99,
            40,41,0,0,0,0,0,0,0,0,
            0,40,41,0,0,0,0,0,36,24,
            25,26,27,28,29,30,31,24,25,26,
            27,28,29,30,31,0,23,42,43,0,
            1,2,3,4,5,6,7,8,9,10,
            0,1,2,3,4,5,6,7,8,9,
            10,0,1,2,3,4,5,6,7,8,
            9,10,0,0,0,36,0,1,2,3,
            4,5,6,7,8,9,10,0,0,0,
            0,0,0,0,99,0,1,2,3,4,
            5,6,7,8,9,10,0,1,2,3,
            4,5,6,7,8,9,10,0,1,2,
            3,4,5,6,7,8,9,10,0,1,
            2,3,4,5,6,7,8,9,10,0,
            0,1,2,3,4,5,6,7,8,0,
            1,2,3,4,5,6,7,8,0,0,
            0,0,0,24,25,26,27,28,29,30,
            31,0,1,2,3,4,5,6,7,8,
            0,1,2,3,4,5,6,7,8,0,
            1,2,3,4,5,6,7,8,0,1,
            2,3,4,5,6,7,8,0,1,2,
            3,4,5,6,7,8,0,1,2,3,
            4,5,6,7,8,0,1,2,3,4,
            5,6,7,8,0,0,0,0,99,0,
            1,2,3,4,5,6,7,8,0,0,
            0,0,0,0,0,0,0,23,23,23,
            0,24,25,0,0,0,32,0,32,0,
            0,0,24,25,23,40,24,25,23,0,
            0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0
        };
    };
    public final static byte termCheck[] = TermCheck.termCheck;
    public final int termCheck(int index) { return termCheck[index]; }

    public interface TermAction {
        public final static char termAction[] = {0,
            681,775,775,775,775,775,775,775,775,775,
            775,775,775,775,775,775,775,775,775,775,
            775,775,775,775,775,775,775,775,775,775,
            775,775,775,775,775,775,775,775,775,775,
            775,775,775,775,775,775,775,775,775,775,
            775,775,775,775,775,775,775,775,775,775,
            775,775,775,775,775,775,775,775,775,775,
            775,775,775,775,775,775,775,775,775,775,
            775,775,774,524,775,775,775,775,775,775,
            775,775,775,775,775,775,775,775,775,103,
            775,775,681,773,773,773,773,773,773,773,
            773,773,773,773,773,773,773,773,773,773,
            773,773,773,773,773,773,773,773,773,773,
            773,773,773,773,773,773,773,773,773,773,
            773,773,773,773,773,773,773,773,773,773,
            773,773,773,773,773,773,773,773,773,773,
            773,773,773,773,773,773,773,773,773,773,
            773,773,773,773,773,773,773,773,773,773,
            773,773,773,773,772,778,773,773,773,773,
            773,773,773,773,773,773,773,773,773,773,
            773,101,773,773,98,781,781,781,781,781,
            781,781,781,781,781,781,781,781,781,781,
            781,781,781,781,781,781,781,781,781,781,
            781,781,781,781,781,781,781,781,781,781,
            781,781,781,781,781,781,781,781,781,781,
            781,781,781,781,781,781,781,781,781,781,
            781,781,781,781,781,781,781,781,781,781,
            781,781,781,781,781,781,781,781,781,781,
            781,781,781,781,781,781,781,781,781,781,
            781,781,781,781,781,781,781,781,781,781,
            781,781,781,95,681,681,781,681,447,669,
            669,669,669,669,669,669,669,669,668,668,
            668,668,668,668,668,668,668,668,668,668,
            658,668,668,668,668,668,668,668,668,478,
            668,668,668,619,486,497,627,661,660,668,
            668,681,670,670,668,668,668,668,668,668,
            668,668,668,668,668,668,668,668,668,668,
            668,668,668,668,668,668,668,668,668,668,
            668,668,668,670,562,668,544,657,522,662,
            659,581,456,724,712,629,709,721,722,719,
            720,723,707,704,705,681,681,670,670,681,
            785,785,785,785,785,785,785,785,785,785,
            785,785,785,785,785,785,785,785,785,785,
            785,785,785,785,785,785,785,785,785,785,
            785,785,785,785,785,785,684,785,785,785,
            785,785,785,642,785,785,785,785,785,785,
            785,785,785,785,785,785,785,785,785,785,
            785,785,785,785,785,785,785,785,785,785,
            785,785,785,785,785,785,785,785,785,785,
            785,785,785,785,785,785,785,785,785,785,
            785,785,785,785,785,785,785,785,681,783,
            783,783,783,783,783,783,783,783,783,783,
            783,783,783,783,783,783,783,783,783,783,
            783,783,783,783,783,783,783,783,783,783,
            783,783,783,783,783,783,783,683,783,783,
            783,783,625,783,783,783,783,783,783,783,
            783,783,783,783,783,783,783,783,783,783,
            783,783,783,783,783,783,783,783,783,783,
            783,783,783,783,783,783,783,783,783,783,
            783,783,783,783,783,783,783,783,783,783,
            783,783,783,783,783,783,783,681,665,665,
            665,665,665,665,665,665,665,665,665,665,
            665,665,665,665,665,665,665,665,665,665,
            665,665,665,665,665,665,665,665,665,665,
            665,665,665,665,665,681,665,665,665,665,
            665,484,665,665,665,665,665,665,665,665,
            665,665,665,665,665,665,665,665,665,665,
            665,665,665,665,665,665,665,665,665,665,
            665,665,665,665,665,665,665,665,665,665,
            665,665,665,665,665,665,665,665,665,665,
            665,665,665,665,665,665,1,913,913,913,
            913,913,913,913,913,913,913,912,912,912,
            912,912,912,912,912,912,912,912,912,37,
            912,912,912,912,912,912,912,912,681,912,
            912,912,681,681,680,681,681,681,912,912,
            27,681,730,912,912,912,912,912,912,912,
            912,912,912,912,912,912,912,912,912,912,
            912,912,912,912,912,912,912,912,912,912,
            912,912,1110,685,912,681,515,515,515,515,
            515,515,515,515,515,515,515,515,515,515,
            515,515,515,515,515,515,515,515,681,566,
            566,566,566,566,566,566,566,566,566,566,
            566,566,566,566,566,566,566,566,566,566,
            566,68,789,789,789,789,789,789,789,789,
            789,789,789,789,789,789,789,789,789,789,
            789,789,789,789,681,584,584,584,584,584,
            584,584,584,584,584,584,584,584,584,584,
            584,584,584,584,584,584,584,681,588,588,
            588,588,588,588,588,588,588,588,588,588,
            588,588,588,588,588,588,588,588,588,588,
            681,595,595,595,595,595,595,595,595,595,
            595,595,595,595,595,595,595,595,595,595,
            595,595,595,681,599,599,599,599,599,599,
            599,599,599,599,599,599,599,599,599,599,
            599,599,599,599,599,599,681,603,603,603,
            603,603,603,603,603,603,603,603,603,603,
            603,603,603,603,603,603,603,603,603,681,
            607,607,607,607,607,607,607,607,607,607,
            607,607,607,607,607,607,607,607,607,607,
            607,607,681,1085,1085,1085,1085,1085,1085,1085,
            1085,1085,1085,1085,1085,1085,1085,1085,1085,1085,
            1085,1085,1085,1085,1085,681,611,611,611,611,
            611,611,611,611,611,611,611,611,611,611,
            611,611,611,611,611,611,611,611,681,615,
            615,615,615,615,615,615,615,615,615,615,
            615,615,615,615,615,615,615,615,615,615,
            615,681,1076,1076,1076,1076,1076,1076,1076,1076,
            1076,1076,1076,1076,1076,1076,1076,1076,1076,1076,
            1076,1076,1076,1076,681,1094,1094,1094,1094,1094,
            1094,1094,1094,1094,1094,1094,1094,1094,1094,1094,
            1094,1094,1094,1094,1094,1094,1094,67,787,787,
            787,787,787,787,787,787,787,787,763,770,
            770,763,681,631,631,681,671,672,673,674,
            675,676,677,678,14,681,1099,18,681,681,
            1096,84,30,623,34,760,767,767,760,439,
            621,621,761,768,768,761,681,33,1098,1100,
            1097,72,1101,1102,1103,741,681,728,681,1104,
            681,1077,1077,1077,1077,1077,1077,1077,1077,32,
            738,1099,910,910,85,1096,681,754,758,756,
            754,758,756,425,570,762,769,769,762,230,
            228,681,739,1098,1100,1097,5,1101,1102,1103,
            20,22,35,910,1104,681,671,672,673,674,
            675,676,677,678,1109,664,1099,681,681,747,
            1096,681,29,735,737,729,746,910,910,574,
            681,681,1107,681,663,681,681,681,1098,1100,
            1097,726,1101,1102,1103,740,745,681,681,1104,
            90,578,578,578,578,578,578,578,578,578,
            578,681,578,578,578,578,578,578,578,578,
            578,578,36,451,451,451,451,451,451,451,
            451,451,451,780,455,105,681,681,1,18,
            636,634,681,5,681,681,681,681,681,681,
            681,636,634,21,681,681,681,681,666,67,
            67,67,67,67,67,67,67,532,532,640,
            526,639,640,526,639,681,736,552,552,110,
            646,646,646,646,646,646,646,646,646,646,
            109,787,787,787,787,787,787,787,787,787,
            787,681,653,653,653,653,653,653,653,653,
            653,653,681,681,681,1111,681,655,655,655,
            655,655,655,655,655,655,655,681,681,681,
            681,681,681,681,5,112,787,787,787,787,
            787,787,787,787,787,787,111,787,787,787,
            787,787,787,787,787,787,787,114,787,787,
            787,787,787,787,787,787,787,787,113,787,
            787,787,787,787,787,787,787,787,787,105,
            209,868,869,870,871,872,873,874,875,210,
            868,869,870,871,872,873,874,875,681,681,
            681,681,681,67,67,67,67,67,67,67,
            67,209,187,187,187,187,187,187,187,187,
            209,188,188,188,188,188,188,188,188,209,
            189,189,189,189,189,189,189,189,209,190,
            190,190,190,190,190,190,190,209,191,191,
            191,191,191,191,191,191,209,192,192,192,
            192,192,192,192,192,209,193,193,193,193,
            193,193,193,193,25,19,51,71,5,209,
            194,194,194,194,194,194,194,194,70,681,
            681,50,69,681,681,52,681,727,734,743,
            681,759,759,681,681,681,1108,681,667,681,
            681,681,757,757,742,725,755,755,744
        };
    };
    public final static char termAction[] = TermAction.termAction;
    public final int termAction(int index) { return termAction[index]; }
    public final int asb(int index) { return 0; }
    public final int asr(int index) { return 0; }
    public final int nasb(int index) { return 0; }
    public final int nasr(int index) { return 0; }
    public final int terminalIndex(int index) { return 0; }
    public final int nonterminalIndex(int index) { return 0; }
    public final int scopePrefix(int index) { return 0;}
    public final int scopeSuffix(int index) { return 0;}
    public final int scopeLhs(int index) { return 0;}
    public final int scopeLa(int index) { return 0;}
    public final int scopeStateSet(int index) { return 0;}
    public final int scopeRhs(int index) { return 0;}
    public final int scopeState(int index) { return 0;}
    public final int inSymb(int index) { return 0;}
    public final String name(int index) { return null; }
    public final int originalState(int state) { return 0; }
    public final int asi(int state) { return 0; }
    public final int nasi(int state) { return 0; }
    public final int inSymbol(int state) { return 0; }

    /**
     * assert(! goto_default);
     */
    public final int ntAction(int state, int sym) {
        return baseAction[state + sym];
    }

    /**
     * assert(! shift_default);
     */
    public final int tAction(int state, int sym) {
        int i = baseAction[state],
            k = i + sym;
        return termAction[termCheck[k] == sym ? k : i];
    }
    public final int lookAhead(int la_state, int sym) {
        int k = la_state + sym;
        return termAction[termCheck[k] == sym ? k : la_state];
    }
}