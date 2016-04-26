package com.sipstacks.script;


import org.json.simple.JSONArray;

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
            if (objs.size() > 1) {
                if (objs.get(1) instanceof List) {
                    list.addAll((List<Object>) objs.get(i));
                } else {
                    list.add(objs.get(i));
                }
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
