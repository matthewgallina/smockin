package com.smockin.mockserver.service;

import org.springframework.stereotype.Service;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

@Service
public class JavaScriptResponseHandlerImpl implements JavaScriptResponseHandler {

    public Object execute(final String js) throws ScriptException {
        return buildEngine().eval(js);
    }

    private ScriptEngine buildEngine() {
        final ScriptEngineManager factory = new ScriptEngineManager();
        return factory.getEngineByName(jsEngine);
    }

}
