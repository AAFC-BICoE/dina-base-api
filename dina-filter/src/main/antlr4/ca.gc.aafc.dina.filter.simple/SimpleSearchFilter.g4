grammar SimpleSearchFilter;

simpleFilter: expression ( '&' expression )*;

expression: filter | fiql | fields | optFields | sort | page | include;

filter: FILTER_KW '[' propertyName ']'( '[' comparison ']' )? '=' attributeValue (',' attributeValue)*;

fiql: 'fiql' '=' fiqlPart;

fields: FIELDS_KW '[' type ']' '=' propertyName ( ',' propertyName )*;

optFields: OPT_FIELDS_KW '[' type ']' '=' propertyName ( ',' propertyName )*;

sort: 'sort' '=' sortPropertyName ( ',' sortPropertyName )*;

page: 'page' '[' ( 'limit' | 'offset' ) ']' '=' pageValue;

include: 'include' '=' propertyName ( ',' propertyName )*;

comparison: 'EQ' | 'NEQ' | 'GT' | 'GOE' | 'LT' | 'LOE' | 'LIKE' | 'ILIKE' | 'IN';

namePart: (LETTERS|UNDERSCORE)+ (LETTERS | INT | UNDERSCORE | DOT |
  FIELDS_KW | FILTER_KW)*;

propertyName: namePart;
fieldName: namePart;
type: (LETTERS|UNDERSCORE)+ (LETTERS | INT | UNDERSCORE | DASH)*;
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
  | COLON
  | FILTER_KW
  | FIELDS_KW)+;

pageValue: INT;

// Keywords
FIELDS_KW: 'fields';
OPT_FIELDS_KW: 'optfields';
FILTER_KW: 'filter';

// lexer rules in order
QUOTED_STRING: '"' (~["])* '"';
INT: [0-9]+;
LETTERS: [a-zA-Z]+;
UNDERSCORE: [_];
DASH: '-';
FORWARD_SLASH: [/];
SPACE: [ ];
PARENTHESIS: [()];
DOT: '.';
PERCENTAGE: '%';
ASTERISK: '*';
COLON: ':';
COMMA: ',';
SEMI: ';';
EXCL: '!';
EQUALS: '=';

WS: [\t\r\n]+ -> skip; // Skip whitespace
