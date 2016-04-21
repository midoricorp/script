package com.sipstacks.script;

import java.util.List;

interface Expression {
	public Object eval() throws ScriptParseException;
	public String dump();
	public void getFunctions(List<Function> functions);
}

