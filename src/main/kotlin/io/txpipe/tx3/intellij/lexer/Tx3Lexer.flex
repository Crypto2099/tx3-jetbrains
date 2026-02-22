package io.txpipe.tx3.intellij.lexer;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;
import io.txpipe.tx3.intellij.lexer.Tx3TokenTypes;
import com.intellij.psi.TokenType;

%%

%class Tx3FlexLexer
%implements FlexLexer
%8bit
%function advance
%type IElementType
%eof{ return;
%eof}

// ── Macros ───────────────────────────────────────────────────────────────────
DIGIT           = [0-9]
HEX_DIGIT       = [0-9a-fA-F]
ALPHA           = [a-zA-Z]
ALPHA_NUM       = [a-zA-Z0-9_]
IDENT           = {ALPHA}{ALPHA_NUM}*
INT_LITERAL     = {DIGIT}+
HEX_PREFIX      = "0x"{HEX_DIGIT}+
STRING_LITERAL  = \"([^\"\\]|\\.)*\"
BYTES_LITERAL   = "#"{HEX_DIGIT}+
WHITE_SPACE     = [ \t\r\n]+
LINE_COMMENT    = "//"[^\r\n]*
BLOCK_COMMENT   = "/*"([^*]|\*[^/])*"*/"

%%

// ── Whitespace & Comments ────────────────────────────────────────────────────
{WHITE_SPACE}       { return TokenType.WHITE_SPACE; }
{LINE_COMMENT}      { return Tx3TokenTypes.LINE_COMMENT; }
{BLOCK_COMMENT}     { return Tx3TokenTypes.BLOCK_COMMENT; }

// ── Compound Literals (must precede their components) ────────────────────────
// UTxO reference:  0xABCDEF#1
{HEX_PREFIX}"#"{DIGIT}+             { return Tx3TokenTypes.UTXO_REF_LITERAL; }
// Asset identifier: 0xABCDEF."MYTOKEN"
{HEX_PREFIX}"."{STRING_LITERAL}     { return Tx3TokenTypes.ASSET_LITERAL; }

// ── Multi-char Operators (must precede single-char variants) ─────────────────
"..."               { return Tx3TokenTypes.OP_SPREAD; }
"::"                { return Tx3TokenTypes.OP_DOUBLE_COLON; }
"=="                { return Tx3TokenTypes.OP_EQ; }
"!="                { return Tx3TokenTypes.OP_NEQ; }
"<="                { return Tx3TokenTypes.OP_LE; }
">="                { return Tx3TokenTypes.OP_GE; }
"&&"                { return Tx3TokenTypes.OP_AND; }
"||"                { return Tx3TokenTypes.OP_OR; }

// ── Keywords with underscores (must precede IDENT rule) ──────────────────────
"min_amount"        { return Tx3TokenTypes.KW_MIN_AMOUNT; }
"datum_is"          { return Tx3TokenTypes.KW_DATUM_IS; }
"since_slot"        { return Tx3TokenTypes.KW_SINCE_SLOT; }
"until_slot"        { return Tx3TokenTypes.KW_UNTIL_SLOT; }

// ── Top-level Keywords ───────────────────────────────────────────────────────
"party"             { return Tx3TokenTypes.KW_PARTY; }
"policy"            { return Tx3TokenTypes.KW_POLICY; }
"record"            { return Tx3TokenTypes.KW_RECORD; }
"type"              { return Tx3TokenTypes.KW_TYPE; }
"tx"                { return Tx3TokenTypes.KW_TX; }
"env"               { return Tx3TokenTypes.KW_ENV; }
"asset"             { return Tx3TokenTypes.KW_ASSET; }

// ── Tx Body Block Keywords ───────────────────────────────────────────────────
"input"             { return Tx3TokenTypes.KW_INPUT; }
"output"            { return Tx3TokenTypes.KW_OUTPUT; }
"burn"              { return Tx3TokenTypes.KW_BURN; }
"mint"              { return Tx3TokenTypes.KW_MINT; }
"locals"            { return Tx3TokenTypes.KW_LOCALS; }
"collateral"        { return Tx3TokenTypes.KW_COLLATERAL; }
"reference"         { return Tx3TokenTypes.KW_REFERENCE; }
"signers"           { return Tx3TokenTypes.KW_SIGNERS; }
"validity"          { return Tx3TokenTypes.KW_VALIDITY; }
"metadata"          { return Tx3TokenTypes.KW_METADATA; }
"cardano"           { return Tx3TokenTypes.KW_CARDANO; }

// ── Field Keywords ───────────────────────────────────────────────────────────
"from"              { return Tx3TokenTypes.KW_FROM; }
"to"                { return Tx3TokenTypes.KW_TO; }
"amount"            { return Tx3TokenTypes.KW_AMOUNT; }
"datum"             { return Tx3TokenTypes.KW_DATUM; }
"redeemer"          { return Tx3TokenTypes.KW_REDEEMER; }
"ref"               { return Tx3TokenTypes.KW_REF; }
"script"            { return Tx3TokenTypes.KW_SCRIPT; }
"hash"              { return Tx3TokenTypes.KW_HASH; }
"drep"              { return Tx3TokenTypes.KW_DREP; }
"stake"             { return Tx3TokenTypes.KW_STAKE; }
"version"           { return Tx3TokenTypes.KW_VERSION; }
"coin"              { return Tx3TokenTypes.KW_COIN; }

// ── Control Keywords ─────────────────────────────────────────────────────────
"import"            { return Tx3TokenTypes.KW_IMPORT; }
"let"               { return Tx3TokenTypes.KW_LET; }
"if"                { return Tx3TokenTypes.KW_IF; }
"else"              { return Tx3TokenTypes.KW_ELSE; }
"true"              { return Tx3TokenTypes.KW_TRUE; }
"false"             { return Tx3TokenTypes.KW_FALSE; }

// ── Built-in Types ───────────────────────────────────────────────────────────
"Int"               { return Tx3TokenTypes.TYPE_INT; }
"Bytes"             { return Tx3TokenTypes.TYPE_BYTES; }
"Bool"              { return Tx3TokenTypes.TYPE_BOOL; }
"Unit"              { return Tx3TokenTypes.TYPE_UNIT; }
"UtxoRef"           { return Tx3TokenTypes.TYPE_UTXO_REF; }
"Address"           { return Tx3TokenTypes.TYPE_ADDRESS; }
"Value"             { return Tx3TokenTypes.TYPE_VALUE; }
"List"              { return Tx3TokenTypes.TYPE_LIST; }
"Map"               { return Tx3TokenTypes.TYPE_MAP; }

// ── Built-in Asset Constructors ──────────────────────────────────────────────
"Ada"               { return Tx3TokenTypes.BUILTIN_ADA; }

// ── Built-in Values ──────────────────────────────────────────────────────────
"fees"              { return Tx3TokenTypes.BUILTIN_FEES; }

// ── Single-char Operators ────────────────────────────────────────────────────
"+"                 { return Tx3TokenTypes.OP_PLUS; }
"-"                 { return Tx3TokenTypes.OP_MINUS; }
"*"                 { return Tx3TokenTypes.OP_MUL; }
"/"                 { return Tx3TokenTypes.OP_DIV; }
"<"                 { return Tx3TokenTypes.OP_LT; }
">"                 { return Tx3TokenTypes.OP_GT; }
"!"                 { return Tx3TokenTypes.OP_NOT; }
"="                 { return Tx3TokenTypes.OP_ASSIGN; }

// ── Punctuation ──────────────────────────────────────────────────────────────
"{"                 { return Tx3TokenTypes.LBRACE; }
"}"                 { return Tx3TokenTypes.RBRACE; }
"("                 { return Tx3TokenTypes.LPAREN; }
")"                 { return Tx3TokenTypes.RPAREN; }
"["                 { return Tx3TokenTypes.LBRACKET; }
"]"                 { return Tx3TokenTypes.RBRACKET; }
","                 { return Tx3TokenTypes.COMMA; }
";"                 { return Tx3TokenTypes.SEMICOLON; }
":"                 { return Tx3TokenTypes.COLON; }
"."                 { return Tx3TokenTypes.DOT; }

// ── Literals ─────────────────────────────────────────────────────────────────
{HEX_PREFIX}        { return Tx3TokenTypes.HEX_LITERAL; }
{INT_LITERAL}       { return Tx3TokenTypes.INT_LITERAL; }
{STRING_LITERAL}    { return Tx3TokenTypes.STRING_LITERAL; }
{BYTES_LITERAL}     { return Tx3TokenTypes.BYTES_LITERAL; }

// ── Identifiers ──────────────────────────────────────────────────────────────
{IDENT}             { return Tx3TokenTypes.IDENTIFIER; }

// ── Fallthrough ──────────────────────────────────────────────────────────────
[^]                 { return TokenType.BAD_CHARACTER; }
