
app.service('globalVars', function() {

    this.AlertTimeoutMillis = 5000;
    this.FormatValidationTimeoutMillis = 10000;

    this.GeneralErrorMessage = "Oops looks like something went wrong!";
    this.AuthRequiredMessage = "Please login to access this service";
    this.AuthErrorMessage = "Invalid Login";

    this.SmockinAuthToken = "SMOCKIN_AUTH_TOKEN";
    this.PasswordResetUrl = "/reset_password.html?rt=";

    this.HttpServerMode = "HTTP";
    this.S3ServerMode = "S3";
    this.MailServerMode = "MAIL";

    this.SysAdminRole = "SYS_ADMIN";
    this.AdminRole = "ADMIN";
    this.RegularRole = "REGULAR";

    this.SyncModeNone = 'NO_SYNC';
    this.SyncModeOneWay = 'ONE_WAY';
    this.SyncModeBiDirectional = 'BI_DIRECTIONAL';

    this.SyncModeNoneLabel = 'NO SYNC';
    this.SyncModeOneWayLabel = 'ONE WAY';
    this.SyncModeBiDirectionalLabel = 'BI-DIRECTIONAL';

    this.RestfulServerType = "RESTFUL";
    this.S3ServerType = "S3";
    this.MailServerType = "MAIL";

    this.TextDataType = 'TEXT';
    this.NumericDataType = 'NUMERIC';
    this.PathVariableText = 'PATH_VARIABLE';
    this.PathVariableWildcardText = 'PATH_VARIABLE_WILD';
    this.RequestHeaderText = 'REQUEST_HEADER';
    this.RequestParamText = 'REQUEST_PARAM';
    this.RequestBodyText = 'REQUEST_BODY';
    this.RequestBodyJsonAnyFieldText = 'REQUEST_BODY_JSON_ANY';
    this.EqualsText = 'EQUALS';
    this.ContainsText = 'CONTAINS';
    this.IsMissingText = 'IS_MISSING';

    this.NodeTypeBucket = 'BUCKET';
    this.NodeTypeDir = 'DIR';
    this.NodeTypeFile = 'FILE';
    this.S3BucketNameRegex = /(?=^.{3,63}$)(?!^(\d+\.)+\d+$)(^(([a-z0-9]|[a-z0-9][a-z0-9\-]*[a-z0-9])\.)*([a-z0-9]|[a-z0-9][a-z0-9\-]*[a-z0-9])$)/gm;

    this.ActiveStatus = "ACTIVE";
    this.InActiveStatus = "INACTIVE";

    this.MockServerRunningStatus = 'Running';
    this.MockServerStoppedStatus = 'Stopped';
    this.MockServerRestartStatus = 'Restarting';

    this.JsonContentType = 'application/json';
    this.XmlContentType = 'application/xml';

    this.CoreDashboardAlertBroadcast = 'CORE_DASHBOARD_ALERT_BROADCAST';

    this.ContentMimeTypes = [
        this.JsonContentType,
        'text/html',
        'text/plain',
        'text/css',
        this.XmlContentType
    ];

    this.httpMethods = [
        'GET',
        'POST',
        'PUT',
        'DELETE',
        'PATCH'
    ];

    this.MockTypeDefinitions = {
        MockTypeSeq : 'SEQ',
        MockTypeRule : 'RULE',
        MockTypeProxyHttp : 'PROXY_HTTP',
        MockTypeWebSocket : 'PROXY_WS',
        MockTypeProxySse : 'PROXY_SSE',
        MockTypeCustomJs : 'CUSTOM_JS',
        MockTypeRuleWs : 'RULE_WS',
        MockTypeStateful : 'STATEFUL'
    };

    this.RuleComparators = [
       { dropDownName : 'Equals ( TEXT )', tableName : 'equals', value : this.EqualsText, dataType: this.TextDataType },
       { dropDownName : 'Equals ( NUMBER )', tableName : '==', value : this.EqualsText, dataType: this.NumericDataType },
       { dropDownName : 'Contains ( TEXT )', tableName : 'contains', value : this.ContainsText, dataType: this.TextDataType },
       { dropDownName : 'Is Missing', tableName : 'is missing', value : this.IsMissingText, dataType: this.TextDataType }
    ];

    this.RuleMatchingTypes = [
        { name : 'Path Variable', value : this.PathVariableText, fieldPlaceholderText : 'Enter \'Path Variable\' Key Name' },
        { name : 'Path Variable (Wildcard)', value : this.PathVariableWildcardText, fieldPlaceholderText : 'Position of \'Wildcard\' var (e.g for /home/*/pet/* enter 1 or 2)' },
        { name : 'Request Header', value : this.RequestHeaderText, fieldPlaceholderText : 'Enter \'Request Header\' Key Name' },
        { name : 'Request Parameter', value : this.RequestParamText, fieldPlaceholderText : 'Enter \'Request Parameter\' Key Name' },
        { name : 'Request Body', value : this.RequestBodyText, fieldPlaceholderText : '' },
        { name : 'Request Body JSON Field', value : this.RequestBodyJsonAnyFieldText, fieldPlaceholderText : 'Enter \'JSON Parameter\' Key Name'  }
    ];

    this.WebSocketRuleMatchingTypes = [
        { name : 'Request Body', value : this.RequestBodyText, fieldPlaceholderText : '' },
        { name : 'Request Body JSON Field', value : this.RequestBodyJsonAnyFieldText, fieldPlaceholderText : 'Enter \'JSON Parameter\' Key Name'  }
    ];

});
