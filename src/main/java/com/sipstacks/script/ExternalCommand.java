package com.sipstacks.script;
import java.util.List;

public abstract class ExternalCommand implements Statement {
	
	abstract public String run(List<String> args);

	public void exec(OutputStream os) throws ScriptParseException, ScriptFlowException {
		os.appendText(run(null));
	}
	public void exec(OutputStream os, List<String> args) throws ScriptParseException, ScriptFlowException {
		os.appendText(run(args));
	}

	public void reset() {}

	public String dump() {
		return "**Can't dump external functions!**\n";
	}

	@Override
	public void getFunctions(List<Function> functions) {

	}
}
