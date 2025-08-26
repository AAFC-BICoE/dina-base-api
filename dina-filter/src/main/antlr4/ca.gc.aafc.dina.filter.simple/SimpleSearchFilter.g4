grammar SimpleSearchFilter;

simpleFilter: expression ( '&' expression )*;

expression: filter | fiql | fields | optFields | sort | page | include;

filter: 'filter' '[' propertyName ']'( '[' comparison ']' )? '=' attributeValue (',' attributeValue)*;

fiql: 'fiql' '=' fiqlPart;

fields: 'fields' '[' type ']' '=' propertyName ( ',' propertyName )*;

optFields: 'optfields' '[' type ']' '=' propertyName ( ',' propertyName )*;

sort: 'sort' '=' sortPropertyName ( ',' sortPropertyName )*;

page: 'page' '[' ( 'limit' | 'offset' ) ']' '=' pageValue;

include: 'include' '=' propertyName ( ',' propertyName )*;

comparison: 'EQ' | 'NEQ' | 'GT' | 'LT' | 'LIKE' | 'ILIKE' | 'IN';

namePart: (LETTERS|UNDERSCORE)+ (LETTERS | INT | UNDERSCORE | DOT)*;

propertyName: namePart;
fieldName: namePart;
type: (LETTERS|UNDERSCORE)+ (LETTERS | INT | UNDERSCORE)*;
fiqlPart: (COMMA | SEMI | PARENTHESIS | attributeAcceptedValue | EXCL | EQUALS | ASTERISK)+;

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
PARENTHESIS: [()];
PERCENTAGE: '%';
ASTERISK: '*';
COLON: ':';
COMMA: ',';
SEMI: ';';
EXCL: '!';
EQUALS: '=';

WS: [\t\r\n]+ -> skip; // Skip whitespace
