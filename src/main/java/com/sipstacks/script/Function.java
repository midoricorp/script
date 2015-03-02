package com.sipstacks.script;

class Function extends UnaryOperator implements Cloneable {
	Command func;

	public String eval() throws ScriptParseException {
		super.eval();
		return func.exec(right.eval());
	}

	public Function clone() {
		Function f = new Function();
		f.func = func;
		return f;
	}
}
