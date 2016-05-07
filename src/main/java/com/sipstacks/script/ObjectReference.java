package com.sipstacks.script;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.util.List;
import java.util.Map;

/**
 * Created by torrey on 07/05/16.
 */
public class ObjectReference {
    private Object obj;

    public ObjectReference(Object obj) {
        this.obj = obj;
    }

    public Object getReference() {
        return obj;
    }

    public String toString() {
        return this.obj.toString();
    }

    public Object toJSON() {
        if(obj instanceof Map ||
                obj instanceof List) {
            return obj;
        }
        Object newobj = JSONValue.parse(obj.toString());
        if(newobj != null) {
            obj = newobj;
        }
        return newobj;
    }
}
