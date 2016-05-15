package com.sipstacks.script;

/**
 * Created by torrey on 15/05/16.
 */
public class ScriptReturnFlowException extends ScriptFlowException {
    public ScriptReturnFlowException() {
        super("Return called outside of a SUB!");
    }
}
