
app.controller('endpointInfoRuleController', function($scope, $location, $uibModal, $uibModalInstance, $http, $timeout, utils, globalVars, data) {

    //
    // Constants
    var AlertTimeoutMillis = globalVars.AlertTimeoutMillis;


    //
    // Labels
    $scope.newEndpointRuleHeading = ( (data.rule != null)?'View':'New' ) + ' Rule';
    $scope.contentTypeLabel = 'Content Type';
    $scope.contentTypePlaceholderTxt = 'e.g. (application/json)';
    $scope.httpStatusCodeLabel = 'HTTP Status Code';
    $scope.httpStatusCodePlaceholderTxt = 'e.g. (200, 201, 404)';
    $scope.responseBodyLabel = 'Response Body';
    $scope.ruleConditionsTableLabel = "Rule Conditions";
    $scope.responseHeadersLabel = 'Response Headers';
    $scope.responseHeaderNameLabel = 'Name';
    $scope.responseHeaderValueLabel = 'Value';


    //
    // Buttons
    $scope.cancelButtonLabel = 'Cancel';
    $scope.saveButtonLabel = (data.rule != null)?'Amend Rule':'Add Rule';
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

    if (data.rule != null) {
        $scope.ruleResponse = data.rule;

        angular.forEach($scope.ruleResponse.responseHeaders, function(v, k) {
            $scope.responseHeaderList.push({ 'name' : k, 'value' : v });
        });

    }


    //
    // Functions
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

    $scope.doRemoveCondition = function(index) {
        $scope.ruleResponse.groups.splice(index, 1);
    };

    $scope.doAddCondition = function() {

        var modalInstance = $uibModal.open({
          templateUrl: 'endpoint_info_rule_condition.html',
          controller: 'endpointInfoRuleConditionController',
          resolve: {
            data: function () {
              return { };
            }
          }
        });

        modalInstance.result.then(function (conditionArgs) {

            $scope.ruleResponse.groups.push({
                "extId" : null,
                "orderNo" : ($scope.ruleResponse.groups.length + 1),
                "conditions" : conditionArgs
            });

        }, function () {

        });

    };

    $scope.doCancel = function() {
        $uibModalInstance.dismiss('cancel');
    };

});
