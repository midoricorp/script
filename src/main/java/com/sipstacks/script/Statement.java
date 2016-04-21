package com.sipstacks.script;
import java.util.List;

public interface Statement {
	public OutputStream exec() throws ScriptParseException;
	public OutputStream exec(List<String> arg) throws ScriptParseException;
	public void reset();
	public String dump();

}

