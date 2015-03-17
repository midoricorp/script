package com.sipstacks.script;
import java.util.List;

interface Command {
	public String exec() throws ScriptParseException;
	public String exec(List<String> arg) throws ScriptParseException;

}

