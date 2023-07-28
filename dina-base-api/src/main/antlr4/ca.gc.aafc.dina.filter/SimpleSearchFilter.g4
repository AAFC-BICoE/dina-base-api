grammar SimpleSearchFilter;

expr: filterExpression;

// Examples:
// filter[name]=a name&filter[theDate][GT]=2015-10-01
// sort=name,-shortName
// page[offset]=0&page[limit]=10
// include=author.name
filterExpression
  : 'filter[' filterAttribute ']' '[' filterOp ']=' filterValue (','filterValue)* ('&' filterExpression)*
  ;

filterAttribute: TEXT;
filterOp: EQ | NEQ;
filterValue: TEXT;

// lexer rule in order
EQ: 'EQ';
NEQ: 'NEQ';
//NUMBER
//VAR_NAME: [a-zA-Z_][a-zA-Z0-9_]*;
TEXT: [a-zA-Z0-9_]*;
WS: [ \n\t\r]+ -> skip;
