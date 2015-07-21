package com.sipstacks.script;
import java.util.List;

public interface Command {
	public OutputStream exec() throws ScriptParseException;
	public OutputStream exec(List<String> arg) throws ScriptParseException;
	public void reset();
	public String dump();

}

