package com.sipstacks.script;
import java.util.List;
import java.util.ArrayList;

class Function extends UnaryOperator implements Cloneable {
	Command func;

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
		return func.exec(strs);
	}

	public Function clone() {
		Function f = new Function();
		f.func = func;
		return f;
	}
}
