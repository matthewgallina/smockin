
var SMOCKIN_DEMO_SESSION_TOKEN = "SMOCKIN_DEMO_SESSION_TOKEN";

function isBlank(value) {
    return (value == null || value.length == 0 || value.trim().length == 0);
}


function getAuthSession() {
    return sessionStorage.getItem(SMOCKIN_DEMO_SESSION_TOKEN);
}

function setAuthSession(session) {
    sessionStorage.setItem(SMOCKIN_DEMO_SESSION_TOKEN, session);
}

function clearAuthSession() {
    sessionStorage.removeItem(SMOCKIN_DEMO_SESSION_TOKEN);
}
