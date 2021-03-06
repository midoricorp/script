package com.sipstacks.script;

import java.io.Reader;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Stack;
import java.util.List;
import java.util.Random;
import org.json.simple.*;

import javax.script.ScriptException;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class Script{
  

	Stack<ScopedHashtable> symbolTable = new Stack<ScopedHashtable>();
	Hashtable <String, Function> functionTable = new Hashtable<String,Function>();
	ScriptScanner scanner;
	Random random;

	/**
	 * These counters are used during parsing to make sure break, continue
	 * and return are only used in legal places
	 */
	private class StatementCounters {
		int numWhile = 0;
		int numSub = 0;
	}

	StatementCounters stmtCounters = new StatementCounters();

	ArrayList<Statement> script = new ArrayList<Statement>();

	private FunctionListener _functionListener = null;

	int loopLimit;

	private void pushStack() {
		symbolTable.push(new ScopedHashtable());
	}

	private void popStack() {
		symbolTable.pop();
	}

	public Script(Reader in) {
		pushStack();
		scanner = new ScriptScanner(in);
		random = new Random();
		loopLimit = -1; // default no limit
		addStatementFunction("get", new GetFunction());
		addFunction(new PostFunction());
		addStatementFunction("html_encode", new HtmlEncodeFunction());
		addStatementFunction("url_encode", new UrlEncodeFunction());
		addStatementFunction("url_encode", new UrlEncodeFunction());
		addStatementFunction("uc", new ToUppercaseFunction());
		addStatementFunction("lc", new ToLowercaseFunction());

		addFunction(new SplitFunction());
		addFunction(new JoinFunction());
		addFunction(new ShiftFunction());
		addFunction(new PopFunction());
		addFunction(new PushFunction());
		addFunction(new UnshiftFunction());
		addFunction(new GmTime());
	}

	public void setLoopLimit(int limit) {
		this.loopLimit = limit;
	}

	private class Expression implements Statement {
		com.sipstacks.script.Expression op;

		public void exec(OutputStream os, List<String> arg) throws ScriptParseException, ScriptFlowException {
			exec(os);
		}
		public void exec(OutputStream os) throws ScriptParseException, ScriptFlowException {
			// allow expression to be just a ;
			if (op != null) {
				try {
					op.eval();
				} catch (ScriptParseException e) {
					StringBuffer sb = new StringBuffer();
					sb.append("The following Statement\n");
					sb.append(op.dump());
					sb.append("Generated the following error\n");
					sb.append(e.getMessage());
					throw new ScriptFlowException(sb.toString());
				}
			}
		}

		public Expression() throws ScriptParseException {
			  ArrayList<com.sipstacks.script.Expression> ops = new ArrayList<com.sipstacks.script.Expression>();
			  String input = null;
			  while((input=scanner.getToken())!=null){
				  //System.out.println("Got token '"+input+"'");
				  if (input.equals(";")) {
					com.sipstacks.script.Expression op = getExpression(ops);
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

		public void reset() {}
		
		public String dump() {
			return op.dump() + ";\n";
		}

		public void getFunctions(List<Function> functions) { op.getFunctions(functions); }

	}

	private class Break implements Statement {

		public Break() throws ScriptParseException {
			String input = scanner.getToken();

			if(!input.equals(";")) {
				throw new ScriptParseException("Break: expected ; got " + input, scanner);
			}
		}
		@Override
		public void exec(OutputStream os) throws ScriptParseException, ScriptFlowException {
			throw new ScriptBreakFlowException();
		}

		@Override
		public void exec(OutputStream os, List<String> arg) throws ScriptParseException, ScriptFlowException {
			exec(os);
		}

		@Override
		public void reset() {

		}

		@Override
		public String dump() {
			return "break;\n";
		}

		@Override
		public void getFunctions(List<Function> functions) {

		}
	}

	private class Continue implements Statement {

		public Continue() throws ScriptParseException {
			String input = scanner.getToken();

			if(!input.equals(";")) {
				throw new ScriptParseException("Continue: expected ; got " + input, scanner);
			}
		}
		@Override
		public void exec(OutputStream os) throws ScriptParseException, ScriptFlowException {
			throw new ScriptContinueFlowException();
		}

		@Override
		public void exec(OutputStream os, List<String> arg) throws ScriptParseException, ScriptFlowException {
			exec(os);
		}

		@Override
		public void reset() {

		}

		@Override
		public String dump() {
			return "continue;\n";
		}

		@Override
		public void getFunctions(List<Function> functions) {

		}
	}

	private class Return implements Statement {

		public Return() throws ScriptParseException {
			String input = scanner.getToken();

			if(!input.equals(";")) {
				throw new ScriptParseException("Return: expected ; got " + input, scanner);
			}
		}
		@Override
		public void exec(OutputStream os) throws ScriptParseException, ScriptFlowException {
			throw new ScriptReturnFlowException();
		}

		@Override
		public void exec(OutputStream os, List<String> arg) throws ScriptParseException, ScriptFlowException {
			exec(os);
		}

		@Override
		public void reset() {

		}

		@Override
		public String dump() {
			return "return;\n";
		}

		@Override
		public void getFunctions(List<Function> functions) {

		}
	}

	private class Var implements Statement {
		String name;
		com.sipstacks.script.Expression op;

		public void exec(OutputStream os, List<String> arg) throws ScriptParseException, ScriptFlowException {
				exec(os);
		}
		public void exec(OutputStream os) throws ScriptParseException, ScriptFlowException {
			if (op == null) {
				symbolTable.peek().put(name, "");
			} else {
				Object eval = null;
				try {
					eval = op.eval();
				} catch (ScriptParseException e) {
					StringBuffer sb = new StringBuffer();
					sb.append("The following Statement\n");
					sb.append(dump());
					sb.append("Generated the following error\n");
					sb.append(e.getMessage());
					throw new ScriptFlowException(sb.toString());
				}
				if (eval instanceof Assignable ) {
					Assignable ass = (Assignable)eval;
					symbolTable.peek().put(name, ass.getValue());
				} else if (eval instanceof ObjectReference) {
					symbolTable.peek().put(name, eval);
				} else {
					symbolTable.peek().put(name, new ObjectReference(eval));
				}
			}
		}

		public Var() throws ScriptParseException {
			  ArrayList<com.sipstacks.script.Expression> ops = new ArrayList<com.sipstacks.script.Expression>();
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
						com.sipstacks.script.Expression op = getExpression(ops);

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

		public void reset() {}

		public String dump() {
			String str = "var " + name;

			if (op != null) {
				str += " = " + op.dump();
			}

			str += ";\n";
			return str;
		}

		public void getFunctions(List<Function> functions) {
			if (op != null) {
				op.getFunctions(functions);
			}
		}

	}

	private class FunctionDeclare implements Statement {
		String func_name;
		Statement cmd;
		boolean local;

		public void exec(OutputStream os, List<String> arg) throws ScriptParseException, ScriptFlowException {
			exec(os);
		}
		public void exec(OutputStream os) throws ScriptParseException, ScriptFlowException {
			StatementFunction f = new StatementFunction();
			f.stmt = cmd;
			f.name = func_name;
			functionTable.put(func_name,f);
		}

		public FunctionDeclare(boolean local) throws ScriptParseException {


			this.local = local;
			this.func_name = scanner.getToken();
			try {
				stmtCounters.numSub++;
				this.cmd = new ScopedStatement(getStatement());
				stmtCounters.numSub--;
			} catch (ScriptParseException e) {
				throw new ScriptParseException(e.getMessage(), scanner);
			}
			if (cmd == null) {
				throw new ScriptParseException("sub: missing statement", scanner);
			}

			if (!local && _functionListener != null) {
				_functionListener.addFunction(func_name, cmd);
			}

		}

		public FunctionDeclare() throws ScriptParseException {
			this(false);
		}

		public void reset() {}

		public String dump() {
			return (local?"local ":"") + "sub " + func_name + "\n" + cmd.dump() + "\n";
		}

		public void getFunctions(List<Function> functions) {
			cmd.getFunctions(functions);
		}
	}

	private class ScopedStatement implements Statement {

		Statement cmd;
		private int totalCalls = 0;

		public ScopedStatement(Statement cmd) throws ScriptParseException {
			if (cmd == null) {
				throw new ScriptParseException("Attempting to set Scoped Statement to NULL!");
			}
			this.cmd = cmd;
		}

		public void exec(OutputStream os, List<String> arg) throws ScriptParseException, ScriptFlowException {
			pushStack();
			int i = 0;
			symbolTable.peek().put("_", JSONValue.toJSONString(arg));
			try {
				cmd.exec(os);
			} catch (ScriptReturnFlowException e) {
				// somebody called return, consume it and return
			}
			popStack();
		}

		public void exec(OutputStream os) throws ScriptParseException, ScriptFlowException {
			if(loopLimit > 0 && totalCalls > loopLimit) {
				throw new ScriptParseException("ScopedStatement: recursion depth exceeded. Limit=" + (loopLimit) + " Current=" + totalCalls);
			}
			totalCalls++;

			pushStack();
			try {
				cmd.exec(os);
			} catch (ScriptReturnFlowException e) {
				// somebody called return, consume it and return
			}
			popStack();
		}

		public void reset() {
			totalCalls = 0;
			cmd.reset();
		}

		public String dump() {
			return cmd.dump();
		}

		public void getFunctions(List<Function> functions) {
			cmd.getFunctions(functions);
		}
	}

	private class If implements Statement {
		com.sipstacks.script.Expression op;
		Statement cmd;
		Statement else_cmd;

		public void exec(OutputStream os, List<String> arg) throws ScriptParseException, ScriptFlowException {
			exec(os);
		}
		public void exec(OutputStream os) throws ScriptParseException, ScriptFlowException {
			symbolTable.peek().enterScope();
			boolean b = false;
			try{
				b = parseBoolean(op.eval().toString());
			} catch (ScriptParseException e) {
				StringBuffer sb = new StringBuffer();
				sb.append("The following Statement\n");
				sb.append("if (" + op.dump() + ")\n");
				sb.append("Generated the following error\n");
				sb.append(e.getMessage());
				throw new ScriptFlowException(sb.toString());
			}
			if (b) {
				cmd.exec(os);
			}
			else if (else_cmd != null) {
				else_cmd.exec(os);
			}
			symbolTable.peek().exitScope();
		}

		public String dump() {
			String str = "if (" + op.dump() + ")\n" + cmd.dump();

			if (else_cmd != null) {
				str += "else ";
				if(!(else_cmd instanceof If)) {
					str +="\n";
				}
				str += else_cmd.dump();
			}

			return str;
		}

		public void getFunctions(List<Function> functions) {
			op.getFunctions(functions);
			cmd.getFunctions(functions);

			if (else_cmd != null) {
				else_cmd.getFunctions(functions);
			}

		}

		public If() throws ScriptParseException {

			com.sipstacks.script.Expression op = null;
			Statement cmd = null;
			Statement else_cmd = null;

			String token;
			token = scanner.getToken();

			if ( !token.equals("(") ) {
				throw new ScriptParseException("If: Expected '('", scanner);
			}

			ArrayList<com.sipstacks.script.Expression> ops = new ArrayList<com.sipstacks.script.Expression>();

			int depth = 0;

			while (true) {
				token = scanner.getToken();
				if (token == null ) {
					throw new ScriptParseException("If: unexpected eof on condition", scanner);
				}

				if (token.equals("(")) {
					depth++;
				} else if (token.equals(")")) {
					// make sure its the RParen we want
					if (depth == 0) {
						op = getExpression(ops);
						if (op == null) {
							throw new ScriptParseException("If: missing op inside ()", scanner);
						}
						break;
					} else {
						depth--;
					}
				}
				try {
					ops.add(tokenToOp(token));
				} catch (ScriptParseException spe) {
					throw new ScriptParseException("If: " + spe.getMessage(), scanner);
				}
			}

			cmd = getStatement();
			if (cmd == null) {
				throw new ScriptParseException("If: missing statement", scanner);
			}

			token = scanner.getToken();

			if (token != null ) {
				if ( token.equals("else") ) {
					else_cmd = getStatement();
				} else {
					scanner.pushBack(token);
				}
			}
				

			this.op = op;
			this.cmd = cmd;
			this.else_cmd = else_cmd;
		}

		public void reset() {
			cmd.reset();

			if (else_cmd != null) {
				else_cmd.reset();
			}
		}
	}

	private class While implements Statement {
		com.sipstacks.script.Expression op;
		Statement cmd;

		private int totalCalls = 0;

		public void exec(OutputStream os, List<String> arg) throws ScriptParseException, ScriptFlowException {
			exec(os);
		}
		public void exec(OutputStream os) throws ScriptParseException, ScriptFlowException {
			int counter = 0;
			symbolTable.peek().enterScope();
			while (true) {
				boolean b = false;
				try {
					b = parseBoolean(op.eval().toString());
				} catch (ScriptParseException e) {
					StringBuffer sb = new StringBuffer();
					sb.append("The following Statement\n");
					sb.append("while (" + op.dump() + ")\n");
					sb.append("Generated the following error\n");
					sb.append(e.getMessage());
					throw new ScriptFlowException(sb.toString());
				}
				if (!b) break;
				if(loopLimit > 0 && counter > loopLimit) {
					throw new ScriptParseException("While: loop count exceeded. Limit=" + loopLimit + " Current=" + counter + " Condition=" + op.eval());
				}
				if(loopLimit > 0 && totalCalls > loopLimit*loopLimit) {
					throw new ScriptParseException("While: nested loop count exceeded. Limit=" + (loopLimit*loopLimit) + " Current=" + totalCalls);
				}
				try {
					cmd.exec(os);
				} catch (ScriptBreakFlowException e) {
					break;
				} catch (ScriptContinueFlowException e) {
					// swallow and continue looping
				}
				counter++;
				totalCalls++;
			}
			symbolTable.peek().exitScope();
		}

		public String dump() {
			return "while (" + op.dump() + ")\n" + cmd.dump();
		}

		public void getFunctions(List<Function> functions) {
			op.getFunctions(functions);
			cmd.getFunctions(functions);
		}

		public While() throws ScriptParseException {

			com.sipstacks.script.Expression op = null;
			Statement cmd = null;

			String token;
			token = scanner.getToken();

			if ( !token.equals("(") ) {
				throw new ScriptParseException("While: Expected '('", scanner);
			}

			ArrayList<com.sipstacks.script.Expression> ops = new ArrayList<com.sipstacks.script.Expression>();

			int depth = 0;

			while (true) {
				token = scanner.getToken();
				if (token == null ) {
					throw new ScriptParseException("While: unexpected eof on condition", scanner);
				}

				if (token.equals("(")) {
					depth++;
				} else if (token.equals(")")) {
					// make sure it's the RParen we want we want
					if (depth == 0) {
						op = getExpression(ops);
						if (op == null) {
							throw new ScriptParseException("While: missing op inside ()", scanner);
						}
						break;
					} else {
						depth--;
					}
				}

				try {
					ops.add(tokenToOp(token));
				} catch (ScriptParseException spe) {
					throw new ScriptParseException("While: " + spe.getMessage(), scanner);
				}
			}

			stmtCounters.numWhile++;
			cmd = getStatement();
			stmtCounters.numWhile--;

			if (cmd == null) {
				throw new ScriptParseException("While: missing statement", scanner);
			}

			this.op = op;
			this.cmd = cmd;
		}

		public void reset() {
			totalCalls = 0;
			cmd.reset();
		}
	}

	private class StatementBlock implements Statement {
		ArrayList<Statement> statements;


		public void exec(OutputStream os, List<String> arg) throws ScriptParseException, ScriptFlowException {
			exec(os);
		}
		public void exec(OutputStream os) throws ScriptParseException, ScriptFlowException {
			symbolTable.peek().enterScope();
			for (Statement cmd : statements) {
				cmd.exec(os);
			}
			symbolTable.peek().exitScope();
		}

		public String dump() {
			StringBuffer sb = new StringBuffer();
			StringBuffer sb2 = new StringBuffer();
			sb2.append("{\n");
			for (Statement cmd : statements) {
				sb2.append(cmd.dump());
			}
			String cmdstr = sb2.toString().replaceAll("\n", "\n\t");
			cmdstr = cmdstr.substring(0, cmdstr.length()-1);
			sb.append(cmdstr);
			sb.append("}\n");
			return sb.toString();
		}

		public void getFunctions(List<Function> functions) {
			for (Statement cmd : statements) {
				cmd.getFunctions(functions);
			}
		}

		public void reset() {
			for (Statement cmd : statements) {
				cmd.reset();
			}
		}

		public StatementBlock() throws ScriptParseException {
			ArrayList<Statement> statements = new ArrayList<Statement>();


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
				Statement cmd = getStatement();
				statements.add(cmd);
			}

			this.statements = statements;
		}
	}

	private class Print implements Statement {
		com.sipstacks.script.Expression op;
		boolean isHtml;

		public void exec(OutputStream os, List<String> arg) throws ScriptParseException, ScriptFlowException {
			exec(os);
		}
		public void exec(OutputStream os) throws ScriptParseException, ScriptFlowException {
			if (op != null) {
				String s = null;
				try {
					s =op.eval().toString();
				} catch (ScriptParseException e) {
					StringBuffer sb = new StringBuffer();
					sb.append("The following Statement\n");
					sb.append(dump());
					sb.append("Generated the following error\n");
					sb.append(e.getMessage());
					throw new ScriptFlowException(sb.toString());
				}
				if (isHtml) {
					os.appendHtml(s + "\n");
				} else {
					os.appendText(s + "\n");
				}
			} else {
				if (isHtml) {
					os.appendHtml("\n");
				} else {
					os.appendText("\n");
				}
			}
		}

		public String dump() {
			if (isHtml) {
				return "print HTML " + op.dump() + ";\n";
			}
			return "print " + op.dump() + ";\n";
		}

		public void getFunctions(List<Function> functions) {
			op.getFunctions(functions);
		}

		public void reset() {
		}

		public Print() throws ScriptParseException {

			com.sipstacks.script.Expression op = null;

			ArrayList<com.sipstacks.script.Expression> ops = new ArrayList<com.sipstacks.script.Expression>();

			boolean first = true;

			while (true) {
				String token = scanner.getToken();
				if ( first && token.equals("HTML")) {
					isHtml = true;
					first = false;
					continue;
				} else if ( first && token.equals("TEXT")) {
					isHtml = false;
					first = false;
					continue;
				}

				first = false;

				if (token == null ) {
					throw new ScriptParseException("Print: unexpected eof on condition", scanner);
				}

				if (token.equals(";")) {
					op = getExpression(ops);
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

	private interface AlsoBinary {

		BinaryOperator getBinary();

	}
	private class LParen implements com.sipstacks.script.Expression {
		com.sipstacks.script.Expression inner;
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

		public String dump() {
			if (inner != null) {
				return "(" + inner.dump() + ")";
			} else {
				return "()";
			}
		}

		public void getFunctions(List<Function> functions) {
			inner.getFunctions(functions);
		}

		@Override
		public boolean complete() {
			return inner != null;
		}
	}

	private class RParen implements com.sipstacks.script.Expression {
		public Object eval() throws ScriptParseException {
			throw new ScriptParseException("Trying to eval a ). This shouldn't happen");
		}

		public String dump() {
			return ")"; // removed during reduction so no need for implementation, but useful for debugging
		}

		public void getFunctions(List<Function> functions) {

		}

		@Override
		public boolean complete() {
			return true;
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
					ObjectReference eval = null;
					Object obj = leval;
					if (leval instanceof Assignable) {
						obj = ((Assignable)leval).getValue();
					}

					if (obj instanceof ObjectReference){
						eval = (ObjectReference)obj;
					} else {
						eval = new ObjectReference(obj);
					}

					if (eval.toJSON() == null) {
						throw new ScriptParseException("[] expected array as lval\nGot: " + left.eval().toString());
					}

					if (!(eval.getReference() instanceof JSONArray)) {
						throw new ScriptParseException("[] expected array as lval\nGot a map: " + left.eval().toString());
					}

					this.arr = (JSONArray)eval.getReference();

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

				public Object getValue() {
					return arr.get(index);
				}

				public void assign(Object value) throws ScriptParseException {
					arr.set(index,value);
					if (leval instanceof Assignable) {
						((Assignable)leval).assign(arr);
					} else {
						throw new ScriptParseException("[] trying to assign value to non assigable lparam " + leval.getClass().getName());
					}
					
				}

			}.init(left.eval(),parseInteger(right.eval().toString()));
		}

		public String dump() {
			String rightstr = right==null?"undefined":right.dump();
			return left.dump() + "[" + rightstr + "]";
		}

		public void getFunctions(List<Function> functions) {
			if (right != null) {
				right.getFunctions(functions);
			}

			left.getFunctions(functions);
		}
	}

	private class RBracket implements com.sipstacks.script.Expression {
		public Object eval() throws ScriptParseException {
			throw new ScriptParseException("Trying to eval a ]. This shouldn't happen");
		}

		public String dump() {
			return "]"; // removed during reduction so no need for implementation but useful for debugging
		}

		public void getFunctions(List<Function> functions) {

		}

		@Override
		public boolean complete() {
			return false;
		}
	}

	private static abstract class BinaryOperator implements com.sipstacks.script.Expression {
		com.sipstacks.script.Expression left;
		com.sipstacks.script.Expression right;
		String operator;

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

		public String dump() {
			String leftstr = left == null ? " undefined ": left.dump();
			String rightstr = right == null ? " undefined ": right.dump();
			return leftstr + operator + rightstr;
		}

		@Override
		public void getFunctions(List<Function> functions) {
			if (left != null) {
				left.getFunctions(functions);
			}

			if (right != null) {
				right.getFunctions(functions);
			}
		}

		@Override
		public boolean complete() {
			return (left != null && right != null);
		}

	}

	private static abstract class PostfixOperator implements com.sipstacks.script.Expression {
		com.sipstacks.script.Expression left;
		String operator;

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

		public String dump() {
			return (left == null ? "undefined" : left.dump()) + operator;
		}

		public void getFunctions(List<Function> functions) {
			if (left != null) {
				left.getFunctions(functions);
			}
		}

		@Override
		public boolean complete() {
			return left != null;
		}

	}

	private class Increment extends PostfixOperator {

		public Increment() {
			operator = "++";
		}

		public Object eval() throws ScriptParseException {
			super.eval();

			Assignable ass = (Assignable)left;
			int v = parseInteger(left.eval().toString());
			Integer result = Integer.valueOf(v+1);
			ass.assign(result);
			return Integer.valueOf(v);
		}
	}	

	private class Decrement extends PostfixOperator {

		public Decrement() {
			operator = "--";
		}

		public Object eval() throws ScriptParseException {
			super.eval();

			Assignable ass = (Assignable)left;
			int v = parseInteger(left.eval().toString());
			Integer result = Integer.valueOf(v-1);
			ass.assign(result);
			return Integer.valueOf(v);
		}
	}	

	private class BindingOperator extends BinaryOperator {

		public BindingOperator() {
			operator = "=~";
		}

		public Object eval() throws ScriptParseException {
			super.eval();

			String rval = right.eval().toString();

			if (rval.contains("/")) {
				// add a bogus option to ensure correct splitting
				rval += "_"; 
			}

			String delim = "/";
			String esc = "\\";
			String regex = "(?<!" + Pattern.quote(esc) + ")" + Pattern.quote(delim);
			String [] args = rval.split(regex);

			if (args.length == 1) {
				// simple string match
				boolean res = left.eval().toString().contains(args[0]);
				return res?"1":"0";
			} else if (args.length == 3 ) {
				// regex match in the form of /regex/opts
				int flags = 0;
				if (args[2].contains("i")) {
					flags |= Pattern.CASE_INSENSITIVE;
				}
				if (args[2].contains("s")) {
					flags |= Pattern.DOTALL;
				}
				if(args[2].contains("m")) {
					flags |= Pattern.MULTILINE;
				}

				Pattern p = Pattern.compile(args[1],flags);
				Matcher m = p.matcher(left.eval().toString());
				
				boolean found = m.find();
				if (found) {
					JSONArray arr = new JSONArray();
					for (int i = 0; i <= m.groupCount(); i++) {
						arr.add(m.group(i));
					}

					symbolTable.peek().put("_", arr);
				}
				return found?"1":"0";
			} else if (args.length == 4) {
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

		public BitwiseAnd() {
			operator = "&";
		}

		public Object eval() throws ScriptParseException {
			super.eval();
			return Integer.valueOf(parseInteger(left.eval().toString()) & parseInteger(right.eval().toString()));
		}
	}

	private class BitwiseOr extends BinaryOperator {

		public BitwiseOr() {
			operator = "|";
		}

		public Object eval() throws ScriptParseException {
			super.eval();
			return Integer.valueOf(parseInteger(left.eval().toString()) | parseInteger(right.eval().toString()));
		}
	}

	private class Positive extends UnaryOperator  implements AlsoBinary{
		public Positive() { operator = "+"; }
		public Object eval() throws ScriptParseException {
			super.eval();
			return Integer.valueOf(parseInteger(right.eval().toString()));
		}

		@Override
		public BinaryOperator getBinary() {
			return new Add();
		}
	}

	private class Add extends BinaryOperator {

		public Add() {
			operator = "+";
		}

		public Object eval() throws ScriptParseException {
			super.eval();
			return Integer.valueOf(parseInteger(left.eval().toString()) + parseInteger(right.eval().toString()));
		}


	}

	private class Negative extends UnaryOperator implements AlsoBinary {
		public Negative() { operator = "-"; }
		public Object eval() throws ScriptParseException {
			super.eval();
			return -Integer.valueOf(parseInteger(right.eval().toString()));
		}

		@Override
		public BinaryOperator getBinary() {
			return new Subtract();
		}
	}
	private class Subtract extends BinaryOperator  {

		public Subtract() {
			operator = "-";
		}

		public Object eval() throws ScriptParseException {
			super.eval();
			return Integer.valueOf(parseInteger(left.eval().toString()) - parseInteger(right.eval().toString()));
		}

	}

	private class Multiply extends BinaryOperator {

		public Multiply() {
			operator = "*";
		}

		public Object eval() throws ScriptParseException {
			super.eval();
			return Integer.valueOf(parseInteger(left.eval().toString()) * parseInteger(right.eval().toString()));
		}
	}

	private class Divide extends BinaryOperator {

		public Divide() {
			operator = "/";
		}

		public Object eval() throws ScriptParseException {
			super.eval();
			return Integer.valueOf(parseInteger(left.eval().toString()) / parseInteger(right.eval().toString()));
		}
	}

	private class Modulo extends BinaryOperator {

		public Modulo() {
			operator = "%";
		}

		public Object eval() throws ScriptParseException {
			super.eval();
			return Integer.valueOf(parseInteger(left.eval().toString()) % parseInteger(right.eval().toString()));
		}
	}

	private class GreaterThan extends BinaryOperator {

		public GreaterThan() {
			operator = ">";
		}

		public Object eval() throws ScriptParseException {
			super.eval();
			return parseInteger(left.eval().toString()) > parseInteger(right.eval().toString())?Integer.valueOf("1"):Integer.valueOf("0");
		}
	}

	private class GreaterThanOrEquals extends BinaryOperator {

		public GreaterThanOrEquals() {
			operator = ">=";
		}

		public Object eval() throws ScriptParseException {
			super.eval();
			return parseInteger(left.eval().toString()) >= parseInteger(right.eval().toString())?Integer.valueOf("1"):Integer.valueOf("0");
		}
	}

	private class LessThan extends BinaryOperator {

		public LessThan() {
			operator = "<";
		}

		public Object eval() throws ScriptParseException {
			super.eval();
			return parseInteger(left.eval().toString()) < parseInteger(right.eval().toString())?Integer.valueOf("1"):Integer.valueOf("0");
		}
	}

	private class LessThanOrEquals extends BinaryOperator {

		public LessThanOrEquals() {
			operator = "<=";
		}

		public Object eval() throws ScriptParseException {
			super.eval();
			return parseInteger(left.eval().toString()) <= parseInteger(right.eval().toString())?Integer.valueOf("1"):Integer.valueOf("0");
		}
	}

	private class ConditionalAnd extends BinaryOperator {

		public ConditionalAnd() {
			operator = "&&";
		}

		public Object eval() throws ScriptParseException {
			super.eval();
			boolean result = parseBoolean(left.eval().toString()) && parseBoolean(right.eval().toString());
			return result?Integer.valueOf("1"):Integer.valueOf("0");
		}
	}

	private class ConditionalOr extends BinaryOperator {

		public ConditionalOr() {
			operator = "||";
		}

		public Object eval() throws ScriptParseException {
			super.eval();
			boolean result = parseBoolean(left.eval().toString()) || parseBoolean(right.eval().toString());
			return result?Integer.valueOf("1"):Integer.valueOf("0");
		}
	}

	private class Equals extends BinaryOperator {

		public Equals() {
			operator = "==";
		}

		public Object eval() throws ScriptParseException {
			super.eval();
			return left.eval().toString().equals(right.eval().toString())?Integer.valueOf("1"):Integer.valueOf("0");
		}
	}

	private class NotEquals extends BinaryOperator {

		public NotEquals() {
			operator = "!=";
		}

		public Object eval() throws ScriptParseException {
			super.eval();
			return left.eval().toString().equals(right.eval().toString())?Integer.valueOf("0"):Integer.valueOf("1");
		}
	}

	private class Not extends UnaryOperator {

		public Not() {
			operator = "!";
		}

		public Object eval() throws ScriptParseException {
			super.eval();
			return (!parseBoolean(right.eval().toString()))?Integer.valueOf("1"):Integer.valueOf("0");
		}
	}

	private class SizeOf extends UnaryOperator {

		public SizeOf() {
			operator = "sizeof";
		}

		public String dump() {
			String rightstr = right==null?"undefined":right.dump();
			return " " + operator + " " + rightstr;
		}

		public Object eval() throws ScriptParseException {
			super.eval();
			Object eval = right.eval();

			if (eval instanceof Assignable) {
				eval = ((Assignable)eval).getValue();
			}

			if (eval instanceof ObjectReference) {
				ObjectReference reference = (ObjectReference)eval;
				reference.toJSON();
				eval = reference.getReference();
			}

			if (eval instanceof JSONObject) {
				JSONArray keys = new JSONArray();
				keys.addAll(((JSONObject)eval).keySet());
				return keys;

			}

			if (eval instanceof JSONArray) {
				return Integer.valueOf(((JSONArray)eval).size());

			}
			eval=JSONValue.parse(eval.toString());

			if ( eval != null && eval instanceof JSONArray) {
				return Integer.valueOf(((JSONArray)eval).size());
			}
			return 0;
		}
	}

	private class Keys extends UnaryOperator {

		public Keys() {
			operator = "keys";
		}

		public Object eval() throws ScriptParseException {
			super.eval();
			Object eval = right.eval();

			if (eval instanceof Assignable) {
				eval = ((Assignable) eval).getValue();
			}

			if (eval instanceof ObjectReference) {
				ObjectReference reference = (ObjectReference)eval;
				reference.toJSON();
				eval = reference.getReference();
			}

			if (eval instanceof JSONObject) {
				JSONArray keys = new JSONArray();	
				keys.addAll(((JSONObject)eval).keySet());
				return keys;

			}

			eval=JSONValue.parse(eval.toString());

			if ( eval != null && eval instanceof JSONObject) {
				JSONArray keys = new JSONArray();	
				keys.addAll(((JSONObject)eval).keySet());
				return keys;
			}
			return new JSONArray();
		}
	}

	private class Concat extends BinaryOperator {

		public Concat() {
			operator = ".";
		}

		public Object eval() throws ScriptParseException {
			super.eval();
			return left.eval().toString()+right.eval().toString();
		}
	}

	private class Reference extends BinaryOperator {

		public Reference() {
			operator = "->";
		}

		public Object eval() throws ScriptParseException {
			super.eval();
			return new Assignable() {
				String key;
				JSONObject map;
				Object leval;

				Object init(Object leval, String key) throws ScriptParseException {

					ObjectReference eval = null;
					Object obj = leval;
					if (leval instanceof Assignable) {
						obj = ((Assignable)leval).getValue();
					}

					if (obj instanceof ObjectReference){
						eval = (ObjectReference)obj;
					} else {
						eval = new ObjectReference(obj);
					}

					if (eval.toJSON() == null) {
						throw new ScriptParseException("-> expected map as lval\nGot: " + left.eval().toString());
					}

					if (!(eval.getReference() instanceof JSONObject)) {
						throw new ScriptParseException("-> expected map as lval\nGot: " + left.eval().toString());
					}

					this.map = (JSONObject)eval.getReference();

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

				public Object getValue() {
					Object obj = map.get(key);
					if(obj == null) {
						return "";
					} else {
						return obj;
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

	private class Number implements com.sipstacks.script.Expression {
		Integer number;
		
		public Number(String number) {
			this.number = Integer.valueOf(number);
		}

		public Object eval() throws ScriptParseException {
			return number;
		}

		public String dump() {
			return number.toString();
		}

		@Override
		public void getFunctions(List<Function> functions) {

		}

		@Override
		public boolean complete() {
			return true;
		}
	}

	private class Rand implements com.sipstacks.script.Expression {
		Integer number;
		
		public Rand() {
		}

		public Object eval() throws ScriptParseException {
			return new Integer(random.nextInt(Integer.MAX_VALUE));
		}

		public String dump() {
			return " rand ";
		}

		@Override
		public void getFunctions(List<Function> functions) {

		}

		@Override
		public boolean complete() {
			return true;
		}
	}

	private class StringLiteral implements com.sipstacks.script.Expression {
		String string;
		
		public StringLiteral(String string) {
			this.string = string.substring(1,string.length()-1);
		}

		public Object eval() throws ScriptParseException {
			return string;
		}

		public String dump() {
		
			String result = string;
			result = result.replace("\\", "\\\\");
			result = result.replace("\"", "\\\"");
			result = result.replace("\n", "\\n");
			result = result.replace("\t", "\\t");
			return "\"" + result + "\"";
		}

		@Override
		public void getFunctions(List<Function> functions) {

		}

		@Override
		public boolean complete() {
			return true;
		}
	}

	private static class NoOP implements com.sipstacks.script.Expression, Listable {
		public Object eval() throws ScriptParseException {
			return "";
		}

		public List<Object> getList() {
			return new JSONArray();
		}

		public String dump() {
			return "";
		}

		@Override
		public void getFunctions(List<Function> functions) {

		}

		@Override
		public boolean complete() {
			return true;
		}
	}


	private class Variable implements com.sipstacks.script.Expression, Assignable {
		String name;

		public Variable(String name) {
			this.name = name;
		}

		public Object eval() throws ScriptParseException {
			if (symbolTable.peek().get(name) == null) {
				throw new ScriptParseException("Undefined variable: " + name);
			}
			return this;
		}

		public Object getValue() {
			Object obj = symbolTable.peek().get(name);
			/*
			if (obj instanceof Assignable) {
				return ((Assignable)obj).getValue();
			}*/
			return obj;
		}

		public String toString() {
			return symbolTable.peek().get(name).toString();
		}

		public void assign(Object value) throws ScriptParseException {
			if (value == null) {
				throw new ScriptParseException("Assign: attempting to assign undefined to " + name);
			}
			if (symbolTable.peek().get(name) == null) {
				throw new ScriptParseException("Undefined variable: " + name);
			}

			if (value instanceof ObjectReference || value instanceof Assignable) {
				symbolTable.peek().update(name, value);
			} else {
				symbolTable.peek().update(name, new ObjectReference(value));
			}

		}

		public String dump() {
			return name;
		}

		@Override
		public void getFunctions(List<Function> functions) {

		}

		@Override
		public boolean complete() {
			return true;
		}
	}

	private class Assign extends BinaryOperator {

		public Assign() {
			operator = "=";
		}

		public Object eval() throws ScriptParseException {
			super.eval();

			Object larg = left.eval();

			if (!(larg instanceof Assignable)) {
				throw new ScriptParseException("Assign: attempting to assign value to non-assignable type " + larg.getClass().getName());
			}
			Assignable ass = (Assignable)larg;
			Object v = right.eval();
			if (v instanceof Assignable ) {
				Assignable rass = (Assignable) v;
				ass.assign(rass.getValue());
			} else {
				ass.assign(v);
			}
			return v;
		}
	}

	private class Comma extends BinaryOperator implements Listable {

		public Comma() {
			operator = ",";
		}

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

	private com.sipstacks.script.Expression tokenToOp(String input) throws ScriptParseException {
		  if (input.equals("+")) {
			return new Positive();
		  } else if(input.equals("-")) {
			return new Negative();
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
		  } else if(input.equals("sizeof")) {
		  	return new SizeOf();
		  } else if(input.equals("keys")) {
		  	return new Keys();
		  } else if(input.startsWith("\"")) {
			  return new StringLiteral(input);
		  } else if ( input.matches("^[0-9]+")) {
			return new Number(input);
		  } else if ( input.matches("^[a-zA-Z_][a-zA-Z0-9_]*$")) {
			  // function names have precedents over variables
			  if (functionTable.containsKey(input)) {
				  Function f = functionTable.get(input);
			
				  return f.clone();
			  }
			return new Variable(input);
		  }

		  throw new ScriptParseException("Invalid Operator: '"+input+"'");
	}
	
	private static com.sipstacks.script.Expression getExpression(List<com.sipstacks.script.Expression> ops) throws ScriptParseException {
		return getExpression(ops,0,null);
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

		op = new OperationSet(false);
		op.operators.add(Not.class);
		op.operators.add(Positive.class);
		op.operators.add(Negative.class);
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

		op = new OperationSet(false);
		op.operators.add(Keys.class);
		op.operators.add(SizeOf.class);
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

	private static com.sipstacks.script.Expression getExpression(List<com.sipstacks.script.Expression> ops, int start, Class terminator) throws ScriptParseException {

		// sanity check

		if (ops.size() == 0) {
			return null;
		}
		
		// work your way backwards, only have the non-recursive part eval ()
		if(start == 0) {
			for (int i = ops.size()-1; i >= 0; i--) {
				com.sipstacks.script.Expression command = ops.get(i);
				if (command instanceof LParen) {
					((LParen)command).inner = getExpression(ops,i+1,RParen.class);
					ops.remove(i+1);
				}
				if (command instanceof LBracket) {
					((LBracket)command).right= getExpression(ops,i+1,RBracket.class);
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
					com.sipstacks.script.Expression command = ops.get(i);
					if(terminator.isInstance(command)) {
						startPos = i-1;
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
				com.sipstacks.script.Expression command = ops.get(i);


				// if start != 0 we are in a (), stop at the )

				if (start > 0 && terminator.isInstance(command)) {
					break;
				}

				for (Class clazz : opset.operators) {
					if ( clazz.isInstance(command) ) {
						if (UnaryOperator.class.isAssignableFrom(command.getClass())) {
							UnaryOperator op = (UnaryOperator)command;
							if (ops.size() == (i+1) || (start > 0 && terminator.isInstance(ops.get(i+1))) ) {
								throw new ScriptParseException(command.getClass().getName() + " expected Right hand argument expected");
							}
							if (!ops.get(i+1).complete()) {
								throw new ScriptParseException(command.getClass().getName() + " expected Right hand argument incomplete\n" + ops.get(i+1).dump() );
							}

							if (AlsoBinary.class.isAssignableFrom(command.getClass())) {
								AlsoBinary alsoBinary = (AlsoBinary)command;
								if (i > start && ops.get(i-1).complete()) {
									ops.set(i,alsoBinary.getBinary());
									continue;
								}
							}
							op.right = ops.get(i+1);
							ops.remove(i+1);
						} else if (LBracket.class.isAssignableFrom(command.getClass())) {
							// works like binary operator but we handle [] like ()
							BinaryOperator op = (BinaryOperator)command;
							if (!ops.get(i-1).complete()) {
								throw new ScriptParseException(command.getClass().getName() + " expected Left hand argument incomplete\n" + ops.get(i-1).dump() );
							}
							op.left = ops.get(i-1);
							ops.remove(i-1);
							i--;
						} else if (PostfixOperator.class.isAssignableFrom(command.getClass())) {
							PostfixOperator op = (PostfixOperator)command;
							if (!ops.get(i-1).complete()) {
								throw new ScriptParseException(command.getClass().getName() + " expected Left hand argument incomplete\n" + ops.get(i-1).dump() );
							}
							op.left = ops.get(i-1);
							ops.remove(i-1);
							i--;
						} else if (BinaryOperator.class.isAssignableFrom(command.getClass())) {
							BinaryOperator op = (BinaryOperator)command;
							ScriptParseException e = null;
							if (ops.size() == (i+1) || (start > 0 && terminator.isInstance(ops.get(i+1))) ) {
								throw new ScriptParseException(command.getClass().getName() + " expected Right hand argument after " + ops.get(i-1).dump());
							}
							if (i == start) {
								throw new ScriptParseException(command.getClass().getName() + " expected Left hand argument after " + ops.get(i+1).dump());
							}
							if (!ops.get(i-1).complete() || !ops.get(i+1).complete()) {
								throw new ScriptParseException(command.getClass().getName() + " evaluating before left & right evaluated\nleft:" + ops.get(i-1).dump()+"\nright:"+ops.get(i+1).dump());
							}

							op.left = ops.get(i - 1);
							op.right = ops.get(i + 1);
							ops.remove(i + 1);
							ops.remove(i - 1);
							i--;

						}
					}
				}
				i=opset.leftToRight?i+1:i-1;
			}
		}

		if (start > 0) {
			// check for the closest terminator to start
			if (ops.size() > (start)) {
 
				// this is the case where terminator is right after the start
				if (terminator.isInstance(ops.get(start))) {
					ops.remove(start);
					ops.add(start, new NoOP());
					return ops.get(start);
				}
			}
			if (ops.size() > (start + 1)) {
				if (terminator.isInstance(ops.get(start+1))) {
					ops.remove(start+1);
					return ops.get(start);
				}
			} 

			String list = "Ops:  in () processing: operator expected between\n";

			// if there is only one element and no terminator, then we were missing the terminator
			if (ops.size() - start == 1) {
				list = "Ops: in () processing:  missing " + terminator.getName() + " after\n";
			}

			if (start < ops.size()) {
				boolean first = true;
				for (com.sipstacks.script.Expression op : ops.subList(start, ops.size())) {
					if (!first) {
						list += "and\n";
					}
					list += op.dump() + "\n";
					first = false;

					if (terminator.isInstance(op)) {
						break;
					}
					
				}
			}
			else {
				// Do we ever hit this case?
				// if so we can't dump becaue all the args
				// might not have been evaluated yet
				boolean first = true;
				list += "Full List:\n"; 
				for (com.sipstacks.script.Expression op : ops) {
					if (!first) {
						list += "and\n";
					}
					list += op.getClass().getName() + "\n";
					first = false;
				}
			}
			throw new ScriptParseException(list);

		}


		if (ops.size() > 1 ) {
			String list = "";
			boolean first = true;
			for (com.sipstacks.script.Expression op : ops) {
				if (!first) {
					list += "and\n";
				}
				list += op.dump() + "\n";
				first = false;
			}
			throw new ScriptParseException("Ops: operator expected between\n" + list);
		}




		return ops.get(0);


	}

	private int parseInteger(String val) throws ScriptParseException {
		if (val == "") {
			return 0;
		}

		try {
			return Integer.parseInt(val);
		} catch (NumberFormatException e) {
			throw new ScriptParseException("The value '" +val+"' can not be used as a number");
		}
	}

	public static boolean parseBoolean(String val) throws ScriptParseException {
		if (val.equals("")) {
			return false;
		}

		try {
			return Integer.parseInt(val) != 0;
		} catch (NumberFormatException e) {
			// any string that is non empty and isn't a number is true
			return true;
		}
	}


	private Statement getStatement() throws ScriptParseException {
		String token = scanner.getToken();
		if (token == null) {
		  return null;
		}

		if (token.equals("local")) {
			token = scanner.getToken();
			if (token.equals("sub")) {
				return new FunctionDeclare(true);
			} else {
				throw new ScriptParseException("local keyword can only be used for sub", scanner);
			}
		}
		if (token.equals("while")) {
		  return new While();
		}
		if (token.equals("break")) {
			if (stmtCounters.numWhile > 0) {
				return new Break();
			} else {
				throw new ScriptParseException("break called outside while loop", scanner);
			}
		}
		if (token.equals("continue")) {
			if (stmtCounters.numWhile > 0) {
				return new Continue();
			} else {
				throw new ScriptParseException("continue called outside while loop", scanner);
			}
		}
		if (token.equals("return")) {
			if (stmtCounters.numSub > 0) {
				return new Return();
			} else {
				throw new ScriptParseException("return called outside a sub declariation", scanner);
			}
		}
		if (token.equals("sub")) {
		  return new FunctionDeclare();
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
		return new StatementBlock();
		}

		//System.out.println("Pushing back token '" + token + "'");
		scanner.pushBack(token);
		return new Expression();
	}

	public void addStatementFunction(String name, Statement func) {
		StatementFunction f = new StatementFunction();
		f.stmt = func;
		f.name = name;
		addFunction(f);
	}

	public void addFunction(Function func) {
		functionTable.put(func.getName(),func);
	}

	public void addFunctionListener(FunctionListener listener) {
		_functionListener = listener;
	}

	public void reset() {
		for (Function f : functionTable.values()) {
			f.reset();
		}
	}

	public OutputStream run() throws ScriptParseException {
		OutputStream result = new OutputStream();
		Statement cmd = null;

		while((cmd = getStatement())!=null){
			try {
				cmd.exec(result);
			} catch (ScriptFlowException e) {
				throw new ScriptParseException(e.getMessage());
			}
			script.add(cmd);
		}
		reset();
		return result;
	}
	
	public String dump() {
		StringBuffer result = new StringBuffer();

		for(Statement cmd : script){
			List<Function> funcs = new ArrayList<Function>();
			cmd.getFunctions(funcs);
			for(Function func : funcs) {
				if(func instanceof StatementFunction) {
					StatementFunction stmtFunc = (StatementFunction)func;
					result.append("local sub ");
					result.append(stmtFunc.name);
					result.append(stmtFunc.stmt.dump());
				}
			}
			result.append(cmd.dump());
		}
		reset();
		return result.toString();

	}

}

