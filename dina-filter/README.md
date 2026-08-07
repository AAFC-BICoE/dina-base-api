# DINA Filter

A URL query string parsing library built with [ANTLR4](https://www.antlr.org/) for filtering, sorting, pagination, and field selection in REST APIs.
Designed to support JSON:API-style query parameters and FIQL (Feed Item Query Language) filtering.

## Features

- 🔍 **Flexible Filtering** - Support for multiple comparison operators (EQ, NEQ, GT, LT, LIKE, IN, etc.)
- 📊 **FIQL Support** - Advanced filtering using Feed Item Query Language
- 🎯 **Sparse Fieldsets** - Select specific fields to return in responses
- 📄 **Pagination** - Built-in limit/offset pagination support
- 🔗 **Relationship Inclusion** - Include related resources in responses
- ⬆️⬇️ **Sorting** - Multi-field sorting with ascending/descending order
- 🌍 **Unicode Support** - Full support for international characters
- 🚀 **Type-Safe Parsing** - ANTLR4-based grammar ensures valid query strings

## Maven Dependency

[![Maven Central](https://img.shields.io/maven-central/v/io.github.aafc-bicoe/dina-filter.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/io.github.aafc-bicoe/dina-filter/)

## Query String Syntax

### Filtering

#### Basic Filter Syntax

```
filter[attribute][operator]=value
```

**Supported Operators:**

| Operator | Description | Example |
|----------|-------------|---------|
| `EQ` | Equals | `filter[name][EQ]=John` |
| `NEQ` | Not equals | `filter[status][NEQ]=inactive` |
| `GT` | Greater than | `filter[age][GT]=18` |
| `GOE` | Greater or equal | `filter[price][GOE]=100` |
| `LT` | Less than | `filter[count][LT]=50` |
| `LOE` | Less or equal | `filter[score][LOE]=100` |
| `LIKE` | Pattern match | `filter[name][LIKE]=%John%` |
| `ILIKE` | Case-insensitive pattern | `filter[email][ILIKE]=%@example.com` |
| `IN` | In list | `filter[status][IN]=active,pending` |

#### Default Operator

If no operator is specified, `EQ` is used by default:

```
filter[name]=John  // Equivalent to filter[name][EQ]=John
```

#### Multiple Values (OR Logic)

Comma-separated values create an OR condition:

```
filter[status][EQ]=active,pending
// Matches: status == 'active' OR status == 'pending'
```

#### Multiple Filters (AND Logic)

Multiple filter parameters are combined with AND:

```
filter[status][EQ]=active&filter[age][GT]=18
// Matches: status == 'active' AND age > 18
```

#### Nested Properties

Use dot notation for nested properties:

```
filter[author.name][EQ]=John
filter[department.location.city][EQ]=Ottawa
```

#### Quoted Values

Use quotes for values with special characters:

```
filter[description][EQ]="Hello, World!"
```

### FIQL (Feed Item Query Language)

For complex queries, use FIQL syntax:

```
fiql=name==John*;age=gt=25
```

**FIQL Operators:**

| Operator | Description | Example |
|----------|-------------|---------|
| `==` | Equals | `name==John` |
| `!=` | Not equals | `status!=inactive` |
| `=lt=` | Less than | `age=lt=30` |
| `=le=` | Less or equal | `price=le=100` |
| `=gt=` | Greater than | `count=gt=5` |
| `=ge=` | Greater or equal | `score=ge=50` |

**FIQL Logical Operators:**

- `;` - AND operator
- `,` - OR operator
- `()` - Grouping

**FIQL Examples:**

```
# Simple equality
fiql=name==John

# Wildcard search
fiql=email==*@example.com

# Complex query with grouping
fiql=(status==active;age=gt=18),role==admin

# Multiple conditions
fiql=name==John*;(age=gt=25,department==IT)

# Quotes to amke sure reserved words are escaped
fiql="name==John*;(age=gt=25,department==IN)"
```

### Sorting

Sort by one or more fields:

```
sort=name              # Ascending by name
sort=-createdDate      # Descending by createdDate
sort=name,-age         # Multiple fields
```

Prefix with `-` for descending order.

### Pagination

```
page[limit]=10         # Number of results per page
page[offset]=20        # Starting position
```

### Sparse Fieldsets

Select specific fields to return:

```
fields[person]=name,email,age
fields[department]=name
```

### Optional Fields

Request optional fields that may not be included by default:

```
optfields[metadata]=md5,fileSize
```

### Including Related Resources

Include related resources in the response:

```
include=author
include=author.department
include=comments,author
```

## Limitations

- Maximum integer value: Standard Java `int` range
- Query string length: Limited by HTTP server configuration
- Nested property depth: No enforced limit, but consider performance

## Related Documentation

- [ANTLR4 Documentation](https://www.antlr.org/)
- [JSON:API Specification](https://jsonapi.org/)
- [FIQL Specification](https://tools.ietf.org/html/draft-nottingham-atompub-fiql-00)
