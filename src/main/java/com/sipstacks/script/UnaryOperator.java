package com.sipstacks.script;

abstract class UnaryOperator implements Operation {
	Operation right;
	String operator;

	public Object eval() throws ScriptParseException {
		if (right == null) {
			throw new ScriptParseException(this.getClass().getName() +": Missing right arg");
		}

		// must override to have proper return val
		return null;
	}

	public String dump() {
		return operator + " " + right.dump() + " ";
	}

}
