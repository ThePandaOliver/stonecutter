package dev.kikugie.stitcher.scanner;

import dev.kikugie.stitcher.data.token.ContentType;
%%

%public
%class FlexScanner
%unicode
%function advance
%type ContentType

%state IN_SLASH_COMMENT
%state IN_STAR_COMMENT

%state IN_CHAR
%state IN_STRING

%%

<YYINITIAL> \/\/ { yybegin(IN_SLASH_COMMENT); return ContentType.COMMENT_START; }
<YYINITIAL> \/\* { yybegin(IN_STAR_COMMENT); return ContentType.COMMENT_START; }
<YYINITIAL> ' { yybegin(IN_CHAR); }
<YYINITIAL> \" { yybegin(IN_STRING); }
<YYINITIAL> [^] { }

<IN_SLASH_COMMENT> \r|\n|\r\n { yybegin(YYINITIAL); return ContentType.COMMENT_END; }
<IN_SLASH_COMMENT> [^] { }

<IN_STAR_COMMENT> \*\/ { yybegin(YYINITIAL); return ContentType.COMMENT_END; }
<IN_STAR_COMMENT> [^] { }

<IN_CHAR> [^']|\\\\|\\' { }
<IN_CHAR> ' { yybegin(YYINITIAL); }

<IN_STRING> [^\"]|\\\\|\\\" { }
<IN_STRING> \" { yybegin(YYINITIAL); }