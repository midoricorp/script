package com.sipstacks.script;


import org.json.simple.JSONValue;

import java.util.List;

public class PushFunction extends Function implements Cloneable {
    public PushFunction() {
        name = "push";
    }

    public Object eval() throws ScriptParseException {
        super.eval();
        List<Object> objs = getParamObjects();

        if (objs.size() == 0) {
            throw new ScriptParseException("push(): list required");
        }

        if (objs.size() == 1) {
            throw new ScriptParseException("push(): list required");
        }

        Object param = objs.get(0);
        if(param instanceof Assignable) {
            Assignable assignable = (Assignable) param;
            param = assignable.getValue();
        }

        if (param instanceof ObjectReference) {
            if (((ObjectReference) param).toJSON() == null) {
                throw new ScriptParseException("push(): argument must be a list\nGot: " + param.toString());
            }
            param = ((ObjectReference) param).getReference();
        }

        if ( !(param instanceof List)) {
            param = JSONValue.parse(param.toString());
            if (param == null) {
                throw new ScriptParseException("push(): argument must be a list\nGot: " +objs.get(0).toString());
            }
        }

        if (param instanceof List) {
            List<Object> objectList = (List<Object>) param;
            objectList.addAll(objs.subList(1, objs.size()));
            return  objectList;
        } else {
            throw new ScriptParseException("push(): argument must be a list");
        }
    }
    public Function clone() {
        return new PushFunction();
    }


}
