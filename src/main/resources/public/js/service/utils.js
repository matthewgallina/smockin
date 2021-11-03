
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

    this.hasWhiteSpace = function (value) {
        return /\s/g.test(value);
    };

    this.isAlpha = function (value) {
        var letters = /^[a-zA-Z]+$/;
        return (value.match(letters));
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

    this.validateJson = function(jsonString) {
        try {
            jsonlint.parse(jsonString)
            return null;
        } catch (e) {
            return e.message;
        }
    };

    this.formatJson = function(jsonString) {
        return JSON.stringify(JSON.parse(jsonString), null, 2);
    };

    this.validateAndFormatXml = function(srcXmlText) {

        var beautifiedXmlText = new XmlBeautify().beautify(srcXmlText, {
            indent: "  ",
            useSelfClosingElement: true
        });

        if (beautifiedXmlText.indexOf("parsererror") > -1) {

            var errorStartPos = beautifiedXmlText.indexOf("error on line");

            if (errorStartPos > -1) {
                var errorEndPos = beautifiedXmlText.indexOf("</div>", errorStartPos);
                return [ 'ERROR', beautifiedXmlText.substring(errorStartPos, errorEndPos) ];
            }

            return [ 'ERROR', 'Unable to format XML. Invalid syntax' ];
        }

        return [ 'OK', beautifiedXmlText ];
    };

    this.handleExportDownload = function(exportData, fileName, contentType) {

        var iFrame = jQuery('#export-download-frame');
        var iFrameDoc = iFrame[0].contentDocument || iFrame[0].contentWindow.document;

        var a = iFrameDoc.createElement('a');
        a.download = fileName;
        a.text = "";
        a.href = "data:" + contentType + ";base64," + exportData;

        iFrame.contents().find("body").append(a);
        iFrameDoc.close();

        iFrame.contents().find("body").append(a);

        var clickEvent = iFrameDoc.createEvent("MouseEvent");
        clickEvent.initEvent("click", true, true);
        a.dispatchEvent(clickEvent);

    };


    //
    // Confirmation Modal
    function openConfirmation(heading, body, actionType, nextButtonLabel, callbackAction) {

       var modalInstance = $uibModal.open({
          templateUrl: 'confirmation_alert.html',
          controller: 'confirmationAlertController',
          backdrop  : 'static',
          keyboard  : false,
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

        if (callbackAction != null) {
            modalInstance.result.then(function () {
                callbackAction(true);
            }, function () {
                callbackAction(false);
            });
        }

    }

    this.openDeleteConfirmation = function(body, callbackAction) {
        openConfirmation("Delete Confirmation", body, "danger", "Delete", callbackAction);
    };

    this.openWarningConfirmation = function(body, callbackAction) {
        openConfirmation("Confirmation", body, "warning", "Continue", callbackAction);
    };

    this.openAlert = function(heading, body, callbackAction) {
        openConfirmation(heading, body, "success", null, callbackAction);
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

    this.checkS3ServerStatus = function (callback) {

        restClient.doGet($http, '/mockedserver/s3/status', function(status, data) {

            if (status == 200) {
                callback(data.running, data.port);
                return;
            }

            callback();
        });

    };

    this.isSecureConnectionType = function() {
        return window.location.protocol.indexOf("https") > -1;
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
