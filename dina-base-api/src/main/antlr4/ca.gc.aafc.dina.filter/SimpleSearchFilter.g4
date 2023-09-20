grammar SimpleSearchFilter;

expr: simpleSimple;

simpleSimple: (filterExpression
  | sortExpression
  | pageExpression
  | includeExpression)*;

// Examples:
// filter[name]=a name&filter[theDate][GT]=2015-10-01
// sort=name,-shortName
// page[offset]=0&page[limit]=10
// include=author.name
filterExpression
  : ('&')* 'filter[' attributeValue ']' '[' filterOp ']='
  filterValue (','filterValue)*
  ;
sortExpression
  : ('&')* 'sort='attributeValue (','attributeValue)*
  ;
pageExpression
  : ('&')* 'page[' pageOp ']='NUMBER
  ;
includeExpression
  : ('&')* 'include='attributeValue (','attributeValue)*
  ;

attributeValue: TEXT;
filterOp: EQ | NEQ;
pageOp: LIMIT | OFFSET;
filterValue: NUMBER | TEXT;

// lexer rule in order
EQ: 'EQ';
NEQ: 'NEQ';
LIMIT: 'limit';
OFFSET: 'offset';
NUMBER: [0-9]+;
TEXT: [a-zA-Z0-9_\-.]+;
WS: [ \n\t\r]+ -> skip;
