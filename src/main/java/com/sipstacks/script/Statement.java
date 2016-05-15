package com.sipstacks.script;
import java.util.List;

public interface Statement {
	public void exec(OutputStream os) throws ScriptParseException, ScriptFlowException;
	public void exec(OutputStream os, List<String> arg) throws ScriptParseException, ScriptFlowException;
	public void reset();
	public String dump();
	public void getFunctions(List<Function> functions);

}

