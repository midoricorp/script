package com.sipstacks.script;
import java.util.List;

interface Listable {
	public List<Object> getList() throws ScriptParseException;
}
