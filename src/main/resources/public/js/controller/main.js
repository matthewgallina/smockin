var app = angular.module('smockinApp', ['ngRoute', 'ngAnimate', 'ngSanitize', 'ui.bootstrap']);

//
// Routes
app.config(function($routeProvider) {
    $routeProvider
        .when("/", {
            templateUrl : "dashboard.html",
            controller : "dashboardController"
        })
        .when("/dashboard", {
            templateUrl : "dashboard.html",
            controller : "dashboardController"
        })
        .when("/endpoint", {
            templateUrl : "endpoint_info.html",
            controller : "endpointInfoController"
        })
        .when("/help", {
            templateUrl : "help_section.html",
            controller : "helpSectionController"
        });
});


//
// Main Controller
app.controller('mainController', function($scope, $location, $http, $timeout, $uibModal, globalVars, restClient) {

    //
    // Labels
    $scope.toolsLabel = "Tools";
    $scope.helpLink = "Help"


    //
    // Buttons / Links
    $scope.testClientLink = "Open Http Client";
    $scope.helpLink = "Help";

    //
    // Data Objects
    var httpClientState = null;

    //
    // Functions
    $scope.doOpenHttpClient = function() {

      var modalInstance = $uibModal.open({
          templateUrl: 'http_client.html',
          controller: 'httpClientController',
          resolve: {
            data: function () {
              return {
                "state" : httpClientState
              };
            }
          }
        });

        modalInstance.result.then(function (state) {
            httpClientState = state;
        }, function () {

        });
    };

    $scope.doOpenHelp = function() {
        $location.path("/help");
    };

});


//
// Services
app.service('restClient', function() {

    this.doGet = function (http, url, callback) {

        http({
          method: 'GET',
          url: url,
          timeout: 8000
        }).then(function successCallback(response) {
            callback(response.status, response.data);
        }, function errorCallback(response) {
            callback(response.status, response.data);
        });

    };

    this.doPost = function (http, url, reqData, callback) {

        http({
          method: 'POST',
          url: url,
          data: reqData,
          headers: {
             'Content-Type': 'application/json'
           },
           timeout: 8000
        }).then(function successCallback(response) {
            callback(response.status, response.data);
        }, function errorCallback(response) {
            callback(response.status, response.data);
        });

    };

    this.doPut = function (http, url, reqData, callback) {

        http({
          method: 'PUT',
          url: url,
          data: reqData,
          headers: {
             'Content-Type': 'application/json'
           },
           timeout: 8000
        }).then(function successCallback(response) {
            callback(response.status, response.data);
        }, function errorCallback(response) {
            callback(response.status, response.data);
        });

    };

    this.doDelete = function (http, url, callback) {

        http({
          method: 'DELETE',
          url: url,
          timeout: 8000
        }).then(function successCallback(response) {
            callback(response.status, response.data);
        }, function errorCallback(response) {
            callback(response.status, response.data);
        });

    };

});

app.service('globalVars', function($uibModal) {

    this.AlertTimeoutMillis = 5000;

    this.GeneralErrorMessage = "Oops looks like something went wrong!";

    this.RestfulServerType = "RESTFUL";

    this.TextDataType = 'TEXT';
    this.NumericDataType = 'NUMERIC';
    this.PathVariableText = 'PATH_VARIABLE';
    this.PathVariableWildcardText = 'PATH_VARIABLE_WILD';
    this.RequestHeaderText = 'REQUEST_HEADER';
    this.RequestParamText = 'REQUEST_PARAM';
    this.RequestBodyText = 'REQUEST_BODY';
    this.EqualsText = 'EQUALS';
    this.ContainsText = 'CONTAINS';
    this.IsMissingText = 'IS_MISSING';

    this.ContentMimeTypes = [
        'application/json',
        'text/html',
        'text/plain',
        'text/css',
        'application/xml'
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
        { name : 'Request Body', value : this.RequestBodyText, fieldPlaceholderText : '' }
    ];

});

app.service('utils', function($uibModal, globalVars, restClient, $http) {

    //
    // Validation
    this.isBlank = function (value) {
        return (value == null
            || String(value).trim().length == 0 );
    };

    this.isNumeric = function (value) {
        return (value != null
            && !isNaN(value));
    };


    //
    // Tools
    this.generateUUID = function() {
        var d = new Date().getTime();
        var uuid = 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
            var r = (d + Math.random()*16)%16 | 0;
            d = Math.floor(d/16);
            return (c=='x' ? r : (r&0x3|0x8)).toString(16);
        });
        return uuid;
    };

    this.prettyPrintJSON = function(input) {

        if (input == null) {
            return null;
        }

        try {
            JSON.parse( input );
            return vkbeautify.json(input, 4).trim();
        } catch (err) {}

        return null;
    };


    //
    // Confirmation Modal
    function openConfirmation(heading, body, actionType, nextButtonLabel, callbackAction) {

       var modalInstance = $uibModal.open({
          templateUrl: 'confirmation_alert.html',
          controller: 'confirmationAlertController',
          resolve: {
            data: function () {
              return {
              "heading" : heading,
              "body" : body,
              "alertType" : actionType,
              "nextButtonLabel" : nextButtonLabel
              };
            }
          }
        });

        modalInstance.result.then(function () {
            callbackAction(true);
        }, function () {
            callbackAction(false);
        });

    }

    this.openDeleteConfirmation = function(body, callbackAction) {
        openConfirmation("Delete Confirmation", body, "danger", "Delete", callbackAction);
    };

    this.openWarningConfirmation = function(body, callbackAction) {
        openConfirmation("Confirmation", body, "warning", "Continue", callbackAction);
    };


    //
    // DTO conversions

    // Converts each 'conditionArg' object to the DTO format required by the 'save rule' REST service
    this.convertToDTO = function(conditionArgList) {

       var conditionDTOs = [];

       for (var ca=0; ca < conditionArgList.length; ca++) {

           conditionDTOs.push({
               "extId" : null,
               "ruleMatchingType" : conditionArgList[ca].matchType.value,
               "field" : conditionArgList[ca].fieldName,
               "comparator" : conditionArgList[ca].comparator.value,
               "dataType" : conditionArgList[ca].dataType,
               "caseSensitive" : conditionArgList[ca].caseSensitive,
               "value" : conditionArgList[ca].matchValue,
           });

       }

       return conditionDTOs;
    };

    // Converts each condition DTO object to the 'conditionArg' object
    this.convertFromDTO =  function(conditionDTOs) {

       var conditionArgList = [];

       for (var ct=0; ct < conditionDTOs.length; ct++) {

           conditionDTOs[ct];

            conditionArgList.push({
               "matchType" : getMatchingTypeForValue(conditionDTOs[ct].ruleMatchingType),
               "fieldName" : conditionDTOs[ct].field,
               "comparator" : getComparatorForValue(conditionDTOs[ct].comparator, conditionDTOs[ct].dataType),
               "dataType" : conditionDTOs[ct].dataType,
               "caseSensitive" : conditionDTOs[ct].caseSensitive,
               "matchValue" : conditionDTOs[ct].value,
           });

       }

       return conditionArgList;
    };

    function getMatchingTypeForValue(mValue) {

       for (var m=0; m < globalVars.RuleMatchingTypes.length; m++) {
           if (globalVars.RuleMatchingTypes[m].value == mValue) {
               return globalVars.RuleMatchingTypes[m];
           }
       }

       return null;
    };

    function getComparatorForValue(cValue, dtValue) {

        for (var c=0; c < globalVars.RuleComparators.length; c++) {
            if (globalVars.RuleComparators[c].value == cValue
                    && globalVars.RuleComparators[c].dataType == dtValue) {
                return globalVars.RuleComparators[c];
            }
        }

        return null;
    };


    //
    // Mock Server Actions

    this.checkServerStatus = function (callback) {

        restClient.doGet($http, '/mockedserver/rest/status', function(status, data) {

            if (status == 200) {
                callback(data.running);
                return;
            }

            callback();
        });

    };

    this.showBlockingOverlay = function() {
        angular.element( document.getElementById("blocking-overlay") ).css('width', '100%');
    };

    this.hideBlockingOverlay = function() {
        angular.element( document.getElementById("blocking-overlay") ).css('width', '0%');
    };

    // Uses jqLite
    function setLoadingOverlayWidth(width, msg) {

        if (msg != null) {
            angular.element( document.getElementById("loading-overlay-message") ).text(msg);
        }

        angular.element( document.getElementById("loading-overlay") ).css('width', width);
    };

    this.showLoadingOverlay = function(msg) {

        if (msg == null) {
            msg = 'Loading';
        }

        setLoadingOverlayWidth('100%', msg);
    };

    this.hideLoadingOverlay = function() {
        setLoadingOverlayWidth('0%');
    };

    // Uses jqLite
    function setLoadingOverlayWidth(width, msg) {

        if (msg != null) {
            angular.element( document.getElementById("loading-overlay-message") ).text(msg);
        }

        angular.element( document.getElementById("loading-overlay") ).css('width', width);
    };

});
