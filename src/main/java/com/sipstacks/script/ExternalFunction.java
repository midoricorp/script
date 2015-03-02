package com.sipstacks.script;

public abstract class ExternalFunction implements Command {
	
	abstract public String run(String args);

	public String exec() throws ScriptParseException {
		return run(null);
	}
	public String exec(String args) throws ScriptParseException {
		return run(args);
	}
}
