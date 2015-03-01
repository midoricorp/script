package com.sipstacks.script;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PushbackReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Stack;
import java.util.Random;

public class Script{
  

	Hashtable <String, String> symbolTable = new Hashtable<String,String>();
	ScriptScanner scanner;
	Random random;

	int loopLimit;

	public Script(Reader in) {
		Hashtable <String, String> symbolTable = new Hashtable<String,String>();
		scanner = new ScriptScanner(in);
		random = new Random();
		loopLimit = -1; // default no limit
	}

	public void setLoopLimit(int limit) {
		this.loopLimit = limit;
	}


	private interface Command {
		public String exec() throws ScriptParseException;

	}



	private class Expression implements Command {
		Operation op;

		public String exec() throws ScriptParseException {
			// allow expression to be just a ;
			if (op != null) {
				op.eval();
			}
			return "";
		}

		public Expression() throws ScriptParseException {
			  ArrayList<Operation> ops = new ArrayList<Operation>();
			  String input = null;
			  while((input=scanner.getToken())!=null){
				  //System.out.println("Got token '"+input+"'");
				  if (input.equals(";")) {
					Operation  op = getOperation(ops);
					this.op = op;
					return;
				  }
				  try {
				  	ops.add(tokenToOp(input));
				  } catch (ScriptParseException spe) {
					  throw new ScriptParseException("Expression: " + spe.getMessage(), scanner);
				  }


			  }

			  throw new ScriptParseException("Expression: hit end while expecting ;", scanner);

		}

	}

	private class Var implements Command {
		String name;
		Operation op;

		public String exec() throws ScriptParseException {
			if (op == null) {
				symbolTable.put(name, "0");
			} else {
				symbolTable.put(name, op.eval());
			}
			return "";
		}

		public Var() throws ScriptParseException {
			  ArrayList<Operation> ops = new ArrayList<Operation>();
			  String input = null;
			  String name = null;

			  input = scanner.getToken();

			  if (input == null) {
				  throw new ScriptParseException("Var: unexpected EOF before varname", scanner);
			  }

			  this.name = input;

			  input = scanner.getToken();

			  if (input == null) {
				  throw new ScriptParseException("Var: unexpected EOF before ;", scanner);
			  }


			  if (input.equals("=")) {
				  while((input=scanner.getToken())!=null){
					  //System.out.println("Got token '"+input+"'");
					  if (input.equals(";")) {
						Operation  op = getOperation(ops);

						if (op == null) {
							throw new ScriptParseException("Var: missing operation for assignment", scanner);
						}
						this.op = op;
						return;
					  }

					  try {
						  ops.add(tokenToOp(input));
					  } catch (ScriptParseException spe) {
						  throw new ScriptParseException("Var: " + spe.getMessage(), scanner);
					  }

				  }
			  } else if (!input.equals(";")) {
				  throw new ScriptParseException("Var: ; expceted", scanner);
			  }

		  }

	}

	private class If implements Command {
		Operation op;
		Command cmd;
		Command else_cmd;

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

		public If() throws ScriptParseException {

			Operation op = null;
			Command cmd = null;
			Command else_cmd = null;

			String token;
			token = scanner.getToken();

			if ( !token.equals("(") ) {
				throw new ScriptParseException("If: Expected '('", scanner);
			}

			ArrayList<Operation> ops = new ArrayList<Operation>();

			while (true) {
				token = scanner.getToken();
				if (token == null ) {
					throw new ScriptParseException("If: unexpected eof on condition", scanner);
				}

				if (token.equals(")")) {
					op = getOperation(ops);			
					if (op == null) {
						throw new ScriptParseException("If: missing op inside ()", scanner);
					}
					break;
				}
				try {
					ops.add(tokenToOp(token));
				} catch (ScriptParseException spe) {
					throw new ScriptParseException("If: " + spe.getMessage(), scanner);
				}
			}

			cmd = getCommand();
			if (cmd == null) {
				throw new ScriptParseException("If: missing command", scanner);
			}

			token = scanner.getToken();

			if ( token.equals("else") ) {
				else_cmd = getCommand();
			} else {
				scanner.pushBack(token);
			}
				

			this.op = op;
			this.cmd = cmd;
			this.else_cmd = else_cmd;
		}
	}

	private class While implements Command {
		Operation op;
		Command cmd;

		public String exec() throws ScriptParseException {
			StringBuffer sb = new StringBuffer();
			int counter = 0;
			while (Integer.parseInt(op.eval()) != 0) {
				if(loopLimit > 0 && counter > loopLimit) {
					throw new ScriptParseException("While: loop count exceeded. Limit=" + loopLimit + " Current=" + counter);
				}
				sb.append(cmd.exec());
				counter++;
			}
			return sb.toString();
		}

		public While() throws ScriptParseException {

			Operation op = null;
			Command cmd = null;

			String token;
			token = scanner.getToken();

			if ( !token.equals("(") ) {
				throw new ScriptParseException("While: Expected '('", scanner);
			}

			ArrayList<Operation> ops = new ArrayList<Operation>();

			while (true) {
				token = scanner.getToken();
				if (token == null ) {
					throw new ScriptParseException("While: unexpected eof on condition", scanner);
				}

				if (token.equals(")")) {
					op = getOperation(ops);			
					if (op == null) {
						throw new ScriptParseException("While: missing op inside ()", scanner);
					}
					break;
				}

				try {
					ops.add(tokenToOp(token));
				} catch (ScriptParseException spe) {
					throw new ScriptParseException("While: " + spe.getMessage(), scanner);
				}
			}

			cmd = getCommand();
			if (cmd == null) {
				throw new ScriptParseException("While: missing command", scanner);
			}

			this.op = op;
			this.cmd = cmd;
		}
	}

	private class CommandBlock implements Command {
		ArrayList<Command> commands;


		public String exec() throws ScriptParseException {
			StringBuffer sb = new StringBuffer();
			for (Command cmd : commands) {
				sb.append(cmd.exec());
			}
			return sb.toString();
		}

		public CommandBlock() throws ScriptParseException {
			ArrayList<Command> commands = new ArrayList<Command>();


			String token;
			token = scanner.getToken();

			if ( !token.equals("{") ) {
				throw new ScriptParseException("Block: Expected '{'", scanner);
			}


			while (true) {
				token = scanner.getToken();
				if (token == null ) {
					throw new ScriptParseException("Block: unexpected eof while expecting }", scanner);
				}

				if (token.equals("}")) {
					break;
				}
				scanner.pushBack(token);
				Command cmd = getCommand();
				commands.add(cmd);
			}

			this.commands = commands;
		}
	}

	private class Print implements Command {
		Operation op;

		public String exec() throws ScriptParseException {
			if (op != null) {
				return op.eval() + "\n";
			} else {
				return "\n";
			}
		}

		public Print() throws ScriptParseException {

			Operation op = null;

			ArrayList<Operation> ops = new ArrayList<Operation>();

			while (true) {
				String token = scanner.getToken();
				if (token == null ) {
					throw new ScriptParseException("Print: unexpected eof on condition", scanner);
				}

				if (token.equals(";")) {
					op = getOperation(ops);			
					break;
				}
				
				try {
					ops.add(tokenToOp(token));
				} catch (ScriptParseException spe) {
					throw new ScriptParseException("Print: " + spe.getMessage(), scanner);
				}
			}
			this.op = op;
		}
	}

	private interface Operation {
		public String eval() throws ScriptParseException;
	}

	private class LParen implements Operation {
		Operation inner;
		public String eval() throws ScriptParseException {
			if (inner != null) {
				return inner.eval();
			}

			// () is usless but legal
			return "";
		}
	}

	private class RParen implements Operation {
		public String eval() throws ScriptParseException {
			throw new ScriptParseException("Trying to eval a ). This shouldn't happen");
		}
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

	private class Increment extends PostfixOperator {
		public String eval() throws ScriptParseException {
			super.eval();

			Variable var = (Variable)left;
			int v = Integer.parseInt(left.eval());
			var.assign(Integer.toString(v+1));
			return Integer.toString(v);
		}
	}	

	private class Decrement extends PostfixOperator {
		public String eval() throws ScriptParseException {
			super.eval();

			Variable var = (Variable)left;
			int v = Integer.parseInt(left.eval());
			var.assign(Integer.toString(v-1));
			return Integer.toString(v);
		}
	}	

	private class Add extends BinaryOperator {
		public String eval() throws ScriptParseException {
			super.eval();
			return Integer.toString(Integer.parseInt(left.eval()) + Integer.parseInt(right.eval()));
		}
	}

	private class Subtract extends BinaryOperator {
		public String eval() throws ScriptParseException {
			super.eval();
			return Integer.toString(Integer.parseInt(left.eval()) - Integer.parseInt(right.eval()));
		}
	}

	private class Multiply extends BinaryOperator {
		public String eval() throws ScriptParseException {
			super.eval();
			return Integer.toString(Integer.parseInt(left.eval()) * Integer.parseInt(right.eval()));
		}
	}

	private class Divide extends BinaryOperator {
		public String eval() throws ScriptParseException {
			super.eval();
			return Integer.toString(Integer.parseInt(left.eval()) / Integer.parseInt(right.eval()));
		}
	}

	private class Modulo extends BinaryOperator {
		public String eval() throws ScriptParseException {
			super.eval();
			return Integer.toString(Integer.parseInt(left.eval()) % Integer.parseInt(right.eval()));
		}
	}

	private class GreaterThan extends BinaryOperator {
		public String eval() throws ScriptParseException {
			super.eval();
			return Integer.parseInt(left.eval()) > Integer.parseInt(right.eval())?"1":"0";
		}
	}

	private class LessThan extends BinaryOperator {
		public String eval() throws ScriptParseException {
			super.eval();
			return Integer.parseInt(left.eval()) < Integer.parseInt(right.eval())?"1":"0";
		}
	}

	private class Equals extends BinaryOperator {
		public String eval() throws ScriptParseException {
			super.eval();
			return left.eval().equals(right.eval())?"1":"0";
		}
	}

	private class NotEquals extends BinaryOperator {
		public String eval() throws ScriptParseException {
			super.eval();
			return left.eval().equals(right.eval())?"0":"1";
		}
	}

	private class Not extends UnaryOperator {
		public String eval() throws ScriptParseException {
			super.eval();
			return Integer.parseInt(right.eval()) == 0?"1":"0";
		}
	}

	private class Concat extends BinaryOperator {
		public String eval() throws ScriptParseException {
			super.eval();
			return left.eval()+right.eval();
		}
	}

	private class Number implements Operation {
		String number;
		
		public Number(String number) {
			this.number = number;
		}

		public String eval() throws ScriptParseException {
			return number;
		}
	}

	private class Rand implements Operation {
		String number;
		
		public Rand() {
		}

		public String eval() throws ScriptParseException {
			return Integer.toString(random.nextInt(Integer.MAX_VALUE));
		}
	}

	private class StringLiteral implements Operation {
		String string;
		
		public StringLiteral(String string) {
			this.string = string.substring(1,string.length()-1);
		}

		public String eval() throws ScriptParseException {
			return string;
		}
	}


	private class Variable implements Operation {
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

	private class Assign extends BinaryOperator {
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

	private Operation tokenToOp(String input) throws ScriptParseException {
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
		  } else if(input.equals("(")) {
			return new LParen();
		  } else if(input.equals(")")) {
			return new RParen();
		  } else if(input.equals("rand")) {
		  	return new Rand();
		  } else if(input.startsWith("\"")) {
			  return new StringLiteral(input);
		  } else if ( input.matches("^[0-9]+")) {
			return new Number(input);
		  } else if ( input.matches("^[a-zA-Z][a-zA-Z0-9]*$")) {
			return new Variable(input);
		  }

		  throw new ScriptParseException("Invalid Operator: '"+input+"'");
	}
	
	private static Operation getOperation(List<Operation> ops) throws ScriptParseException {
		return getOperation(ops,0);
	}

	private static Operation getOperation(List<Operation> ops, int start) throws ScriptParseException {

		// sanity check

		if (ops.size() == 0) {
			return null;
		}
		
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

		// work your way backwards, only have the non-recursive part eval ()
		if(start == 0) {
			for (int i = ops.size()-1; i >= 0; i--) {
				Operation command = ops.get(i);
				if (command instanceof LParen) {
					((LParen)command).inner = getOperation(ops,i+1);
					ops.remove(i+1);
				}
			}
		}

		for (Class clazz : classes) {
			for (int i = start; i < ops.size(); i++) {
				Operation command = ops.get(i);

				// if start != 0 we are in a (), stop at the )

				if (start > 0 && command instanceof RParen) {
					break;
				}

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

		if (start > 0) {
			if(ops.size() > (start + 1)) {
				if (ops.get(start+1) instanceof RParen) {
					ops.remove(start+1);
					return ops.get(start);
				}
			}

			String list = "";
			for (Operation op : ops.subList(start, ops.size())) {
				list += " " + op.getClass().getName();
			}
			throw new ScriptParseException("Ops: incomplete reduction in () processing start=" + start + " ops.size=" +(ops.size()-start) + "(" + list + ")");

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



	private Command getCommand() throws ScriptParseException {
		String token = scanner.getToken();
		if (token == null) {
		  return null;
		}

		if (token.equals("while")) {
		  return new While();
		}
		if (token.equals("var")) {
		  return new Var();
		}
		if (token.equals("if")) {
		  return new If();
		}
		if (token.equals("print")) {
		  return new Print();
		}
		if (token.equals("{")) {
		scanner.pushBack(token);
		return new CommandBlock();
		}

		//System.out.println("Pushing back token '" + token + "'");
		scanner.pushBack(token);
		return new Expression();
	}


	public String run() throws ScriptParseException {
		StringBuffer result = new StringBuffer();
		Command cmd = null;

		while((cmd = getCommand())!=null){
			result.append(cmd.exec());
		}
		return result.toString();
	}

}

