package com.sipstacks.script;

public class ScriptParseException extends Exception {
	ScriptScanner ss;
	public ScriptParseException(String exe) {
		super(exe);
		ss = null;
	}

	public ScriptParseException(String exe, ScriptScanner ss) {
		super(exe);
		this.ss = ss;
	}

	public String getMessage() {
		String msg = super.getMessage();

		if (ss != null) {
			msg = ss.getLocation() + "\n" + msg;
		}
		return msg;
	}
}

