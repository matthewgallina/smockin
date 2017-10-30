
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

    this.checkRestServerStatus = function (callback) {

        restClient.doGet($http, '/mockedserver/rest/status', function(status, data) {

            if (status == 200) {
                callback(data.running, data.port);
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
