grammar SimpleSearchFilter;

simpleFilter: expression ( '&' expression )*;

expression: filter | sort | page | include;

filter: 'filter' '[' propertyName ']'( '[' comparison ']' )? '=' attributeValue (',' attributeValue)*;

sort: 'sort' '=' sortPropertyName ( ',' sortPropertyName )*;

page: 'page' '[' ( 'limit' | 'offset' ) ']' '=' pageValue;

include: 'include' '=' propertyName ( ',' propertyName )*;

comparison: 'EQ' | 'NEQ' | 'GT' | 'LT';

propertyName: (LETTERS|UNDERSCORE)+ (LETTERS | INT |
UNDERSCORE | SYMBOLS)*;

// sort property can start with a dash to indicate descending
sortPropertyName: (DASH)? (LETTERS|UNDERSCORE)+ (LETTERS | INT |
UNDERSCORE | SYMBOLS)*;

attributeValue: (
  LETTERS
  | INT
  | UNDERSCORE
  | DASH
  | SYMBOLS
  | SPACE
  | FORWARD_SLASH)+;

pageValue: INT;

// lexer rules in order
INT: [0-9]+;
LETTERS: [a-zA-Z]+;
UNDERSCORE: [_];
DASH: [-];
SYMBOLS: [.];
FORWARD_SLASH: [/];
SPACE: [ ];

WS: [\t\r\n]+ -> skip; // Skip whitespace
