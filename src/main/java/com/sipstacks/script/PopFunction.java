package com.sipstacks.script;


import org.json.simple.JSONValue;

import java.util.List;

public class PopFunction extends Function implements Cloneable {
    public PopFunction() {
        name = "pop";
    }

    public Object eval() throws ScriptParseException {
        super.eval();
        List<Object> objs = getParamObjects();

        if (objs.size() == 0) {
            return "pop(): list required";
        }

        Object param = objs.get(0);
        if(param instanceof Assignable) {
            Assignable assignable = (Assignable) param;
            param = assignable.getValue();
            if ( !(param instanceof List)) {
                param = JSONValue.parse(param.toString());
                if (param == null) {
                    throw new ScriptParseException("pop(): argument must be a list\nGot: " +assignable.toString());
                }
                assignable.assign(param);
            }
        }

        if (param instanceof List) {
            List<Object> objectList = (List<Object>) param;
            return  objectList.size() > 0 ? objectList.remove(objectList.size()-1): "";
        } else {
            throw new ScriptParseException("pop(): argument must be a list");
        }
    }
    public Function clone() {
        return new PopFunction();
    }


}
