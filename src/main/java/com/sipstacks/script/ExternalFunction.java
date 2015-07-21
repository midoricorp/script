package com.sipstacks.script;
import java.util.List;

public abstract class ExternalFunction implements Command {
	
	abstract public String run(List<String> args);

	public OutputStream exec() throws ScriptParseException {
		OutputStream os = new OutputStream();
		os.appendText(run(null));
		return os;
	}
	public OutputStream exec(List<String> args) throws ScriptParseException {
		OutputStream os = new OutputStream();
		os.appendText(run(args));
		return os;
	}

	public void reset() {}

	public String dump() {
		return "**Can't dump external functions!**\n";
	}
}
