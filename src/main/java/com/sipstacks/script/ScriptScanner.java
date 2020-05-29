package com.sipstacks.script;
import java.io.PushbackReader;
import java.io.Reader;
import java.util.Stack;
import java.io.IOException;

class ScriptScanner {

	private PushbackReader pr;
	private Stack<String> tokenStack;
	private int lineNo;

	public ScriptScanner(Reader r) {
		pr = new PushbackReader(r, 10);
		tokenStack = new Stack<String>();
		lineNo = 0;
	}

	public void pushBack(String token) {
		tokenStack.push(token);
	}

	public String getLocation() {
		return "Near Line: " + lineNo;
	}

	public String getToken() throws ScriptParseException {
		StringBuffer sb = new StringBuffer();

		boolean skipWhiteSpace = true;
		boolean inQuotes = false;
		boolean inComment = false;

		// if whe have pending pushedBack tokens
		// retun them first

		if (!tokenStack.empty()) {
			return tokenStack.pop();
		}

		int input = 0;

		try {
			while ((input = pr.read()) != -1) {

				char c = (char)input;

				if (skipWhiteSpace && 
					(c == ' ' || c == '\t' || c == '\n' || c == '\r' || c == '\u00A0')) {

					if ( c == '\n' ) {
						lineNo++;
					}
					continue;
				}

				skipWhiteSpace = false;

				if (inComment) {
					if ( c == '\n') {
						skipWhiteSpace = true;
						inComment = false;
						lineNo++;
					}
					continue;
				}

				if (inQuotes && c != '\"' && c != '“' && c != '”')  {
					if(c == '\\') {
						input  = pr.read();
						// end of stream
						if (input == -1) {
							break;
						}
						c = (char)input;

						if (c == '\\') {
							sb.append('\\');
						} else if (c == '“') {
							sb.append('“');
						} else if (c == '”') {
							sb.append('”');
						} else if (c == '\"') {
							sb.append('\"');
						} else if (c == 'n') {
							sb.append('\n');
						} else if (c == '\t') {
							sb.append('\t');
						} else {
							throw new ScriptParseException("Unrecognized excape \\" + c, this);
						}
						continue;

					}
					sb.append(c);
					continue;
				} else if (inQuotes) {
					// end quote hit, return the token
					sb.append('"');
					return sb.toString();
				}

				if (!inQuotes && (c == '\"' || c == '“' || c == '”')) {
					inQuotes = true;
					sb.append('"');
					continue;
				}

				if (c == '#') {
					if(sb.length() > 0) {
						//hit beginning of a comment process the current token
						//before eating the comment
						pr.unread(c);
						return sb.toString();
					} else {
						inComment = true;
						continue;
					}
				}

				// not and not equals

				if (c == '!') {
					// no previous token so return the special char
					if (sb.length() == 0) {
						sb.append(c);
						if ((input = pr.read()) != -1) {
							char c2 = (char)input;
							if (c2 == '=') {
								sb.append(c2);
							}
							else {
								// not a double char
								pr.unread(input);
							}
						 }
						 return sb.toString();
					}

					// we still have a token that needs to be returned
					// pushback and return that
					pr.unread(input);
					return sb.toString();
				}

				// - and -> 

				if (c == '-') {
					// no previous token so return the special char
					if (sb.length() == 0) {
						sb.append(c);
						if ((input = pr.read()) != -1) {
							char c2 = (char)input;
							if (c2 == '>') {
								sb.append(c2);
							}
							else if (c2 == '-') {
								sb.append(c2);
							}
							else {
								// not a double char
								pr.unread(input);
							}
						 }
						 return sb.toString();
					}

					// we still have a token that needs to be returned
					// pushback and return that
					pr.unread(input);
					return sb.toString();
				}

				// =, ==, and =~ 

				if (c == '=') {
					// no previous token so return the special char
					if (sb.length() == 0) {
						sb.append(c);
						if ((input = pr.read()) != -1) {
							char c2 = (char)input;
							if (c2 == '=') {
								sb.append(c2);
							}
							else if (c2 == '~') {
								sb.append(c2);
							}
							else {
								// not a double char
								pr.unread(input);
							}
						 }
						 return sb.toString();
					}

					// we still have a token that needs to be returned
					// pushback and return that
					pr.unread(input);
					return sb.toString();
				}

				// <, >, <=, >=

				if (c == '<' || c== '>') {
					// no previous token so return the special char
					if (sb.length() == 0) {
						sb.append(c);
						if ((input = pr.read()) != -1) {
							char c2 = (char)input;
							if (c2 == '=') {
								sb.append(c2);
							}
							else {
								// not a double char
								pr.unread(input);
							}
						 }
						 return sb.toString();
					}

					// we still have a token that needs to be returned
					// pushback and return that
					pr.unread(input);
					return sb.toString();
				}



				// control chars end previous token & start a new one

				if (c == '(' || c == ')' || c == '{' || c == '}'
						|| c == '+' || c == '*' || c == '&' || c == '|'
						|| c == '/' || c == '%' || c == ';'  
						|| c == ',' || c == '.' || c == '[' || c == ']' ) {
					// no previous token so return the special char
					if (sb.length() == 0) {
						sb.append(c);

						// some control chars can be 2 chars long
						if (c == '=' || c == '+' || c == '-'
								|| c == '&' || c == '|') {

							if ((input = pr.read()) != -1) {
								char c2 = (char)input;
								if (c == c2) {
									sb.append(c2);
								}
								else {
									// not a double char
									pr.unread(input);
								}
							}
						}
						return sb.toString();
					}

					// we still have a token that needs to be returned
					// pushback and return that
					pr.unread(input);
					return sb.toString();
				}
				  
				// now we got words or numbers

				if ((c >= 'a' && c <= 'z')
					|| (c >= 'A' && c <= 'Z')
					|| (c >= '0' && c <= '9')
					|| c == '_') {
					
					sb.append(c);
				}
				else {
					if (sb.length() > 0) {
						return sb.toString();
					} else {
						// this char is invalid, puke & die
						System.err.println("puking on " + c);
						return null;
					}
				}
			}
		} catch (IOException io) {
			throw new ScriptParseException("IOException on parsing", this);
		}


		// hit end of stream, return last token or null
		if (sb.length() > 0) {
			return sb.toString();
		} else {
			return null;
		}

	}
}

