package com.sipstacks.script;

/**
 * Created by torrey on 15/05/16.
 */
public class ScriptContinueFlowException extends ScriptFlowException {
    public ScriptContinueFlowException() {
        super("Break called outside of a while!");
    }
}
