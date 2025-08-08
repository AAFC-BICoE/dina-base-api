grammar SimpleSearchFilter;

simpleFilter: expression ( '&' expression )*;

expression: filter | fiql | fields | sort | page | include;

filter: 'filter' '[' propertyName ']'( '[' comparison ']' )? '=' attributeValue (',' attributeValue)*;

fiql: 'fiql' '=' fiqlPart;

fields: 'fields' '[' fieldName ']' '=' propertyName ( ',' propertyName )*;

sort: 'sort' '=' sortPropertyName ( ',' sortPropertyName )*;

page: 'page' '[' ( 'limit' | 'offset' ) ']' '=' pageValue;

include: 'include' '=' propertyName ( ',' propertyName )*;

comparison: 'EQ' | 'NEQ' | 'GT' | 'LT' | 'LIKE' | 'ILIKE' | 'IN';

namePart: (LETTERS|UNDERSCORE)+ (LETTERS | INT | UNDERSCORE | DOT)*;

propertyName: namePart;
fieldName: namePart;
fiqlPart: (COMMA | SEMI | attributeAcceptedValue | EQUALS | ASTERISK)+;

// sort property can start with a dash to indicate descending
sortPropertyName: (DASH)? namePart;

attributeValue: QUOTED_STRING | attributeAcceptedValue;

attributeAcceptedValue: (
  LETTERS
  | INT
  | UNDERSCORE
  | DASH
  | DOT
  | PERCENTAGE
  | SPACE
  | FORWARD_SLASH
  | COLON)+;

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
PARENTHESIS: [()];
ASTERISK: '*';
COLON: ':';
COMMA: ',';
SEMI: ';';
EQUALS: '=';

WS: [\t\r\n]+ -> skip; // Skip whitespace
