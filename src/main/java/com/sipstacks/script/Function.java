package com.sipstacks.script;

class Function extends UnaryOperator implements Cloneable {
	Command func;

	public Object eval() throws ScriptParseException {
		super.eval();
		if (right instanceof Listable) {
			return func.exec(((Listable)right).getList().toString());
		}
		return func.exec(right.eval().toString());
	}

	public Function clone() {
		Function f = new Function();
		f.func = func;
		return f;
	}
}
