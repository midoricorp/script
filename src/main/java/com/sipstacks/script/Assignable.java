package com.sipstacks.script;

interface Assignable {
	public void assign(Object value) throws ScriptParseException;
	public Object getValue();
}
