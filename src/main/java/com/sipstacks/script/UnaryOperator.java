package com.sipstacks.script;

abstract class UnaryOperator implements Expression {
	Expression right;
	String operator;

	public Object eval() throws ScriptParseException {
		if (right == null) {
			throw new ScriptParseException(this.getClass().getName() +": Missing right arg");
		}

		// must override to have proper return val
		return null;
	}

	public String dump() {
		String rightstr = right==null?"undefined":right.dump();
		return operator + " " + rightstr;
	}

}
