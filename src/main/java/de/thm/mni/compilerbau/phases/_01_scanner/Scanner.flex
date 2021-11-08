package de.thm.mni.compilerbau.phases._01_scanner;

import de.thm.mni.compilerbau.utils.SplError;
import de.thm.mni.compilerbau.phases._02_03_parser.Sym;
import de.thm.mni.compilerbau.absyn.Position;
import de.thm.mni.compilerbau.table.Identifier;
import de.thm.mni.compilerbau.CommandLineOptions;
import java_cup.runtime.*;

%%


%class Scanner
%public
%line
%column
%cup
%eofval{
    return new java_cup.runtime.Symbol(Sym.EOF, yyline + 1, yycolumn + 1);   //This needs to be specified when using a custom sym class name
%eofval}

%{
    public CommandLineOptions options = null;
  
    private Symbol symbol(int type) {
      return new Symbol(type, yyline + 1, yycolumn + 1);
    }

    private Symbol symbol(int type, Object value) {
      return new Symbol(type, yyline + 1, yycolumn + 1, value);
    }
%}

%%

// TODO (assignment 1): The regular expressions for all tokens need to be defined here.


else {return symbol(Sym.ELSE);}
if {return symbol(Sym.IF);}
of {return symbol(Sym.OF);}
type {return symbol(Sym.TYPE);}
proc {return symbol(Sym.PROC);}
array {return symbol(Sym.ARRAY);}
var {return symbol(Sym.VAR);}

\< {return symbol(Sym.LT);}
\# {return symbol(Sym.NE);}
\:\= {return symbol(Sym.ASGN);}
\+ {return symbol(Sym.PLUS);}
\/ {return symbol(Sym.SLASH);}
\* {return symbol(Sym.STAR);}
\> {return symbol(Sym.GT);}
\<\= {return symbol(Sym.LE);}
\- {return symbol(Sym.MINUS);}
\>\= {return symbol(Sym.GE);}
\= {return symbol(Sym.EQ);}
\( { return symbol(Sym.LPAREN); }
\) { return symbol(Sym.RPAREN); }
\[ { return symbol(Sym.LBRACK); }
\] { return symbol(Sym.RBRACK); }
\{ { return symbol(Sym.LCURL); }
\} { return symbol(Sym.RCURL); }
\: { return symbol(Sym.COLON); }
\; { return symbol(Sym.SEMIC); }
[ \r\t\n] {}
\/\/.* { }
[a-zA-Z_][a-zA-Z0-9_]* { return symbol(Sym.IDENT, new Identifer (yytext())) ;}
[0-9]+ { return symbol(Sym.INTLIT, Integer.parseInt(yytext())) ;}


