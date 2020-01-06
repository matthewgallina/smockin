package com.smockin.mockserver.service;

import javax.script.ScriptException;

public interface JavaScriptResponseHandler {

    String jsEngine = "JavaScript";

    String handleResponseCaller =
            " var request = { "
            + "   pathVars : [], "
            + "   body : null, "
            + "   headers : [], "
            + "   parameters : [] "
            + " }; "
            + " "
            + " var response = { "
            + "   body : null, "
            + "   status : 200, "
            + "   contentType : 'text/plain', "
            + "   headers : [] "
            + " }; "
            + " "
            + " if (typeof handleResponse === 'function') { "
            + "   handleResponse(request, response); "
            + " } else { "
            + "   response.status = 404; "
            + "   response.body = 'mock js logic is undefined!'; "
            + "   response; "
            + " } ";

    Object execute(final String js) throws ScriptException;

}
