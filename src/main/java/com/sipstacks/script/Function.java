package com.sipstacks.script;
import java.util.List;
import java.util.ArrayList;

public class Function extends UnaryOperator implements Cloneable {
	String name;

	public String getName() {
		return name;
	}

	public void getFunctions(List<Function> functions) {
		if (right != null) {
			right.getFunctions(functions);
		}
		functions.add(this);
	}

	public Function clone() {
		Function f = new Function();
		f.name = name;
		return f;
	}

	public String dump() {
		String rightstr = right==null?"()":right.dump();
		return name + " " + rightstr;
	}

	public void reset() {
	}

	public List<String> getParamStrings() throws ScriptParseException {
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
		return strs;
	}

	public List<Object> getParamObjects() throws ScriptParseException {
		List<Object> objs;
		if (right instanceof Listable) {
			objs = ((Listable)right).getList();
		} else {
			Object obj = right.eval();
			if (obj instanceof List) {
				objs = (List<Object>)obj;
			} else {
				objs = new ArrayList<Object>();
				objs.add(obj);
			}
		}
		return objs;
	}
}
