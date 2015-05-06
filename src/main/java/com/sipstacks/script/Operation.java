package com.sipstacks.script;

interface Operation {
	public Object eval() throws ScriptParseException;
	public String dump();
}

