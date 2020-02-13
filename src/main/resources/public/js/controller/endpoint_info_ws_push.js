
app.controller('endpointInfoWsPushController', function($scope, $uibModalInstance, $http, $timeout, utils, globalVars, data, auth) {

    //
    // Constants
    var isNew = (data.rule == null);
    var AlertTimeoutMillis = globalVars.AlertTimeoutMillis;
    var FormatValidationTimeoutMillis = globalVars.FormatValidationTimeoutMillis;

    $scope.JsonContentType = globalVars.JsonContentType;
    $scope.XmlContentType = globalVars.XmlContentType;
    $scope.MockTypeRuleWs = globalVars.MockTypeDefinitions.MockTypeRuleWs;


    //
    // Labels
    $scope.newEndpointPushHeading = ( (!isNew)?'View':'New' ) + ' WS Periodic Response';
    $scope.contentTypeLabel = 'Content Type';
    $scope.contentTypePlaceholderTxt = 'e.g. (application/json)';
    $scope.responseBodyLabel = 'Response Body';
    $scope.formatJsonLabel = 'Validate & Format JSON';
    $scope.formatXmlLabel = 'Validate & Format XML';
    $scope.labelTriggerType = 'Trigger Type';
    $scope.optionLabelClock = 'Clock';
    $scope.clockTriggerValue = 'Trigger Value (ms)';
    $scope.triggerValuePlaceholderTxt = 1000;


    //
    // Buttons
    $scope.cancelButtonLabel = 'Cancel';
    $scope.saveButtonLabel = (!isNew)?'Amend':'Add';

    //
    // Alerts
    $scope.alerts = [];

    var closeAlertFunc = function() {
        $scope.alerts = [];
    };

   function showAlert(msg, type) {

        if (type == null) {
            type = 'danger';
        }

        $scope.alerts = [];
        $scope.alerts.push({ "type" : type, "msg" : msg });

        $timeout(closeAlertFunc, AlertTimeoutMillis);
    }

    $scope.closeAlert = closeAlertFunc;


    //
    // Data Objects
    $scope.contentTypes = globalVars.ContentMimeTypes;

    $scope.responseHeaderList = [];

    $scope.ruleResponse = {
        "extId" : null,
        "orderNo" : 0,
        "responseContentType" : null,
        "httpStatusCode" : 200,
        "responseBody" : null,
        "sleepInMillis" : 0,
        "responseHeaders" : {
        	"0": {"name":"type","value":"clock" },
        	"1": {"name":"value","value":1000 }
        }
    };

    if (!isNew) {
        $scope.ruleResponse = data.rule;
        
    }
    $scope.isNew = isNew;
    $scope.readOnly = (!isNew && auth.isLoggedIn() && auth.getUserName() != data.createdBy);
    $scope.mockType = data.mockType;


    //
    // Scoped Functions
    $scope.doFormatJson = function() {

        $scope.closeAlert();

        if ($scope.ruleResponse.responseBody == null) {
            return;
        }

        var validationOutcome = utils.validateJson($scope.ruleResponse.responseBody);

        if (validationOutcome != null) {
            showAlert(validationOutcome, 'danger', FormatValidationTimeoutMillis);
            return;
        }

        $scope.ruleResponse.responseBody = utils.formatJson($scope.ruleResponse.responseBody);
    };

    $scope.doFormatXml = function() {

        $scope.closeAlert();

        if ($scope.ruleResponse.responseBody == null) {
            return;
        }

        var validationOutcome = utils.validateAndFormatXml($scope.ruleResponse.responseBody);

        if (validationOutcome == null) {
            showAlert("Unable to format XML. Invalid syntax", 'danger', FormatValidationTimeoutMillis);
            return;
        }

        if (validationOutcome[0] == 'ERROR') {
            showAlert("Unable to format XML: " + validationOutcome[1], 'danger', FormatValidationTimeoutMillis);
            return;
        }

        $scope.ruleResponse.responseBody = validationOutcome[1];
    };

    $scope.doSaveRule = function() {

        if (utils.isBlank($scope.ruleResponse.responseContentType)) {
            showAlert("'Content Type' is required");
            return;
        }
        
        if (utils.isBlank($scope.ruleResponse.responseBody)) {
            showAlert("'Body' is required");
            return;
        }

        if (utils.isBlank($scope.ruleResponse.httpStatusCode)
                || !utils.isNumeric($scope.ruleResponse.httpStatusCode)) {
            showAlert("'Http Status Code' is required and must be numeric");
            return;
        }
        if ($scope.ruleResponse.responseHeaders['0'] && $scope.ruleResponse.responseHeaders['1']) {
        	if ($scope.ruleResponse.responseHeaders['0'].value === undefined) {
        		showAlert("'Trigger Type' is required");
                return;
        	}
        	if ($scope.ruleResponse.responseHeaders['0'].value === 'clock' && $scope.ruleResponse.responseHeaders['1'].value < 100) {
        		showAlert("'Clock' value must be greater than 100ms");
                return;
        	}
        }
        

        $uibModalInstance.close($scope.ruleResponse);
    };

    $scope.doCancel = function() {
        $uibModalInstance.dismiss('cancel');
    };


});
