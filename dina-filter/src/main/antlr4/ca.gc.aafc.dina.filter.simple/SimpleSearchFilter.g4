grammar SimpleSearchFilter;

simpleFilter: expression ( '&' expression )*;

expression: filter | sort | page | include;

filter: 'filter' '[' propertyName ']'( '[' comparison ']' )? '=' attributeValue (',' attributeValue)*;

sort: 'sort' '=' sortPropertyName ( ',' sortPropertyName )*;

page: 'page' '[' ( 'limit' | 'offset' ) ']' '=' pageValue;

include: 'include' '=' propertyName ( ',' propertyName )*;

comparison: 'EQ' | 'NEQ' | 'GT' | 'LT' | 'LIKE' | 'ILIKE' | 'IN';

propertyName: (LETTERS|UNDERSCORE)+ (LETTERS | INT |
UNDERSCORE | DOT)*;

// sort property can start with a dash to indicate descending
sortPropertyName: (DASH)? (LETTERS|UNDERSCORE)+ (LETTERS | INT |
UNDERSCORE | DOT)*;

attributeValue: QUOTED_STRING | attributeAcceptedValue;

attributeAcceptedValue: (
  LETTERS
  | INT
  | UNDERSCORE
  | DASH
  | DOT
  | PERCENTAGE
  | SPACE
  | FORWARD_SLASH)+;

pageValue: INT;

// lexer rules in order
QUOTED_STRING: '"' (~["])* '"';
INT: [0-9]+;
LETTERS: [a-zA-Z]+;
UNDERSCORE: [_];
DASH: [-];
DOT: [.];
FORWARD_SLASH: [/];
SPACE: [ ];
PERCENTAGE: [%];
QUOTE: ["];

WS: [\t\r\n]+ -> skip; // Skip whitespace
