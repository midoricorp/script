package com.sipstacks.script;
import java.util.List;
import java.util.ArrayList;

public class Function extends UnaryOperator implements Cloneable {
	String name;
	Statement stmt;

	public String getName() {
		return name;
	}

	public Statement getStatement() {
		return stmt;
	}

	@Override
	public void getFunctions(List<Function> functions) {
		if (right != null) {
			right.getFunctions(functions);
		}
		functions.add(this);
	}

	public Object eval() throws ScriptParseException {
		super.eval();
		List<String> strs = new ArrayList<String>();
		if (right instanceof Listable) {
			List<Object> objs = ((Listable)right).getList();
			for (Object obj : objs) {
				strs.add(obj.toString());
			}
		} else {
			Object obj = right.eval();
			if (obj instanceof List) {
				List<Object> list = (List<Object>)obj;
				for (Object item : list) {
					strs.add(item.toString());
				}
			} else {
				strs.add(obj.toString());
			}
		}
		return stmt.exec(strs);
	}

	public Function clone() {
		Function f = new Function();
		f.stmt = stmt;
		f.name = name;
		return f;
	}

	public String dump() {
		String rightstr = right==null?"()":right.dump();
		return name + " " + rightstr;
	}
}
