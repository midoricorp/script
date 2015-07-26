# Searle Script
basic scripting language designed for ChatBots

to Run
```
mvn exec:java
```
example apps are in
```
src/main/resources/
```

##Language Syntax

**Note:** the language ignores white space

###Operators (In order of Precedence)
* (*expression*) - evaluate *expression* first, changes order of operations
* ->  - get element from a JSON Map - can also be on the left side of an assign to modify the JSON map
* [*expression*] - get index from JSON array specified by *expression* - can also be on the left side of an assign to modify the JSON array
* function *param* - all functions take one parameter, if *param* is an expression it should be surrounded by ()
* ++ - Post Increment
* -- - Post Decrement
* ! - Not operator
* =~ - Binding Operator - Used for binding a regex to a string (substitution or matching)
* * - Multiply
* / - Divide
* % - Modulo
* + - Add
* - - Subtract
* . - String Concatenation
* sizeof - returns sizeof an array, 0 if not an array
* keys - returns the keys of a map as an array, empty array if not a map
* > - Greater Than
* >= - Greater Than or Equals
* < - Less Than
* <= - Less Than or Equals
* == - Equals
* != - Not Equals
* & - Bitwise And
* | - Bitwise Or
* && - Conditional And
* || - Conditional Or
* = - Assign
* , - Comma operatior - can make lists or return right param depending on context
 
###Data Types
All variables are stored as strings, and parsed as integers or JSON as required by operators.  Conditional operators consider "0" or "" to be false and everything else to be true

* "quoted string" - a string, can have \t\n\" as escape characters
* rand - returns a random positive integer
* *variable* - if declared with var, evaluates to it's curent value


###Commands

commands should be terminated by ;


* var *token* [ = *expr*] - declares *token* as a variable, can also include an assignment
* {command;*} - command block, allows for several commands to be treated as one command
* if (*expression*) command [ else command ]  - executes command if *expression* is true, can have optional else block
* while (*expression*) command - executes command while *expression* is true
* get *url* - returns the body of the HTTP response of *url* as a string
* *expression* - expressions such as i++;
* sub *token* command - declares *token* as a function


###Functions

Functions are declared by the "sub" command.  Parameters are passed in the _ variable.  Functions act as stand-alone subroutines and cannot access variables declared outside of the function.

### Regular Expressions
the right hand operator of =~ is a string that can take multiple forms
* "string" - simple substring match, returns true if there is a match, false otherwise
* "/regex/options" - regular expression matching.  Matched character groups stored in an array in _ . Returns true if a match was found and _ has been set.   Options can be as follows,
** i - case insensitive match
** s - dot all .  matches any character, including a line terminator
** m - multiline  the expressions ^ and $ match just after or just before, respectively, a line terminator or the end of the input sequence
* "/regex/replace/options" - replace everything matched by regex with replace

###Output

There are two available output streams TEXT and HTML.  All scripts should have a TEXT output and can also have an optional HTML output.  Output is done with the print command which has the following syntax
* print [STREAM] msg

If STREAM is omitted it defauts to TEXT

###Built-in Functions

There are some functions built directly into the language
* get url - sends an http request to the given url (with Accept json), returns the response.
* url_encode - performs url escaping, usefull when trying to build urls for get
* html_encode - performs xhtml escaping, useful when you want to make sure no illegal characters are in the xhtml body you are building
