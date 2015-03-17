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
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class Script{
  

	Hashtable <String, Object> symbolTable = new Hashtable<String,Object>();
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

		public String exec(List<String> arg) throws ScriptParseException {
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

		public String exec(List<String> arg) throws ScriptParseException {
			return exec();
		}
		public String exec() throws ScriptParseException {
			if (op == null) {
				symbolTable.put(name, "");
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

		public String exec(List<String> arg) throws ScriptParseException {
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

		public String exec(List<String> arg) throws ScriptParseException {
			return exec();
		}
		public String exec() throws ScriptParseException {
			StringBuffer sb = new StringBuffer();
			int counter = 0;
			while (Integer.parseInt(op.eval().toString()) != 0) {
				if(loopLimit > 0 && counter > loopLimit) {
					throw new ScriptParseException("While: loop count exceeded. Limit=" + loopLimit + " Current=" + counter + " Condition=" + op.eval());
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


		public String exec(List<String> arg) throws ScriptParseException {
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

		public String exec(List<String> arg) throws ScriptParseException {
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
				if (inner instanceof Listable ) {
					return ((Listable)inner).getList();
				}
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
			return new Assignable() {
				int index;
				JSONArray arr;
				Object leval;

				Object init(Object leval, int index) throws ScriptParseException {
					Object obj=JSONValue.parse(leval.toString());
					if ( obj instanceof JSONArray) {
						this.arr = (JSONArray)obj;
					}
					else {
						throw new ScriptParseException("[] expected array as lval\nGot: " + left.eval().toString());
					}

					if ( index >= arr.size()) {
						throw new ScriptParseException("[] index " + index + " is out of bounds of defined array of size " + arr.size());
					}
					this.leval = leval;
					this.index = index;
					return this;
				}

				public String toString() {
					return arr.get(index).toString();
				}

				public void assign(Object value) throws ScriptParseException {
					arr.set(index,value);
					if (leval instanceof Assignable) {
						((Assignable)leval).assign(arr);
					} else {
						throw new ScriptParseException("[] trying to assign value to non assigable lparam " + leval.getClass().getName());
					}
					
				}

			}.init(left.eval(),Integer.parseInt(right.eval().toString()));
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

			if (!(left instanceof Assignable)) {
				throw new ScriptParseException(this.getClass().getName() +": attempting to assign value to non-variable type");
			}

			// must override to have proper return val
			return null;
		}

	}

	private class Increment extends PostfixOperator {
		public Object eval() throws ScriptParseException {
			super.eval();

			Assignable ass = (Assignable)left;
			int v = Integer.parseInt(left.eval().toString());
			Integer result = Integer.valueOf(v+1);
			ass.assign(result);
			return Integer.valueOf(v);
		}
	}	

	private class Decrement extends PostfixOperator {
		public Object eval() throws ScriptParseException {
			super.eval();

			Assignable ass = (Assignable)left;
			int v = Integer.parseInt(left.eval().toString());
			Integer result = Integer.valueOf(v-1);
			ass.assign(result);
			return Integer.valueOf(v);
		}
	}	

	private class BindingOperator extends BinaryOperator {
		public Object eval() throws ScriptParseException {
			super.eval();

			String rval = right.eval().toString();

			if (rval.contains("/")) {
				// add a bogus option to ensure correct splitting
				rval += "_"; 
			}

			String [] args = rval.split("/");

			if (args.length == 1) {
				// simple string match
				boolean res = left.eval().toString().contains(args[0]);
				return res?"1":"0";
			} else if(args.length == 3 ) {
				// regex match in the form of /regex/opts
				int flags = 0;
				if(args[2].contains("i")) {
					flags |= Pattern.CASE_INSENSITIVE;
				}
				if(args[2].contains("s")) {
					flags |= Pattern.DOTALL;
				}
				if(args[2].contains("m")) {
					flags |= Pattern.MULTILINE;
				}

				Pattern p = Pattern.compile(args[1],flags);
				return p.matcher(left.eval().toString()).matches()?"1":"0";
			} else if(args.length == 4) {
				// substitution s/src/dst/opts
				Object lval = left.eval();

				String result = lval.toString().replaceAll(args[1], args[2]);

				if (lval instanceof Assignable) {
					Assignable ass = (Assignable)lval;
					ass.assign(result);
				}

				return result;
			}
			throw new ScriptParseException("=~ malformed regex " + rval);
		}
	}

	private class BitwiseAnd extends BinaryOperator {
		public Object eval() throws ScriptParseException {
			super.eval();
			return Integer.valueOf(Integer.parseInt(left.eval().toString()) & Integer.parseInt(right.eval().toString()));
		}
	}

	private class BitwiseOr extends BinaryOperator {
		public Object eval() throws ScriptParseException {
			super.eval();
			return Integer.valueOf(Integer.parseInt(left.eval().toString()) | Integer.parseInt(right.eval().toString()));
		}
	}

	private class Add extends BinaryOperator {
		public Object eval() throws ScriptParseException {
			super.eval();
			return Integer.valueOf(Integer.parseInt(left.eval().toString()) + Integer.parseInt(right.eval().toString()));
		}
	}

	private class Subtract extends BinaryOperator {
		public Object eval() throws ScriptParseException {
			super.eval();
			return Integer.valueOf(Integer.parseInt(left.eval().toString()) - Integer.parseInt(right.eval().toString()));
		}
	}

	private class Multiply extends BinaryOperator {
		public Object eval() throws ScriptParseException {
			super.eval();
			return Integer.valueOf(Integer.parseInt(left.eval().toString()) * Integer.parseInt(right.eval().toString()));
		}
	}

	private class Divide extends BinaryOperator {
		public Object eval() throws ScriptParseException {
			super.eval();
			return Integer.valueOf(Integer.parseInt(left.eval().toString()) / Integer.parseInt(right.eval().toString()));
		}
	}

	private class Modulo extends BinaryOperator {
		public Object eval() throws ScriptParseException {
			super.eval();
			return Integer.valueOf(Integer.parseInt(left.eval().toString()) % Integer.parseInt(right.eval().toString()));
		}
	}

	private class GreaterThan extends BinaryOperator {
		public Object eval() throws ScriptParseException {
			super.eval();
			return Integer.parseInt(left.eval().toString()) > Integer.parseInt(right.eval().toString())?Integer.valueOf("1"):Integer.valueOf("0");
		}
	}

	private class GreaterThanOrEquals extends BinaryOperator {
		public Object eval() throws ScriptParseException {
			super.eval();
			return Integer.parseInt(left.eval().toString()) >= Integer.parseInt(right.eval().toString())?Integer.valueOf("1"):Integer.valueOf("0");
		}
	}

	private class LessThan extends BinaryOperator {
		public Object eval() throws ScriptParseException {
			super.eval();
			return Integer.parseInt(left.eval().toString()) < Integer.parseInt(right.eval().toString())?Integer.valueOf("1"):Integer.valueOf("0");
		}
	}

	private class LessThanOrEquals extends BinaryOperator {
		public Object eval() throws ScriptParseException {
			super.eval();
			return Integer.parseInt(left.eval().toString()) <= Integer.parseInt(right.eval().toString())?Integer.valueOf("1"):Integer.valueOf("0");
		}
	}

	private class ConditionalAnd extends BinaryOperator {
		public Object eval() throws ScriptParseException {
			super.eval();
			boolean result = (Integer.parseInt(left.eval().toString()) != 0) && (Integer.parseInt(right.eval().toString()) != 0);
			return result?Integer.valueOf("1"):Integer.valueOf("0");
		}
	}

	private class ConditionalOr extends BinaryOperator {
		public Object eval() throws ScriptParseException {
			super.eval();
			boolean result = (Integer.parseInt(left.eval().toString()) != 0) || (Integer.parseInt(right.eval().toString()) != 0);
			return result?Integer.valueOf("1"):Integer.valueOf("0");
		}
	}

	private class Equals extends BinaryOperator {
		public Object eval() throws ScriptParseException {
			super.eval();
			return left.eval().toString().equals(right.eval().toString())?Integer.valueOf("1"):Integer.valueOf("0");
		}
	}

	private class NotEquals extends BinaryOperator {
		public Object eval() throws ScriptParseException {
			super.eval();
			return left.eval().toString().equals(right.eval().toString())?Integer.valueOf("0"):Integer.valueOf("1");
		}
	}

	private class Not extends UnaryOperator {
		public Object eval() throws ScriptParseException {
			super.eval();
			return Integer.parseInt(right.eval().toString()) == 0?Integer.valueOf("1"):Integer.valueOf("0");
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
			return new Assignable() {
				String key;
				JSONObject map;
				Object leval;

				Object init(Object leval, String key) throws ScriptParseException {
					Object obj=JSONValue.parse(leval.toString());
					if ( obj instanceof JSONObject) {
						this.map= (JSONObject)obj;
					}
					else {
						throw new ScriptParseException("-> expected map as lval\nGot: " + left.eval().toString());
					}

					this.leval = leval;
					this.key = key;
					return this;
				}

				public String toString() {
					Object obj = map.get(key);
					if(obj == null) {
						return "";
					} else {
						return obj.toString();
					}
				}

				public void assign(Object value) throws ScriptParseException {
					map.put(key,value);
					if (leval instanceof Assignable) {
						((Assignable)leval).assign(map);
					} else {
						throw new ScriptParseException("-> trying to assign value to non assigable lparam " + leval.getClass().getName());
					}
					
				}

			}.init(left.eval(),right.eval().toString());
		}
	}

	private class Number implements Operation {
		Integer number;
		
		public Number(String number) {
			this.number = Integer.valueOf(number);
		}

		public Object eval() throws ScriptParseException {
			return number;
		}
	}

	private class Rand implements Operation {
		Integer number;
		
		public Rand() {
		}

		public Object eval() throws ScriptParseException {
			return new Integer(random.nextInt(Integer.MAX_VALUE));
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

	private static class NoOP implements Operation, Listable {
		public Object eval() throws ScriptParseException {
			return "";
		}

		public List<Object> getList() {
			return new JSONArray();
		}
	}


	private class Variable implements Operation, Assignable {
		String name;

		public Variable(String name) {
			this.name = name;
		}

		public Object eval() throws ScriptParseException {
			if (symbolTable.get(name) == null) {
				throw new ScriptParseException("Undefined variable: " + name);
			}
			return this;
		}
		public String toString() {
			return symbolTable.get(name).toString();
		}

		public void assign(Object value) throws ScriptParseException {
			if (symbolTable.get(name) == null) {
				throw new ScriptParseException("Undefined variable: " + name);
			}
			symbolTable.put(name, value);
		}
	}

	private class Assign extends BinaryOperator {
		public Object eval() throws ScriptParseException {
			super.eval();

			Object larg = left.eval();

			if (!(larg instanceof Assignable)) {
				throw new ScriptParseException("Assign: attempting to assign value to non-assignable type " + larg.getClass().getName());
			}
			Assignable ass = (Assignable)larg;
			Object v = right.eval();
			ass.assign(v);
			return v;
		}
	}

	private class Comma extends BinaryOperator implements Listable {
		public Object eval() throws ScriptParseException {
			super.eval();

			// comma in scalar context evaluates larg but then discards it
			Object larg = left.eval();

			Object v = right.eval();
			return v;
		}

		public List<Object> getList() throws ScriptParseException {
			List<Object> array;
			if (left instanceof Listable) {
				array = ((Listable)left).getList();
			} else {
				Object obj = left.eval();
				array = new JSONArray();
				array.add(obj);
			}

			if (right instanceof Listable) {
				for (Object obj : ((Listable)right).getList()) {
					array.add(obj);
				}
			} else {
				array.add(right.eval());
			}
			return array;

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
		  } else if(input.equals(",")) {
			return new Comma();
		  } else if(input.equals("=")) {
			return new Assign();
		  } else if(input.equals("==")) {
			return new Equals();
		  } else if(input.equals("=~")) {
			return new BindingOperator();
		  } else if(input.equals("->")) {
			return new Reference();
		  } else if(input.equals("!=")) {
			return new NotEquals();
		  } else if(input.equals(">")) {
			return new GreaterThan();
		  } else if(input.equals(">=")) {
			return new GreaterThanOrEquals();
		  } else if(input.equals("<")) {
			return new LessThan();
		  } else if(input.equals("<=")) {
			return new LessThanOrEquals();
		  } else if(input.equals("&&")) {
			return new ConditionalAnd();
		  } else if(input.equals("&")) {
			return new BitwiseAnd();
		  } else if(input.equals("||")) {
			return new ConditionalOr();
		  } else if(input.equals("|")) {
			return new BitwiseOr();
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
		op.operators.add(BindingOperator.class);
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
		op.operators.add(LessThanOrEquals.class);
		op.operators.add(GreaterThanOrEquals.class);
		classes.add(op);

		op = new OperationSet(true);
		op.operators.add(Equals.class);
		op.operators.add(NotEquals.class);
		classes.add(op);

		op = new OperationSet(true);
		op.operators.add(BitwiseAnd.class);
		classes.add(op);

		op = new OperationSet(true);
		op.operators.add(BitwiseOr.class);
		classes.add(op);

		op = new OperationSet(true);
		op.operators.add(ConditionalAnd.class);
		classes.add(op);

		op = new OperationSet(true);
		op.operators.add(ConditionalOr.class);
		classes.add(op);

		op = new OperationSet(false);
		op.operators.add(Assign.class);
		classes.add(op);

		op = new OperationSet(true);
		op.operators.add(Comma.class);
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
			if (ops.size() > (start + 1)) {
				if (terminator.isInstance(ops.get(start+1))) {
					ops.remove(start+1);
					return ops.get(start);
				}
			} else if (ops.size() > (start)) {
 
				// this is the case where terminator is right after the start
				if (terminator.isInstance(ops.get(start))) {
					ops.remove(start);
					ops.add(start, new NoOP());
					return ops.get(start);
				}
			}

			String list = "";
			if (start < ops.size()) {
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

