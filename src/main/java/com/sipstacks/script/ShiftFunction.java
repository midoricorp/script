package com.sipstacks.script;


import org.json.simple.JSONArray;
import org.json.simple.JSONValue;

import java.util.ArrayList;
import java.util.List;

public class ShiftFunction extends Function implements Cloneable {
    public ShiftFunction() {
        name = "shift";
    }

    public Object eval() throws ScriptParseException {
        super.eval();
        List<Object> objs = getParamObjects();

        if (objs.size() == 0) {
            return "shift(): list required";
        }

        Object param = objs.get(0);

        if (param instanceof ObjectReference) {
            if (((ObjectReference) param).toJSON() == null) {
                throw new ScriptParseException("shift(): argument must be a list\nGot: " +param.toString());
            }
            param = ((ObjectReference) param).getReference();
        }

        if ( !(param instanceof List)) {
            param = JSONValue.parse(param.toString());
            if (param == null) {
                throw new ScriptParseException("shift(): argument must be a list\nGot: " +objs.get(0).toString());
            }
        }


        if (param instanceof List) {
            List<Object> objectList = (List<Object>) param;
            return  objectList.size() > 0 ? objectList.remove(0): "";
        } else {
            throw new ScriptParseException("shift(): argument must be a list");
        }
    }
    public Function clone() {
        return new ShiftFunction();
    }


}
