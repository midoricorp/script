import java.io.PushbackReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Hashtable;
import java.util.ArrayList;
import java.util.List;

public class Script{
  

	static Hashtable <String, String> symbolTable = new Hashtable<String,String>();

	private interface Command {
		public String exec();

	}

	private static class Expression implements Command {
		Operation op;

		public Expression(Operation op) {
			this.op = op;
		}

		public String exec() {
			op.eval();
			return "";
		}

		public static Command parse(PushbackReader br) throws IOException {
			  ArrayList<Operation> ops = new ArrayList<Operation>();
			  String input = null;
			  while((input=getToken(br))!=null){
				  System.out.println("Got token '"+input+"'");
				  if (input.equals(";")) {
					Operation  op = getOperation(ops);
					return new Expression(op);
				  }
				  ops.add(tokenToOp(input));


			  }

			  return null;

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

		public String exec() {
			StringBuffer sb = new StringBuffer();
			if (Integer.parseInt(op.eval()) != 0) {
				sb.append(cmd.exec());
			}
			else if (else_cmd != null) {
				sb.append(else_cmd.exec());
			}

			return sb.toString();
		}

		public static Command parse(PushbackReader br) throws IOException {

			Operation op = null;
			Command cmd = null;
			Command else_cmd = null;

			String token;
			token = getToken(br);

			if ( !token.equals("(") ) {
				System.err.println("If: Expected '('");
				return null;
			}

			ArrayList<Operation> ops = new ArrayList<Operation>();

			while (true) {
				token = getToken(br);
				if (token == null ) {
					System.err.println("If: unexpected eof on condition");
				}

				if (token.equals(")")) {
					op = getOperation(ops);			
					break;
				}
				ops.add(tokenToOp(token));
			}

			cmd = getCommand(br);
			if (cmd == null) {
				System.err.println("If: missing command");
				return null;
			}

			token = getToken(br);

			if ( token.equals("else") ) {
				else_cmd = getCommand(br);
			} else {
				br.unread(' ');
				br.unread(token.toCharArray());
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

		public String exec() {
			StringBuffer sb = new StringBuffer();
			while (Integer.parseInt(op.eval()) != 0) {
				sb.append(cmd.exec());
			}
			return sb.toString();
		}

		public static Command parse(PushbackReader br) throws IOException {

			Operation op = null;
			Command cmd = null;

			String token;
			token = getToken(br);

			if ( !token.equals("(") ) {
				System.err.println("While: Expected '('");
				return null;
			}

			ArrayList<Operation> ops = new ArrayList<Operation>();

			while (true) {
				token = getToken(br);
				if (token == null ) {
					System.err.println("While: unexpected eof on condition");
				}

				if (token.equals(")")) {
					op = getOperation(ops);			
					break;
				}
				ops.add(tokenToOp(token));
			}

			cmd = getCommand(br);
			if (cmd == null) {
				System.err.println("While: missing command");
				return null;
			}

			return new While(op, cmd);
		}
	}

	private static class CommandBlock implements Command {
		ArrayList<Command> commands;

		public CommandBlock(ArrayList<Command> commands) {
			this.commands = commands;
		}

		public String exec() {
			StringBuffer sb = new StringBuffer();
			for (Command cmd : commands) {
				sb.append(cmd.exec());
			}
			return sb.toString();
		}

		public static Command parse(PushbackReader br) throws IOException {
			ArrayList<Command> commands = new ArrayList<Command>();


			String token;
			token = getToken(br);

			if ( !token.equals("{") ) {
				System.err.println("Block: Expected '{'");
				return null;
			}


			while (true) {
				token = getToken(br);
				if (token == null ) {
					System.err.println("Block: unexpected eof while expecting }");
				}

				if (token.equals("}")) {
					break;
				}
				br.unread(' ');
				br.unread(token.toCharArray());
				Command cmd = getCommand(br);
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

		public String exec() {
			return op.eval() + "\n";
		}

		public static Command parse(PushbackReader br) throws IOException {

			Operation op = null;

			ArrayList<Operation> ops = new ArrayList<Operation>();

			while (true) {
				String token = getToken(br);
				if (token == null ) {
					System.err.println("Print: unexpected eof on condition");
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
		public String eval();
	}

	private static class Add implements Operation {
		Operation left;
		Operation right;
		public String eval() {
			return Integer.toString(Integer.parseInt(left.eval()) + Integer.parseInt(right.eval()));
		}
	}

	private static class Subtract implements Operation {
		Operation left;
		Operation right;
		public String eval() {
			return Integer.toString(Integer.parseInt(left.eval()) - Integer.parseInt(right.eval()));
		}
	}

	private static class Multiply implements Operation {
		Operation left;
		Operation right;
		public String eval() {
			return Integer.toString(Integer.parseInt(left.eval()) * Integer.parseInt(right.eval()));
		}
	}

	private static class Divide implements Operation {
		Operation left;
		Operation right;
		public String eval() {
			return Integer.toString(Integer.parseInt(left.eval()) / Integer.parseInt(right.eval()));
		}
	}

	private static class Modulo implements Operation {
		Operation left;
		Operation right;
		public String eval() {
			return Integer.toString(Integer.parseInt(left.eval()) % Integer.parseInt(right.eval()));
		}
	}

	private static class GreaterThan implements Operation {
		Operation left;
		Operation right;
		public String eval() {
			return Integer.parseInt(left.eval()) > Integer.parseInt(right.eval())?"1":"0";
		}
	}

	private static class LessThan implements Operation {
		Operation left;
		Operation right;
		public String eval() {
			return Integer.parseInt(left.eval()) < Integer.parseInt(right.eval())?"1":"0";
		}
	}

	private static class Equals implements Operation {
		Operation left;
		Operation right;
		public String eval() {
			return left.eval().equals(right.eval())?"1":"0";
		}
	}

	private static class NotEquals implements Operation {
		Operation left;
		Operation right;
		public String eval() {
			return left.eval().equals(right.eval())?"0":"1";
		}
	}

	private static class Not implements Operation {
		Operation right;
		public String eval() {
			return Integer.parseInt(right.eval()) == 0?"1":"0";
		}
	}

	private static class Concat implements Operation {
		Operation left;
		Operation right;
		public String eval() {
			return left.eval()+right.eval();
		}
	}

	private static class Number implements Operation {
		String number;
		
		public Number(String number) {
			this.number = number;
		}

		public String eval() {
			return number;
		}
	}

	private static class StringLiteral implements Operation {
		String string;
		
		public StringLiteral(String string) {
			this.string = string.substring(1,string.length()-1);
		}

		public String eval() {
			return string;
		}
	}


	private static class Variable implements Operation {
		String name;

		public Variable(String name) {
			this.name = name;
		}

		public String eval() {
			if (symbolTable.get(name) == null) {
				symbolTable.put(name, "0");
			}
			return symbolTable.get(name);
		}

		public void assign(String value) {
			symbolTable.put(name, value);
		}
	}

	private static class Assign implements Operation {
		Variable var;
		Operation value;
		public String eval() {
			String v = value.eval();
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
	
	private static Operation getOperation(List<Operation> ops) {


		// assignment order ! * / + - < > == != . =
		for (int i = 0; i < ops.size(); i++) {
			Operation command = ops.get(i);
			if ( command instanceof Not) {

				Not op = (Not)command;
				
				op.right = ops.get(i+1);
				ops.remove(i+1);
			}
		}

		for (int i = 0; i < ops.size(); i++) {
			Operation command = ops.get(i);
			if ( command instanceof Multiply ) {

				Multiply op = (Multiply)command;
				
				op.left = ops.get(i-1);
				op.right = ops.get(i+1);
				ops.remove(i+1);
				ops.remove(i-1);
				i--;
			}
		}

		for (int i = 0; i < ops.size(); i++) {
			Operation command = ops.get(i);
			if ( command instanceof Divide ) {
				
				Divide op = (Divide)command;
				op.left = ops.get(i-1);
				op.right = ops.get(i+1);
				ops.remove(i+1);
				ops.remove(i-1);
				i--;
			}
		}

		for (int i = 0; i < ops.size(); i++) {
			Operation command = ops.get(i);
			if ( command instanceof Modulo ) {
				
				Modulo op = (Modulo)command;
				op.left = ops.get(i-1);
				op.right = ops.get(i+1);
				ops.remove(i+1);
				ops.remove(i-1);
				i--;
			}
		}

		for (int i = 0; i < ops.size(); i++) {
			Operation command = ops.get(i);
			if ( command instanceof Add ) {

				Add op = (Add)command;
				op.left = ops.get(i-1);
				op.right = ops.get(i+1);
				ops.remove(i+1);
				ops.remove(i-1);
				i--;
			}
		}

		for (int i = 0; i < ops.size(); i++) {
			Operation command = ops.get(i);
			if ( command instanceof Subtract ) {
				
				Subtract op = (Subtract)command;
				op.left = ops.get(i-1);
				op.right = ops.get(i+1);
				ops.remove(i+1);
				ops.remove(i-1);
				i--;
			}
		}

		for (int i = 0; i < ops.size(); i++) {
			Operation command = ops.get(i);
			if ( command instanceof Concat) {
				
				Concat op = (Concat)command;
				op.left = ops.get(i-1);
				op.right = ops.get(i+1);
				ops.remove(i+1);
				ops.remove(i-1);
				i--;
			}
		}

		for (int i = 0; i < ops.size(); i++) {
			Operation command = ops.get(i);
			if ( command instanceof LessThan ) {
				
				LessThan op = (LessThan)command;
				op.left = ops.get(i-1);
				op.right = ops.get(i+1);
				ops.remove(i+1);
				ops.remove(i-1);
				i--;
			}
		}

		for (int i = 0; i < ops.size(); i++) {
			Operation command = ops.get(i);
			if ( command instanceof GreaterThan ) {
				
				GreaterThan op = (GreaterThan)command;
				op.left = ops.get(i-1);
				op.right = ops.get(i+1);
				ops.remove(i+1);
				ops.remove(i-1);
				i--;
			}
		}

		for (int i = 0; i < ops.size(); i++) {
			Operation command = ops.get(i);
			if ( command instanceof Equals) {
				
				Equals op = (Equals)command;
				op.left = ops.get(i-1);
				op.right = ops.get(i+1);
				ops.remove(i+1);
				ops.remove(i-1);
				i--;
			}
		}

		for (int i = 0; i < ops.size(); i++) {
			Operation command = ops.get(i);
			if ( command instanceof NotEquals) {
				
				NotEquals op = (NotEquals)command;
				op.left = ops.get(i-1);
				op.right = ops.get(i+1);
				ops.remove(i+1);
				ops.remove(i-1);
				i--;
			}
		}

		for (int i = 0; i < ops.size(); i++) {
			Operation command = ops.get(i);
			if ( command instanceof Assign ) {
				Assign op = (Assign)command;
				
				op.var = (Variable)ops.get(i-1);
				op.value = ops.get(i+1);
				ops.remove(i+1);
				ops.remove(i-1);
				i--;
			}
		}


		if (ops.size() > 1 ) {
			System.err.println("Ops: incomplete reduction ops.size=" +ops.size());
		}




		return ops.get(0);


	}

  private static String getToken(PushbackReader br) throws IOException {
	  StringBuffer sb = new StringBuffer();

	  boolean skipWhiteSpace = true;
	  boolean inQuotes = false;

	  int input = 0;

	  while ((input = br.read()) != -1) {
		  // end of stream
		  if (input == -1) {
			  break;
		  }

		  char c = (char)input;

		  if (skipWhiteSpace && 
			(c == ' ' || c == '\t' || c == '\n')) {
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
				  if ((input = br.read()) != -1) {
					  char c2 = (char)input;
					  if (c2 == '=') {
						  sb.append(c2);
					  }
					  else {
						  // not a double char
						  br.unread(input);
					  }
				  }
				  return sb.toString();
			  }

			  // we still have a token that needs to be returned
			  // pushback and return that
			  br.unread(input);
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
				  if (c == '=') {

					  if ((input = br.read()) != -1) {
						  char c2 = (char)input;
						  if (c == c2) {
							  sb.append(c2);
						  }
						  else {
							  // not a double char
							  br.unread(input);
						  }
					  }
				  }
				  return sb.toString();
			  }

			  // we still have a token that needs to be returned
			  // pushback and return that
			  br.unread(input);
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


	  // hit end of stream, return last token or null
	  if (sb.length() > 0) {
		  return sb.toString();
	  } else {
		  return null;
	  }

  }


  private static Command getCommand(PushbackReader br) throws IOException {
	  String token = getToken(br);
	  if (token == null) {
		  return null;
	  }

	  if (token.equals("while")) {
		  return While.parse(br);
	  }
	  if (token.equals("if")) {
		  return If.parse(br);
	  }
	  if (token.equals("print")) {
		  return Print.parse(br);
	  }
	  if (token.equals("{")) {
	  	br.unread(token.toCharArray());
	        return CommandBlock.parse(br);
	  }

	  System.out.println("Pushing back token '" + token + "'");
	  br.unread(token.toCharArray());
	  return Expression.parse(br);
  }

  public static void main(String args[]) {
	try{
		PushbackReader br = 
                      new PushbackReader(new InputStreamReader(System.in), 10);
 
 
		Command cmd = null;
		while((cmd = getCommand(br))!=null){
			System.out.println(cmd.exec());
		}
 
	}catch(IOException io){
		io.printStackTrace();
	}	
  }
}

