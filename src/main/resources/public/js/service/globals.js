
app.service('globalVars', function() {

    this.AlertTimeoutMillis = 5000;
    this.FormatValidationTimeoutMillis = 10000;

    this.GeneralErrorMessage = "Oops looks like something went wrong!";
    this.AuthRequiredMessage = "Please login to access this service";
    this.AuthErrorMessage = "Invalid Login";

    this.SmockinAuthToken = "SMOCKIN_AUTH_TOKEN";
    this.PasswordResetUrl = "/reset_password.html?rt=";

    this.SysAdminRole = "SYS_ADMIN";
    this.AdminRole = "ADMIN";
    this.RegularRole = "REGULAR";

    this.RestfulServerType = "RESTFUL";
    this.JmsServerType = "JMS";
    this.FtpServerType = "FTP";

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

    this.ActiveStatus = "ACTIVE";
    this.InActiveStatus = "INACTIVE";

    this.MockServerRunningStatus = 'Running';
    this.MockServerStoppedStatus = 'Stopped';
    this.MockServerRestartStatus = 'Restarting';

    this.JsonContentType = 'application/json';
    this.XmlContentType = 'application/xml';

    this.ContentMimeTypes = [
        this.JsonContentType,
        'text/html',
        'text/plain',
        'text/css',
        this.XmlContentType
    ];

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

});
