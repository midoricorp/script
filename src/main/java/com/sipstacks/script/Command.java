package com.sipstacks.script;

interface Command {
	public String exec() throws ScriptParseException;
	public String exec(String arg) throws ScriptParseException;

}

