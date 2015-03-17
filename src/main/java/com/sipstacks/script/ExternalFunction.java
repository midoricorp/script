package com.sipstacks.script;
import java.util.List;

public abstract class ExternalFunction implements Command {
	
	abstract public String run(List<String> args);

	public String exec() throws ScriptParseException {
		return run(null);
	}
	public String exec(List<String> args) throws ScriptParseException {
		return run(args);
	}
}
