grammar SimpleSearchFilter;

simpleFilter: expression ( AMP expression )*;

expression: filter | filterGroup | fiql | fields | optFields | sort | page | include;

filter: FILTER_KW '[' propertyName ']'( '[' comparison ']' )? '=' attributeValue (',' attributeValue)*;

/**
 * Logical filter expression supporting:
 *  - OR (|)
 *  - AND (&)
 *  - nested grouping with parentheses
 *
 * AND has higher precedence than OR.
 */
filterGroup
: LPAREN filterOrExpression RPAREN
;

filterOrExpression
: filterAndExpression (PIPE filterAndExpression)*
;

filterAndExpression
: filterPrimary (AMP filterPrimary)*
;

filterPrimary
: filter
| LPAREN filterOrExpression RPAREN
;

fiql: FIQL_KW '=' fiqlPart;

fields: FIELDS_KW '[' type ']' '=' propertyName ( ',' propertyName )*;

optFields: OPT_FIELDS_KW '[' type ']' '=' propertyName ( ',' propertyName )*;

sort: SORT_KW '=' sortPropertyName ( ',' sortPropertyName )*;

page: PAGE_KW '[' ( 'limit' | 'offset' ) ']' '=' pageValue;

include: INCLUDE_KW '=' propertyName ( ',' propertyName )*;

comparison: 'EQ' | 'NEQ' | 'GT' | 'GOE' | 'LT' | 'LOE' | 'LIKE' | 'ILIKE' | 'IN';

propertyName: PROPERTY_REFERENCE;
type: PROPERTY_REFERENCE (DASH PROPERTY_REFERENCE)*;
fiqlPart: (COMMA | SEMI | LPAREN | RPAREN | attributeAcceptedValue | EXCL | EQUALS | ASTERISK | QUOTED_STRING)+;

// sort property can start with a dash to indicate descending
sortPropertyName: (DASH)? PROPERTY_REFERENCE;

attributeValue: QUOTED_STRING | attributeAcceptedValue;

attributeAcceptedValue: (
  PROPERTY_REFERENCE
  | UNICODE_NON_ASCII_LETTER
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
PROPERTY_REFERENCE
  : [a-zA-Z_][a-zA-Z0-9_.]*
  ;
INT: [0-9]+;
ASCII_LETTER: [a-zA-Z]+;
// Unicode letters except ASCII since ASCII already matched ASCII_LETTER
UNICODE_NON_ASCII_LETTER: [\p{L}];
UNDERSCORE: [_];
FORWARD_SLASH: [/];
SPACE: [ ];
AMP : '&';
PIPE : '|';
LPAREN : '(';
RPAREN : ')';
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
