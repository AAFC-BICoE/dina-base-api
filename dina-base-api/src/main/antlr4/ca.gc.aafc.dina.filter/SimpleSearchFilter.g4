grammar SimpleSearchFilter;

expr: filterExpression;

// We define methodCall to be a method name followed by an opening
// paren, an optional list of arguments, and a closing paren.
filterExpression
  : 'filter[' filterAttribute ']' '[' filterOp ']=' filterValue (','filterValue)* ('&' filterExpression)*
  ;

filterAttribute: TEXT;
filterOp: EQ | NEQ;
filterValue: TEXT;

// lexer rule in order
EQ: 'EQ';
NEQ: 'NEQ';
TEXT: [a-zA-Z_][a-zA-Z0-9_]*;
WS: [ \n\t\r]+ -> skip;
