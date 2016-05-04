package com.sipstacks.script;


import org.json.simple.JSONArray;
import org.json.simple.JSONValue;

import java.util.ArrayList;
import java.util.List;

public class JoinFunction extends Function implements Cloneable {
    public JoinFunction() {
        name = "join";
    }

    public Object eval() throws ScriptParseException {
        super.eval();
        List<Object> objs = getParamObjects();

        String delimiter  = null;
        List<Object> list = new ArrayList<Object>();
        if (objs.size() == 0) {
            return "join(): must specify delimiter";
        }

        delimiter = objs.get(0).toString();

        for (int i = 1; i < objs.size(); i++) {
            Object o = objs.get(i);
            if (o instanceof Assignable) {
                Assignable assignable = ((Assignable) o);
                o = assignable.getValue();
                if (!(o instanceof List)) {
                    Object listObject = JSONValue.parse(o.toString());
                    if (listObject != null) {
                        assignable.assign(listObject);
                        o = listObject;
                    }
                }
            }
            if (o instanceof List && i == 1) {
                // if 2nd parameter is a list, just use that
                list.addAll((List<Object>) o);
                break;
            } else {
                // else treat the remaining parameters as the list
                 list.add(o);
            }
        }


        boolean first = true;
        StringBuffer result = new StringBuffer();

        for (Object o : list) {
            if (!first) {
                result.append(delimiter);
            }
            first = false;
            result.append(o.toString());
        }
        return result.toString();
    }
    public Function clone() {
        return new JoinFunction();
    }


}
