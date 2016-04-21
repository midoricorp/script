package com.sipstacks.script;

interface Expression {
	public Object eval() throws ScriptParseException;
	public String dump();
}

