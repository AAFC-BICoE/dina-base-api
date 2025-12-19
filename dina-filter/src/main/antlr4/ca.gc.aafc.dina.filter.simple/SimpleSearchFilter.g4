grammar SimpleSearchFilter;

simpleFilter: expression ( '&' expression )*;

expression: filter | fiql | fields | optFields | sort | page | include;

filter: FILTER_KW '[' propertyName ']'( '[' comparison ']' )? '=' attributeValue (',' attributeValue)*;

fiql: FIQL_KW '=' fiqlPart;

fields: FIELDS_KW '[' type ']' '=' propertyName ( ',' propertyName )*;

optFields: OPT_FIELDS_KW '[' type ']' '=' propertyName ( ',' propertyName )*;

sort: SORT_KW '=' sortPropertyName ( ',' sortPropertyName )*;

page: PAGE_KW '[' ( 'limit' | 'offset' ) ']' '=' pageValue;

include: INCLUDE_KW '=' propertyName ( ',' propertyName )*;

comparison: 'EQ' | 'NEQ' | 'GT' | 'GOE' | 'LT' | 'LOE' | 'LIKE' | 'ILIKE' | 'IN';

namePart: (LETTERS|UNDERSCORE)+ (LETTERS | INT | UNDERSCORE | DOT |
  FIELDS_KW | FILTER_KW | SORT_KW | PAGE_KW | INCLUDE_KW)*;

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
  | FILTER_KW | FIELDS_KW | SORT_KW | PAGE_KW | INCLUDE_KW)+;

pageValue: INT;

// Keywords
FIELDS_KW: 'fields';
OPT_FIELDS_KW: 'optfields';
FILTER_KW: 'filter';
FIQL_KW: 'fiql';
SORT_KW: 'sort';
PAGE_KW: 'page';
INCLUDE_KW: 'include';

// lexer rules in order
QUOTED_STRING: '"' (~["])* '"';
INT: [0-9]+;
LETTERS: [a-zA-Z]+;
UNDERSCORE: [_];
FORWARD_SLASH: [/];
SPACE: [ ];
PARENTHESIS: [()];
DASH: '-';
DOT: '.';
PERCENTAGE: '%';
ASTERISK: '*';
COLON: ':';
COMMA: ',';
SEMI: ';';
EXCL: '!';
EQUALS: '=';

WS: [\t\r\n]+ -> skip; // Skip whitespace
