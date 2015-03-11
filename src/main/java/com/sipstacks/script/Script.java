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
import org.json.simple.*;

public class Script{
  

	Hashtable <String, String> symbolTable = new Hashtable<String,String>();
	Hashtable <String, Function> functionTable = new Hashtable<String,Function>();
	ScriptScanner scanner;
	Random random;

	int loopLimit;

	public Script(Reader in) {
		scanner = new ScriptScanner(in);
		random = new Random();
		loopLimit = -1; // default no limit
		addExternalFunction("get", new GetFunction());
	}

	public void setLoopLimit(int limit) {
		this.loopLimit = limit;
	}

	private class Expression implements Command {
		Operation op;

		public String exec(String arg) throws ScriptParseException {
			return exec();
		}
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

		public String exec(String arg) throws ScriptParseException {
			return exec();
		}
		public String exec() throws ScriptParseException {
			if (op == null) {
				symbolTable.put(name, "0");
			} else {
				symbolTable.put(name, op.eval().toString());
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

		public String exec(String arg) throws ScriptParseException {
			return exec();
		}
		public String exec() throws ScriptParseException {
			StringBuffer sb = new StringBuffer();
			if (Integer.parseInt(op.eval().toString()) != 0) {
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

			if (token != null ) {
				if ( token.equals("else") ) {
					else_cmd = getCommand();
				} else {
					scanner.pushBack(token);
				}
			}
				

			this.op = op;
			this.cmd = cmd;
			this.else_cmd = else_cmd;
		}
	}

	private class While implements Command {
		Operation op;
		Command cmd;

		private int totalCalls = 0;

		public String exec(String arg) throws ScriptParseException {
			return exec();
		}
		public String exec() throws ScriptParseException {
			StringBuffer sb = new StringBuffer();
			int counter = 0;
			while (Integer.parseInt(op.eval().toString()) != 0) {
				if(loopLimit > 0 && counter > loopLimit) {
					throw new ScriptParseException("While: loop count exceeded. Limit=" + loopLimit + " Current=" + counter);
				}
				if(loopLimit > 0 && totalCalls > loopLimit*loopLimit) {
					throw new ScriptParseException("While: nested loop count exceeded. Limit=" + (loopLimit*loopLimit) + " Current=" + totalCalls);
				}
				sb.append(cmd.exec());
				counter++;
				totalCalls++;
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


		public String exec(String arg) throws ScriptParseException {
			return exec();
		}
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

		public String exec(String arg) throws ScriptParseException {
			return exec();
		}
		public String exec() throws ScriptParseException {
			if (op != null) {
				return op.eval().toString() + "\n";
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

	private class LParen implements Operation {
		Operation inner;
		public Object eval() throws ScriptParseException {
			if (inner != null) {
				return inner.eval();
			}

			// () is usless but legal
			return "";
		}
	}

	private class RParen implements Operation {
		public Object eval() throws ScriptParseException {
			throw new ScriptParseException("Trying to eval a ). This shouldn't happen");
		}
	}

	private class LBracket extends BinaryOperator  {
		public Object eval() throws ScriptParseException {
			super.eval();
			Object obj=JSONValue.parse(left.eval().toString());
			if ( obj instanceof JSONArray) {
				return ((JSONArray)obj).get(Integer.parseInt(right.eval().toString())).toString();
			}
			throw new ScriptParseException("[] expected array as lval\nGot: " + left.eval().toString());
		}
	}

	private class RBracket implements Operation {
		public Object eval() throws ScriptParseException {
			throw new ScriptParseException("Trying to eval a ]. This shouldn't happen");
		}
	}

	private static abstract class BinaryOperator implements Operation {
		Operation left;
		Operation right;

		public Object eval() throws ScriptParseException {
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

	private static abstract class PostfixOperator implements Operation {
		Operation left;

		public Object eval() throws ScriptParseException {
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
		public Object eval() throws ScriptParseException {
			super.eval();

			Variable var = (Variable)left;
			int v = Integer.parseInt(left.eval().toString());
			var.assign(Integer.toString(v+1));
			return Integer.toString(v);
		}
	}	

	private class Decrement extends PostfixOperator {
		public Object eval() throws ScriptParseException {
			super.eval();

			Variable var = (Variable)left;
			int v = Integer.parseInt(left.eval().toString());
			var.assign(Integer.toString(v-1));
			return Integer.toString(v);
		}
	}	

	private class Add extends BinaryOperator {
		public Object eval() throws ScriptParseException {
			super.eval();
			return Integer.toString(Integer.parseInt(left.eval().toString()) + Integer.parseInt(right.eval().toString()));
		}
	}

	private class Subtract extends BinaryOperator {
		public Object eval() throws ScriptParseException {
			super.eval();
			return Integer.toString(Integer.parseInt(left.eval().toString()) - Integer.parseInt(right.eval().toString()));
		}
	}

	private class Multiply extends BinaryOperator {
		public Object eval() throws ScriptParseException {
			super.eval();
			return Integer.toString(Integer.parseInt(left.eval().toString()) * Integer.parseInt(right.eval().toString()));
		}
	}

	private class Divide extends BinaryOperator {
		public Object eval() throws ScriptParseException {
			super.eval();
			return Integer.toString(Integer.parseInt(left.eval().toString()) / Integer.parseInt(right.eval().toString()));
		}
	}

	private class Modulo extends BinaryOperator {
		public Object eval() throws ScriptParseException {
			super.eval();
			return Integer.toString(Integer.parseInt(left.eval().toString()) % Integer.parseInt(right.eval().toString()));
		}
	}

	private class GreaterThan extends BinaryOperator {
		public Object eval() throws ScriptParseException {
			super.eval();
			return Integer.parseInt(left.eval().toString()) > Integer.parseInt(right.eval().toString())?"1":"0";
		}
	}

	private class LessThan extends BinaryOperator {
		public Object eval() throws ScriptParseException {
			super.eval();
			return Integer.parseInt(left.eval().toString()) < Integer.parseInt(right.eval().toString())?"1":"0";
		}
	}

	private class Equals extends BinaryOperator {
		public Object eval() throws ScriptParseException {
			super.eval();
			return left.eval().toString().equals(right.eval().toString())?"1":"0";
		}
	}

	private class NotEquals extends BinaryOperator {
		public Object eval() throws ScriptParseException {
			super.eval();
			return left.eval().toString().equals(right.eval().toString())?"0":"1";
		}
	}

	private class Not extends UnaryOperator {
		public Object eval() throws ScriptParseException {
			super.eval();
			return Integer.parseInt(right.eval().toString()) == 0?"1":"0";
		}
	}

	private class Concat extends BinaryOperator {
		public Object eval() throws ScriptParseException {
			super.eval();
			return left.eval().toString()+right.eval().toString();
		}
	}

	private class Reference extends BinaryOperator {
		public Object eval() throws ScriptParseException {
			super.eval();
			Object obj=JSONValue.parse(left.eval().toString());
			if ( obj instanceof JSONObject) {
				String result = ((JSONObject)obj).get(right.eval().toString()).toString();
				return result;
			}
			throw new ScriptParseException("-> expected map as lval");
		}
	}

	private class Number implements Operation {
		String number;
		
		public Number(String number) {
			this.number = number;
		}

		public Object eval() throws ScriptParseException {
			return number;
		}
	}

	private class Rand implements Operation {
		String number;
		
		public Rand() {
		}

		public Object eval() throws ScriptParseException {
			return Integer.toString(random.nextInt(Integer.MAX_VALUE));
		}
	}

	private class StringLiteral implements Operation {
		String string;
		
		public StringLiteral(String string) {
			this.string = string.substring(1,string.length()-1);
		}

		public Object eval() throws ScriptParseException {
			return string;
		}
	}


	private class Variable implements Operation {
		String name;

		public Variable(String name) {
			this.name = name;
		}

		public Object eval() throws ScriptParseException {
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
		public Object eval() throws ScriptParseException {
			super.eval();

			if (!(left instanceof Variable)) {
				throw new ScriptParseException("Assign: attempting to assign value to non-variable type " + left.getClass().getName());
			}
			Variable var = (Variable)left;
			String v = right.eval().toString();
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
		  } else if(input.equals("->")) {
			return new Reference();
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
		  } else if(input.equals("[")) {
			return new LBracket();
		  } else if(input.equals("]")) {
			return new RBracket();
		  } else if(input.equals("rand")) {
		  	return new Rand();
		  } else if(input.startsWith("\"")) {
			  return new StringLiteral(input);
		  } else if ( input.matches("^[0-9]+")) {
			return new Number(input);
		  } else if ( input.matches("^[a-zA-Z][a-zA-Z0-9]*$")) {
			  // function names have precedents over variables
			  if (functionTable.containsKey(input)) {
				  Function f = functionTable.get(input);
			
				  return f.clone();
			  }
			return new Variable(input);
		  }

		  throw new ScriptParseException("Invalid Operator: '"+input+"'");
	}
	
	private static Operation getOperation(List<Operation> ops) throws ScriptParseException {
		return getOperation(ops,0,null);
	}

	private static class OperationSet
	{
		boolean leftToRight; 
		ArrayList<Class> operators;
		public OperationSet(boolean leftToRight) {
			this.leftToRight = leftToRight;
			this.operators = new ArrayList<Class>();
		}
	}

	static ArrayList<OperationSet>classes = new ArrayList<OperationSet>();

	static {
		OperationSet op = new OperationSet(true);
		op.operators.add(Reference.class);
		op.operators.add(LBracket.class);
		op.operators.add(Function.class);
		classes.add(op);

		op = new OperationSet(false);
		op.operators.add(Increment.class);
		op.operators.add(Decrement.class);
		classes.add(op);

		op = new OperationSet(true);
		op.operators.add(Multiply.class);
		op.operators.add(Divide.class);
		op.operators.add(Modulo.class);
		classes.add(op);

		op = new OperationSet(true);
		op.operators.add(Add.class);
		op.operators.add(Subtract.class);
		op.operators.add(Concat.class);
		classes.add(op);

		op = new OperationSet(true);
		op.operators.add(LessThan.class);
		op.operators.add(GreaterThan.class);
		classes.add(op);

		op = new OperationSet(true);
		op.operators.add(Equals.class);
		op.operators.add(NotEquals.class);
		classes.add(op);

		op = new OperationSet(false);
		op.operators.add(Assign.class);
		classes.add(op);


	}

	private static Operation getOperation(List<Operation> ops, int start, Class terminator) throws ScriptParseException {

		// sanity check

		if (ops.size() == 0) {
			return null;
		}
		
		// work your way backwards, only have the non-recursive part eval ()
		if(start == 0) {
			for (int i = ops.size()-1; i >= 0; i--) {
				Operation command = ops.get(i);
				if (command instanceof LParen) {
					((LParen)command).inner = getOperation(ops,i+1,RParen.class);
					ops.remove(i+1);
				}
				if (command instanceof LBracket) {
					((LBracket)command).right= getOperation(ops,i+1,RBracket.class);
					ops.remove(i+1);
				}
			}
		}

		for (OperationSet opset: classes) {
			// if terminator defind, we have terminator as endpos

			int startPos;

			if (opset.leftToRight) {
				startPos=start;
			}
			else if (terminator != null) {
				startPos = ops.size()-1;
				for (int i = ops.size()-1; i >= start; i--) {
					Operation command = ops.get(i);
					if(terminator.isInstance(command)) {
						startPos = i;
					}
				}
			} else {
				startPos = ops.size()-1;
			}


			int i = startPos;
			for (; ;) {
				if (opset.leftToRight) {
					if (i>=ops.size()) {
						break;
					}
				} else {
					if (i < start) {
						break;
					}
				}
				Operation command = ops.get(i);


				// if start != 0 we are in a (), stop at the )

				if (start > 0 && terminator.isInstance(command)) {
					break;
				}

				for (Class clazz : opset.operators) {
					if ( clazz.isInstance(command) ) {
						if (UnaryOperator.class.isAssignableFrom(command.getClass())) {
							UnaryOperator op = (UnaryOperator)command;
							op.right = ops.get(i+1);
							ops.remove(i+1);
						} else if (LBracket.class.isAssignableFrom(command.getClass())) {
							// works like binary operator but we handle [] like ()
							BinaryOperator op = (BinaryOperator)command;
							op.left = ops.get(i-1);
							ops.remove(i-1);
							i--;
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
				i=opset.leftToRight?i+1:i-1;
			}
		}

		if (start > 0) {
			if(ops.size() > (start + 1)) {
				if (terminator.isInstance(ops.get(start+1))) {
					ops.remove(start+1);
					return ops.get(start);
				}
			}

			String list = "";
			if(start < ops.size()) {
				for (Operation op : ops.subList(start, ops.size())) {
					list += " " + op.getClass().getName();
				}
			}
			else {
				list += " Full List: "; 
				for (Operation op : ops) {
					list += " " + op.getClass().getName();
				}
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

	public void addExternalFunction(String name, ExternalFunction func) {
		Function f = new Function();
		f.func= func;
		functionTable.put(name,f);
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

