# Searle Script
basic scripting language designed for ChatBots

to Compile
```
mvn install
```
to Run (must compile first)
```
mvn exec:java
```
If typing a script in by hand, send an EOF when finished by pressing CTRL+D

example apps are in
```
src/main/resources/
```
For real world examples check out the [Snippet Archive](https://github.com/midoricorp/snippets)

## Language Syntax

**Note:** the language ignores white space
\# can be used to put comments in code. comments get discarded by the parser and will not appear in re-generated code

### Operators (In order of Precedence)
* (*expression*) - evaluate *expression* first, changes order of operations
* `->`  - get element from a JSON Map - can also be on the left side of an assign to modify the JSON map
* [*expression*] - get index from JSON array specified by *expression* - can also be on the left side of an assign to modify the JSON array
* function *param* - all functions take one parameter, if *param* is an expression it should be surrounded by ()
* `++` - Post Increment
* `--` - Post Decrement
* `!` - Not operator
* `=~` - Binding Operator - Used for binding a regex to a string (substitution or matching)
* `*` - Multiply
* `/` - Divide
* `%` - Modulo
* `+` - Add
* `-` - Subtract
* `.` - String Concatenation
* `sizeof` - returns sizeof an array, 0 if not an array
* `keys` - returns the keys of a map as an array, empty array if not a map
* `>` - Greater Than
* `>=` - Greater Than or Equals
* `<` - Less Than
* `<=` - Less Than or Equals
* `==` - Equals
* `!=` - Not Equals
* `&` - Bitwise And
* `|` - Bitwise Or
* `&&` - Conditional And
* `||` - Conditional Or
* `=` - Assign
* `,` - Comma operatior - can make lists or return right param depending on context
 
### Data Types
All variables are stored as strings, and parsed as integers or JSON as required by operators.  Conditional operators consider "0" or "" to be false and everything else to be true

* "quoted string" - a string, can have \t\n\" as escape characters
* rand - returns a random positive integer
* *variable* - if declared with var, evaluates to it's curent value

### Flow Control
* if (*expression*) statement [ else statement ]  - executes command if *expression* is true, can have optional else block
* while (*expression*) statement - executes command while *expression* is true
* {[statement;]*} - statement block, allows for several statements to be treated as one statement

* continue; - return back to the beginning of a while loop
* break; - exit from the while loop
* return; - exit from a sub command

### Statements

statements should be terminated by ;

* var *token* [ = *expr*] - declares *token* as a variable, can also include an assignment
* get *url* - returns the body of the HTTP response of *url* as a string
* post( *url*[, *map* [, *use_json*]]) - sends a post to *url*, map is a json map, by default will be converted into a form urlencoded string.  If *use_json* is present and set to 1, then the map will be sent as a json object.
* *expression* - expressions such as i++;
* sub *token* command - declares *token* as a function


### Functions

Functions are declared by the "sub" command.  Parameters are passed in the _ variable.  Functions act as stand-alone subroutines and cannot access variables declared outside of the function.

### Regular Expressions
the right hand operator of =~ is a string that can take multiple forms
* "string" - simple substring match, returns true if there is a match, false otherwise
* "/regex/options" - regular expression matching.  Matched character groups stored in an array in _ . Returns true if a match was found and _ has been set.   Options can be as follows,
** i - case insensitive match
** s - dot all .  matches any character, including a line terminator
** m - multiline  the expressions ^ and $ match just after or just before, respectively, a line terminator or the end of the input sequence
* "/regex/replace/options" - replace everything matched by regex with replace

### Output

There are two available output streams TEXT and HTML.  All scripts should have a TEXT output and can also have an optional HTML output.  Output is done with the print command which has the following syntax
* print [STREAM] msg

If STREAM is omitted it defauts to TEXT

### Built-in Functions

There are some functions built directly into the language
* get url - sends an http request to the given url (with Accept json), returns the response.
* url_encode - performs url escaping, usefull when trying to build urls for get
* html_encode - performs xhtml escaping, useful when you want to make sure no illegal characters are in the xhtml body you are building
* lc - converts string to lowercase
* uc - converts string to uppercase

* split( string, [expression] ) - splits a string based on the regex, if regex is omitted the default is "\\s+"
* join( delimiter, [list | arg ]+ ) - will join all elements of all specifed lists and arguments with the specified delimiter

* push( list, [arg]+ ) - adds each argument to the end of the list, returns the list
* unshift( list, [args]+ ) - adds the list of args to the font of the list, the args keep in the same order that they apper in unshift() (not reversed), returns the list

* shift( list ) - removes the first element from the list and returns it
* pop( list ) - removes the last element from the list and returns it

* gmtime([timestamp]) - returns a map with information of the date specified by unix epoch timestamp (seconds since 1970).  If timestap is omitted the current time is used.

The following information is returned:

Key | Meaning
----|--------
YEAR|Year
MONTH|1-12 the month number
DAY|the day of the month (1-31)
HOUR|the hour in 12 hour format (1-12)
AM_PM|has the value AM or PM
HOUR_24|the hour in 24 hour format (0-23)
MINUTE|the minute of the hour
SECOND|the seconds of the minute
WEEK_OF_YEAR|the current week of the year (1-52)
DAY_OF_WEEK|Day of the week 1=Sunday to 7=Saturday


