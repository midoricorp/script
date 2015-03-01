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

		// if whe have pending pushedBack tokens
		// retun them first

		if (!tokenStack.empty()) {
			return tokenStack.pop();
		}

		int input = 0;

		try {
			while ((input = pr.read()) != -1) {
				// end of stream
				if (input == -1) {
					break;
				}

				char c = (char)input;

				if (skipWhiteSpace && 
					(c == ' ' || c == '\t' || c == '\n')) {

					if ( c == '\n' ) {
						lineNo++;
					}
					continue;
				}


				skipWhiteSpace = false;

				if (inQuotes && c != '\"') {
					sb.append(c);
					continue;
				} else if (inQuotes) {
					// end quote hit, return the token
					sb.append(c);
					return sb.toString();
				}

				if (!inQuotes && c == '\"') {
					inQuotes = true;
					sb.append(c);
					continue;
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


				// control chars end previous token & start a new one

				if (c == '(' || c == ')' || c == '{' || c == '}'
						|| c == '+' || c == '-' || c == '*' 
						|| c == '/' || c == '%' || c == '='
						|| c == ';' || c == '<' || c == '>' 
						|| c == '.' ) {
					// no previous token so return the special char
					if (sb.length() == 0) {
						sb.append(c);

						// some control chars can be 2 chars long
						if (c == '=' || c == '+' || c == '-') {

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
					|| (c >= '0' && c <= '9')) {
					
					sb.append(c);
				}
				else {
					if (sb.length() > 0) {
						return sb.toString();
					} else {
						// this char is invalid, puke & die
						return null;
					}
				}
			}
		} catch (IOException io) {
			throw new ScriptParseException("IOException on parsing");
		}


		// hit end of stream, return last token or null
		if (sb.length() > 0) {
			return sb.toString();
		} else {
			return null;
		}

	}
}

