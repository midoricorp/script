package com.sipstacks.script;

/**
 * Created by torrey on 15/05/16.
 */
public class ScriptBreakFlowException extends ScriptFlowException {
    public ScriptBreakFlowException() {
        super("Break called outside of a while!");
    }
}
