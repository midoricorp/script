package com.sipstacks.script;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PushbackReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Stack;

public class Script{
  

	static Hashtable <String, String> symbolTable = new Hashtable<String,String>();

	private static class ScriptParseException extends Exception {
		ScriptScanner ss;
		public ScriptParseException(String exe) {
			super(exe);
			ss = null;
		}

		public ScriptParseException(String exe, ScriptScanner ss) {
			super(exe);
			this.ss = ss;
		}

		public String getMessage() {
			String msg = super.getMessage();

			if (ss != null) {
				msg = ss.getLocation() + "\n" + msg;
			}
			return msg;
		}
	}

	private interface Command {
		public String exec() throws ScriptParseException;

	}



	private static class Expression implements Command {
		Operation op;

		public Expression(Operation op) {
			this.op = op;
		}

		public String exec() throws ScriptParseException {
			op.eval();
			return "";
		}

		public static Command parse(ScriptScanner sr) throws ScriptParseException {
			  ArrayList<Operation> ops = new ArrayList<Operation>();
			  String input = null;
			  while((input=sr.getToken())!=null){
				  //System.out.println("Got token '"+input+"'");
				  if (input.equals(";")) {
					Operation  op = getOperation(ops);
					return new Expression(op);
				  }
				  ops.add(tokenToOp(input));


			  }

			  throw new ScriptParseException("Expression: hit end while expecting ;", sr);

		  }

	}

	private static class Var implements Command {
		String name;
		Operation op;

		public Var(String name, Operation op) {
			this.name = name;
			this.op = op;
		}

		public String exec() throws ScriptParseException {
			if (op == null) {
				symbolTable.put(name, "0");
			} else {
				symbolTable.put(name, op.eval());
			}
			return "";
		}

		public static Command parse(ScriptScanner sr) throws ScriptParseException {
			  ArrayList<Operation> ops = new ArrayList<Operation>();
			  String input = null;
			  String name = null;

			  input = sr.getToken();

			  if (input == null) {
				  throw new ScriptParseException("Var: unexpected EOF before varname", sr);
			  }

			  name = input;

			  input = sr.getToken();

			  if (input == null) {
				  throw new ScriptParseException("Var: unexpected EOF before ;", sr);
			  }


			  if (input.equals("=")) {
				  while((input=sr.getToken())!=null){
					  //System.out.println("Got token '"+input+"'");
					  if (input.equals(";")) {
						Operation  op = getOperation(ops);
						return new Var(name, op);
					  }
					  ops.add(tokenToOp(input));


				  }
			  } else if (!input.equals(";")) {
				  throw new ScriptParseException("Var: ; expceted", sr);
			  }

			  return new Var(name,null);

		  }

	}

	private static class If implements Command {
		Operation op;
		Command cmd;
		Command else_cmd;

		public If(Operation op, Command cmd, Command else_cmd) {
			this.op = op;
			this.cmd = cmd;
			this.else_cmd = else_cmd;
		}

		public String exec() throws ScriptParseException {
			StringBuffer sb = new StringBuffer();
			if (Integer.parseInt(op.eval()) != 0) {
				sb.append(cmd.exec());
			}
			else if (else_cmd != null) {
				sb.append(else_cmd.exec());
			}

			return sb.toString();
		}

		public static Command parse(ScriptScanner sr) throws ScriptParseException {

			Operation op = null;
			Command cmd = null;
			Command else_cmd = null;

			String token;
			token = sr.getToken();

			if ( !token.equals("(") ) {
				throw new ScriptParseException("If: Expected '('", sr);
			}

			ArrayList<Operation> ops = new ArrayList<Operation>();

			while (true) {
				token = sr.getToken();
				if (token == null ) {
					throw new ScriptParseException("If: unexpected eof on condition", sr);
				}

				if (token.equals(")")) {
					op = getOperation(ops);			
					break;
				}
				ops.add(tokenToOp(token));
			}

			cmd = getCommand(sr);
			if (cmd == null) {
				throw new ScriptParseException("If: missing command", sr);
			}

			token = sr.getToken();

			if ( token.equals("else") ) {
				else_cmd = getCommand(sr);
			} else {
				sr.pushBack(token);
			}
				

			return new If(op, cmd, else_cmd);
		}
	}

	private static class While implements Command {
		Operation op;
		Command cmd;

		public While(Operation op, Command cmd) {
			this.op = op;
			this.cmd = cmd;
		}

		public String exec() throws ScriptParseException {
			StringBuffer sb = new StringBuffer();
			while (Integer.parseInt(op.eval()) != 0) {
				sb.append(cmd.exec());
			}
			return sb.toString();
		}

		public static Command parse(ScriptScanner sr) throws ScriptParseException {

			Operation op = null;
			Command cmd = null;

			String token;
			token = sr.getToken();

			if ( !token.equals("(") ) {
				throw new ScriptParseException("While: Expected '('", sr);
			}

			ArrayList<Operation> ops = new ArrayList<Operation>();

			while (true) {
				token = sr.getToken();
				if (token == null ) {
					throw new ScriptParseException("While: unexpected eof on condition", sr);
				}

				if (token.equals(")")) {
					op = getOperation(ops);			
					break;
				}
				ops.add(tokenToOp(token));
			}

			cmd = getCommand(sr);
			if (cmd == null) {
				throw new ScriptParseException("While: missing command", sr);
			}

			return new While(op, cmd);
		}
	}

	private static class CommandBlock implements Command {
		ArrayList<Command> commands;

		public CommandBlock(ArrayList<Command> commands) {
			this.commands = commands;
		}

		public String exec() throws ScriptParseException {
			StringBuffer sb = new StringBuffer();
			for (Command cmd : commands) {
				sb.append(cmd.exec());
			}
			return sb.toString();
		}

		public static Command parse(ScriptScanner sr) throws ScriptParseException {
			ArrayList<Command> commands = new ArrayList<Command>();


			String token;
			token = sr.getToken();

			if ( !token.equals("{") ) {
				throw new ScriptParseException("Block: Expected '{'", sr);
			}


			while (true) {
				token = sr.getToken();
				if (token == null ) {
					throw new ScriptParseException("Block: unexpected eof while expecting }", sr);
				}

				if (token.equals("}")) {
					break;
				}
				sr.pushBack(token);
				Command cmd = getCommand(sr);
				commands.add(cmd);
			}


			return new CommandBlock(commands);
		}
	}

	private static class Print implements Command {
		Operation op;

		public Print(Operation op) {
			this.op = op;
		}

		public String exec() throws ScriptParseException {
			return op.eval() + "\n";
		}

		public static Command parse(ScriptScanner sr) throws ScriptParseException {

			Operation op = null;

			ArrayList<Operation> ops = new ArrayList<Operation>();

			while (true) {
				String token = sr.getToken();
				if (token == null ) {
					throw new ScriptParseException("Print: unexpected eof on condition", sr);
				}

				if (token.equals(";")) {
					op = getOperation(ops);			
					break;
				}
				ops.add(tokenToOp(token));
			}
			return new Print(op);
		}
	}

	private interface Operation {
		public String eval() throws ScriptParseException;
	}

	private static abstract class BinaryOperator implements Operation {
		Operation left;
		Operation right;

		public String eval() throws ScriptParseException {
			if (left == null) {
				throw new ScriptParseException(this.getClass().getName() + ": Missing left arg");
			}

			if (right == null) {
				throw new ScriptParseException(this.getClass().getName() +": Missing right arg");
			}

			// must override to have proper return val
			return null;
		}

	}

	private static abstract class UnaryOperator implements Operation {
		Operation right;

		public String eval() throws ScriptParseException {
			if (right == null) {
				throw new ScriptParseException(this.getClass().getName() +": Missing right arg");
			}

			// must override to have proper return val
			return null;
		}

	}

	private static abstract class PostfixOperator implements Operation {
		Operation left;

		public String eval() throws ScriptParseException {
			if (left == null) {
				throw new ScriptParseException(this.getClass().getName() +": Missing left arg");
			}

			if (!(left instanceof Variable)) {
				throw new ScriptParseException(this.getClass().getName() +": attempting to assign value to non-variable type");
			}

			// must override to have proper return val
			return null;
		}

	}

	private static class Increment extends PostfixOperator {
		public String eval() throws ScriptParseException {
			super.eval();

			Variable var = (Variable)left;
			int v = Integer.parseInt(left.eval());
			var.assign(Integer.toString(v+1));
			return Integer.toString(v);
		}
	}	

	private static class Decrement extends PostfixOperator {
		public String eval() throws ScriptParseException {
			super.eval();

			Variable var = (Variable)left;
			int v = Integer.parseInt(left.eval());
			var.assign(Integer.toString(v-1));
			return Integer.toString(v);
		}
	}	

	private static class Add extends BinaryOperator {
		public String eval() throws ScriptParseException {
			super.eval();
			return Integer.toString(Integer.parseInt(left.eval()) + Integer.parseInt(right.eval()));
		}
	}

	private static class Subtract extends BinaryOperator {
		public String eval() throws ScriptParseException {
			super.eval();
			return Integer.toString(Integer.parseInt(left.eval()) - Integer.parseInt(right.eval()));
		}
	}

	private static class Multiply extends BinaryOperator {
		public String eval() throws ScriptParseException {
			super.eval();
			return Integer.toString(Integer.parseInt(left.eval()) * Integer.parseInt(right.eval()));
		}
	}

	private static class Divide extends BinaryOperator {
		public String eval() throws ScriptParseException {
			super.eval();
			return Integer.toString(Integer.parseInt(left.eval()) / Integer.parseInt(right.eval()));
		}
	}

	private static class Modulo extends BinaryOperator {
		public String eval() throws ScriptParseException {
			super.eval();
			return Integer.toString(Integer.parseInt(left.eval()) % Integer.parseInt(right.eval()));
		}
	}

	private static class GreaterThan extends BinaryOperator {
		public String eval() throws ScriptParseException {
			super.eval();
			return Integer.parseInt(left.eval()) > Integer.parseInt(right.eval())?"1":"0";
		}
	}

	private static class LessThan extends BinaryOperator {
		public String eval() throws ScriptParseException {
			super.eval();
			return Integer.parseInt(left.eval()) < Integer.parseInt(right.eval())?"1":"0";
		}
	}

	private static class Equals extends BinaryOperator {
		public String eval() throws ScriptParseException {
			super.eval();
			return left.eval().equals(right.eval())?"1":"0";
		}
	}

	private static class NotEquals extends BinaryOperator {
		public String eval() throws ScriptParseException {
			super.eval();
			return left.eval().equals(right.eval())?"0":"1";
		}
	}

	private static class Not extends UnaryOperator {
		public String eval() throws ScriptParseException {
			super.eval();
			return Integer.parseInt(right.eval()) == 0?"1":"0";
		}
	}

	private static class Concat extends BinaryOperator {
		public String eval() throws ScriptParseException {
			super.eval();
			return left.eval()+right.eval();
		}
	}

	private static class Number implements Operation {
		String number;
		
		public Number(String number) {
			this.number = number;
		}

		public String eval() throws ScriptParseException {
			return number;
		}
	}

	private static class StringLiteral implements Operation {
		String string;
		
		public StringLiteral(String string) {
			this.string = string.substring(1,string.length()-1);
		}

		public String eval() throws ScriptParseException {
			return string;
		}
	}


	private static class Variable implements Operation {
		String name;

		public Variable(String name) {
			this.name = name;
		}

		public String eval() throws ScriptParseException {
			if (symbolTable.get(name) == null) {
				throw new ScriptParseException("Undefined variable: " + name);
			}
			return symbolTable.get(name);
		}

		public void assign(String value) throws ScriptParseException {
			if (symbolTable.get(name) == null) {
				throw new ScriptParseException("Undefined variable: " + name);
			}
			symbolTable.put(name, value);
		}
	}

	private static class Assign extends BinaryOperator {
		public String eval() throws ScriptParseException {
			super.eval();

			if (!(left instanceof Variable)) {
				throw new ScriptParseException("Assign: attempting to assign value to non-variable type");
			}
			Variable var = (Variable)left;
			String v = right.eval();
			var.assign(v);
			return v;
		}
	}

	private static Operation tokenToOp(String input) {
		  if (input.equals("+")) {
			return new Add();
		  } else if(input.equals("-")) {
			return new Subtract();
		  } else if(input.equals("*")) {
			return new Multiply();
		  } else if(input.equals("/")) {
			return new Divide();
		  } else if(input.equals("++")) {
			return new Increment();
		  } else if(input.equals("--")) {
			return new Decrement();
		  } else if(input.equals("%")) {
			return new Modulo();
		  } else if(input.equals(".")) {
			return new Concat();
		  } else if(input.equals("=")) {
			return new Assign();
		  } else if(input.equals("==")) {
			return new Equals();
		  } else if(input.equals("!=")) {
			return new NotEquals();
		  } else if(input.equals(">")) {
			return new GreaterThan();
		  } else if(input.equals("<")) {
			return new LessThan();
		  } else if(input.equals("!")) {
			return new Not();
		  } else if(input.startsWith("\"")) {
			  return new StringLiteral(input);
		  } else if ( input.matches("^[0-9]+")) {
			return new Number(input);
		  } else {
			return new Variable(input);
		  }
	}
	
	private static Operation getOperation(List<Operation> ops) throws ScriptParseException {

		// assignment order ! * / % + - . < > == != =
		// order based on http://docs.oracle.com/javase/tutorial/java/nutsandbolts/operators.html

		Class classes[] = {
			Increment.class, Decrement.class, // postfix
			Not.class, //unary
			Multiply.class, Divide.class, Modulo.class,  // multiplicative
			Add.class, Subtract.class, Concat.class, // additive
			LessThan.class, GreaterThan.class, // relational
	       		Equals.class, NotEquals.class, // equality
			Assign.class  // assignment 
		};


		for (Class clazz : classes) {
			for (int i = 0; i < ops.size(); i++) {
				Operation command = ops.get(i);
				if ( clazz.isInstance(command) ) {
					if (UnaryOperator.class.isAssignableFrom(command.getClass())) {
						UnaryOperator op = (UnaryOperator)command;
						op.right = ops.get(i+1);
						ops.remove(i+1);
					} else if (PostfixOperator.class.isAssignableFrom(command.getClass())) {
						PostfixOperator op = (PostfixOperator)command;
						op.left = ops.get(i-1);
						ops.remove(i-1);
						i--;
					} else if (BinaryOperator.class.isAssignableFrom(command.getClass())) {
						BinaryOperator op = (BinaryOperator)command;
						op.left = ops.get(i-1);
						op.right = ops.get(i+1);
						ops.remove(i+1);
						ops.remove(i-1);
						i--;
					}
				}
			}
		}


		if (ops.size() > 1 ) {
			String list = "";
			for (Operation op : ops) {
				list += " " + op.getClass().getName();
			}
			throw new ScriptParseException("Ops: incomplete reduction ops.size=" +ops.size() + "(" + list + ")");
		}




		return ops.get(0);


	}

	private static class ScriptScanner {

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



	private static Command getCommand(ScriptScanner sr) throws ScriptParseException {
		String token = sr.getToken();
		if (token == null) {
		  return null;
		}

		if (token.equals("while")) {
		  return While.parse(sr);
		}
		if (token.equals("var")) {
		  return Var.parse(sr);
		}
		if (token.equals("if")) {
		  return If.parse(sr);
		}
		if (token.equals("print")) {
		  return Print.parse(sr);
		}
		if (token.equals("{")) {
		sr.pushBack(token);
		return CommandBlock.parse(sr);
		}

		//System.out.println("Pushing back token '" + token + "'");
		sr.pushBack(token);
		return Expression.parse(sr);
	}

  public static void main(String args[]) {
	StringBuffer result = new StringBuffer();
	try{
		ScriptScanner sr = 
                      new ScriptScanner(new InputStreamReader(System.in));
 
 
		Command cmd = null;
		while((cmd = getCommand(sr))!=null){
			try {
				result.append(cmd.exec());
			} catch(ScriptParseException e) {
				System.out.println(e.getMessage());
				break;
			}
		}
 
	}catch(ScriptParseException spe){
		System.err.println(spe.getMessage());
	}	

	System.out.println(result.toString());
  }
}

