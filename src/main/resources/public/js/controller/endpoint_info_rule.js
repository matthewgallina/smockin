
app.controller('endpointInfoRuleController', function($scope, $location, $uibModal, $uibModalInstance, $http, $timeout, utils, globalVars, data, auth) {

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
    $scope.newEndpointRuleHeading = ( (!isNew)?'View':'New' ) + ' Rule';
    $scope.contentTypeLabel = 'Content Type';
    $scope.contentTypePlaceholderTxt = 'e.g. (application/json)';
    $scope.httpStatusCodeLabel = 'HTTP Status Code';
    $scope.httpStatusCodePlaceholderTxt = 'e.g. (200, 201, 404)';
    $scope.responseBodyLabel = 'Response Body';
    $scope.ruleConditionsTableLabel = "Rule Conditions";
    $scope.responseHeadersLabel = 'Response Headers';
    $scope.responseHeaderNameLabel = 'Name';
    $scope.responseHeaderValueLabel = 'Value';
    $scope.formatJsonLabel = 'Validate & Format JSON';
    $scope.formatXmlLabel = 'Validate & Format XML';


    //
    // Buttons
    $scope.cancelButtonLabel = 'Cancel';
    $scope.saveButtonLabel = (!isNew)?'Amend Rule':'Add Rule';
    $scope.addConditionButtonLabel = 'Add Rule Condition';
    $scope.removeResponseHeaderButtonLabel = 'X';
    $scope.addResponseHeaderButtonLabel = 'New Row';


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
        "responseHeaders" : {},
        "groups" : []
    };

    if (!isNew) {
        $scope.ruleResponse = data.rule;

        angular.forEach($scope.ruleResponse.responseHeaders, function(v, k) {
            $scope.responseHeaderList.push({ 'name' : k, 'value' : v });
        });

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

    $scope.doAddResponseHeaderRow = function() {
        $scope.responseHeaderList.push({ "name" : null, "value" : null });
    };

    $scope.doRemoveResponseHeaderRow = function(index) {
        $scope.responseHeaderList.splice(index, 1);
    };

    $scope.doSaveRule = function() {

        $scope.ruleResponse.responseHeaders = {};

        if (utils.isBlank($scope.ruleResponse.responseContentType)) {
            showAlert("'Content Type' is required");
            return;
        }

        if (utils.isBlank($scope.ruleResponse.httpStatusCode)
                || !utils.isNumeric($scope.ruleResponse.httpStatusCode)) {
            showAlert("'Http Status Code' is required and must be numeric");
            return;
        }

        if ($scope.ruleResponse.groups.length == 0) {
            showAlert("You have not added any rule conditions");
            return;
        }

        for (var r=0; r < $scope.responseHeaderList.length; r++) {
            var rhName = $scope.responseHeaderList[r].name;
            var rhValue = $scope.responseHeaderList[r].value;

            if (utils.isBlank(rhName) || utils.isBlank(rhValue)) {
                showAlert("You have blank 'Response Header' fields. Please amend or remove these.");
                return;
            }

            if ($scope.ruleResponse.responseHeaders[rhName] != null) {
                showAlert("The 'Response Header' field '" + rhName + "' is defined more then once.");
                return;
            }

            $scope.ruleResponse.responseHeaders[rhName] = rhValue;
        }

        $uibModalInstance.close($scope.ruleResponse);
    };

    $scope.doMoveConditionUp = function(index) {

        if (index == 0) {
            return;
        }

        var rule = $scope.ruleResponse.groups[index];

        $scope.ruleResponse.groups.splice(index, 1);
        $scope.ruleResponse.groups.splice((index - 1), 0, rule);
    };

    $scope.doMoveConditionDown = function(index) {

        if ((index + 1) == $scope.ruleResponse.groups.length) {
            return;
        }

        var rule = $scope.ruleResponse.groups[index];

        $scope.ruleResponse.groups.splice(index, 1);
        $scope.ruleResponse.groups.splice((index + 1), 0, rule);
    };

    $scope.doEditCondition = function(index) {
        openRuleConditions($scope.ruleResponse.groups[index]);
    };

    $scope.doRemoveCondition = function(index) {
        $scope.ruleResponse.groups.splice(index, 1);
    };

    $scope.doAddCondition = function() {

        openRuleConditions();

    };

    $scope.doCancel = function() {
        $uibModalInstance.dismiss('cancel');
    };


    //
    // Internal Functions
    function openRuleConditions(ruleGroup) {

        var modalInstance = $uibModal.open({
          templateUrl: 'endpoint_info_rule_condition.html',
          controller: 'endpointInfoRuleConditionController',
          backdrop  : 'static',
          keyboard  : false,
          resolve: {
            data: function () {
              return {
                "ruleGroup" : ruleGroup,
                "mockType" : data.mockType
              };
            }
          }
        });

        modalInstance.result.then(function (conditionArgs) {

            if (ruleGroup != null) {
                for (var g=0; g < $scope.ruleResponse.groups.length; g++) {
                    if ($scope.ruleResponse.groups[g].extId == ruleGroup.extId) {
                        $scope.ruleResponse.groups.splice(g, 1);
                        break;
                    }
                }
            }

            $scope.ruleResponse.groups.push({
                "extId" : null,
                "orderNo" : ($scope.ruleResponse.groups.length + 1),
                "conditions" : conditionArgs
            });

        }, function () {

        });

    }

});
