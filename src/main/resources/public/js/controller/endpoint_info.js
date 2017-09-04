
app.controller('endpointInfoController', function($scope, $rootScope, $route, $location, $uibModal, $http, $timeout, utils, globalVars, restClient) {


    //
    // Constants
    var MockTypeSeq = 'SEQ';
    var MockTypeRule = 'RULE';
    var MockTypeProxyHttp = 'PROXY_HTTP';
    var MockTypeWebSocket = 'PROXY_WS';
    var isNew = ($rootScope.endpointData == null);
    var RestfulServerType = globalVars.RestfulServerType;
    var AlertTimeoutMillis = globalVars.AlertTimeoutMillis;
    var ActiveStatus = "ACTIVE";
    var InActiveStatus = "INACTIVE";
    var MaxProxyTimeoutInMillis = 60000; // 1 min
    var MaxWebSocketTimeoutInMillis = 3600000; // 1 hour


    //
    // Labels
    $scope.mockTypeSeq = MockTypeSeq;
    $scope.mockTypeRule = MockTypeRule;
    $scope.mockTypeProxyHttp = MockTypeProxyHttp;
    $scope.mockTypeWebSocket = MockTypeWebSocket;
    $scope.newEndpointHeading = (isNew)?'New Endpoint':'View Endpoint';
    $scope.pathLabel = 'Path';
    $scope.pathPlaceHolderTxt = 'e.g. (/hello) (path vars: /hello/:name/greeting) (wildcards: /hello/*/greeting)';
    $scope.methodLabel = 'Method';
    $scope.methodDropDownLabel = 'Select...';
    $scope.defaultContentTypeLabel = 'Default Content Type';
    $scope.defaultContentTypePlaceholderTxt = 'e.g. (application/json)';
    $scope.defaultHttpStatusCodeLabel = 'Default HTTP Status Code';
    $scope.defaultHttpStatusCodePlaceholderTxt = 'e.g. (200, 201, 404)';
    $scope.defaultResponseBodyLabel = 'Default Response Body';
    $scope.noRulesFound = 'No Rules Found';
    $scope.noSeqFound = 'No Sequenced Responses Found';
    $scope.orderNoLabel = 'Seq';
    $scope.ruleLabel = 'Order';
    $scope.statusCodeLabel = 'Code';
    $scope.responseBodyLabel = 'Response Body';
    $scope.sequenceResponsesRadioLabel = 'Sequenced';
    $scope.rulesRadioLabel = 'Rules';
    $scope.proxyRadioLabel = 'Proxied';
    $scope.webSocketRadioLabel = 'WebSocket';
    $scope.responseHeadersLabel = 'Default Response Headers';
    $scope.responseHeaderNameLabel = 'Name';
    $scope.responseHeaderValueLabel = 'Value';
    $scope.serverRestartInstruction = '(Please note, the mock server will need to be restarted for changes to take effect)';
    $scope.endpointStatusLabel = 'Status:';
    $scope.proxyTimeoutLabel = 'Long Polling Timeout (in millis)';
    $scope.webSocketTimeoutLabel = 'Idle Timeout (in millis)';
    $scope.proxyTimeoutPlaceholderTxt = 'Duration the server will hold the request open with no activity (zero for no timeout)';
    $scope.webSocketTimeoutPlaceholderTxt = 'Duration the server will keep the socket open whilst idle (zero for no timeout)';
    $scope.shuffleSequenceLabel = "Shuffle Responses";
    $scope.wsClientConnectionLabel = 'Active Client Connections';
    $scope.activeWsClientsFound = 'No Websocket Clients Found';


    //
    // Buttons
    $scope.saveButtonLabel = 'Save';
    $scope.deleteButtonLabel = 'Delete';
    $scope.cancelButtonLabel = 'Cancel';
    $scope.addRuleButtonLabel = 'Add Rule';
    $scope.addSequenceButtonLabel = 'Add Seq Response';
    $scope.viewButtonLabel = "View";
    $scope.removeResponseHeaderButtonLabel = 'X';
    $scope.addResponseHeaderButtonLabel = 'New Row';
    $scope.formatResponseBodyLinkLabel = '(pretty print JSON)';
    $scope.refreshWsClientsLinkLabel = 'refresh';


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
    $scope.activeStatus = ActiveStatus;
    $scope.inActiveStatus = InActiveStatus;

    $scope.contentTypes = globalVars.ContentMimeTypes;

    $scope.httpMethods = [
        'GET',
        'POST',
        'PUT',
        'DELETE',
        'PATCH'
    ];

    $scope.extId = null;

    $scope.responseHeaderList = [];

    $scope.activeWsClients = [];

    $scope.endpoint = {
        "path" : null,
        "method" : null,
        "contentType" : null,
        "httpStatusCode" : 200,
        "responseBody" : null,
        "status" : ActiveStatus,
        "proxyTimeout" : 0,
        "webSocketTimeout" : 0,
        "mockType" : MockTypeSeq, // RULE
        "randomiseDefinitions" : false,
        "definitions" : [],
        "rules" : []
    };

    // Populate form if viewing existing record...
    if (!isNew) {

        var endpoint = $rootScope.endpointData;

        // Convert all rule arg DTOs to local enriched arg objects
        for (var r=0; r < endpoint.rules.length; r++) {
            for (var g=0; g < endpoint.rules[r].groups.length; g++) {
                endpoint.rules[r].groups[g].conditions = utils.convertFromDTO(endpoint.rules[r].groups[g].conditions);
            }
        }

        $scope.endpoint = {
            "path" : endpoint.path,
            "method" : endpoint.method,
            "contentType" : null,
            "httpStatusCode" : null,
            "responseBody" : null,
            "status" : endpoint.status,
            "proxyTimeout" : endpoint.proxyTimeoutInMillis,
            "webSocketTimeout" : endpoint.webSocketTimeoutInMillis,
            "mockType" : endpoint.mockType,
            "randomiseDefinitions" : endpoint.randomiseDefinitions,
            "definitions" : endpoint.definitions,
            "rules" : endpoint.rules
        };

        $scope.extId = endpoint.extId;

        if (endpoint.mockType == MockTypeSeq
                || endpoint.mockType == MockTypeRule) {

            $scope.endpoint.contentType = endpoint.definitions[0].responseContentType;
            $scope.endpoint.httpStatusCode = endpoint.definitions[0].httpStatusCode;
            $scope.endpoint.responseBody = endpoint.definitions[0].responseBody;

            angular.forEach(endpoint.definitions[0].responseHeaders, function(v, k) {
                $scope.responseHeaderList.push({ 'name' : k, 'value' : v });
            });

        }

    }


    //
    // Functions
    $scope.doAddResponseHeaderRow = function() {
        $scope.responseHeaderList.push({ "name" : null, "value" : null });
    };

    $scope.doRemoveResponseHeaderRow = function(index) {
        $scope.responseHeaderList.splice(index, 1);
    };

    $scope.doSelectHttpMethod = function (m) {
        $scope.endpoint.method = m;
    };

    $scope.doSetEndpointStatus = function(s) {
        $scope.endpoint.status = s;
    };

    $scope.doMoveRuleUp = function(rule, index) {

        if (index == 0
            || $scope.endpoint.rules[index].suspend) {
            return;
        }

        // Update position in rule array
        $scope.endpoint.rules.splice(index, 1);
        $scope.endpoint.rules.splice((index - 1), 0, rule);

        // Update all orderNo fields in rule array
        updateRuleOrderNumbers();

    };

    $scope.doMoveRuleDown = function(rule, index) {

        if ( (index + 1) == $scope.endpoint.rules.length
            || $scope.endpoint.rules[index].suspend) {
            return;
        }

        // Update position in rule array
        $scope.endpoint.rules.splice(index, 1);
        $scope.endpoint.rules.splice(index + 1, 0, rule);

        // Update all orderNo fields in rule array
        updateRuleOrderNumbers();

    };

    $scope.doRemoveRule = function(index) {

        if ($scope.endpoint.rules[index].suspend) {
            return;
        }

        utils.openWarningConfirmation("Remove this rule? (You will need to save for this to take effect)", function (alertResponse) {

            if (alertResponse) {
                $scope.endpoint.rules.splice(index, 1);

                // Update all orderNo fields in rule array
                updateRuleOrderNumbers();
            }

        });

    };

    $scope.doRefreshActiveClients = function() {

        $scope.activeWsClients = [];

        restClient.doGet($http, '/ws/' + $scope.endpoint.path + '/client', function(status, data) {

            if (status != 200) {
                showAlert("Oops looks like something went wrong!");
                return;
            }

            for (var d=0; d < data.length; d++) {
                $scope.activeWsClients.push(data[d]);
            }

        });

    };

    function updateRuleOrderNumbers() {
        for (var r=0; r < $scope.endpoint.rules.length; r++) {
            $scope.endpoint.rules[r].orderNo = (r + 1);
        }
    }

    $scope.doToggleSuspendRule = function (index) {
        $scope.endpoint.rules[index].suspend = !$scope.endpoint.rules[index].suspend;
    };

    $scope.doMoveSeqUp = function (seq, index) {

        if (index == 0
            || $scope.endpoint.definitions[index].suspend) {
            return;
        }

        // Update position in seq array
        $scope.endpoint.definitions.splice(index, 1);
        $scope.endpoint.definitions.splice(index - 1, 0, seq);

        // Update all orderNo fields in seq array
        updateSeqOrderNumbers();

    };

    $scope.doMoveSeqDown = function (seq, index) {

        if ( (index + 1) == $scope.endpoint.definitions.length
            || $scope.endpoint.definitions[index].suspend) {
            return;
        }

        // Update position in seq array
        $scope.endpoint.definitions.splice(index, 1);
        $scope.endpoint.definitions.splice(index + 1, 0, seq);

        // Update all orderNo fields in seq array
        updateSeqOrderNumbers();

    };

    $scope.doRemoveSeq = function(index) {

        if ($scope.endpoint.definitions[index].suspend) {
            return;
        }

        utils.openWarningConfirmation("Remove this sequenced response? (You will need to save for this to take effect)", function (alertResponse) {

            if (alertResponse) {

                $scope.endpoint.definitions.splice(index, 1);

                // Update all orderNo fields in seq array
                updateSeqOrderNumbers();

                if (countActiveDefinitions($scope.endpoint.definitions) < 2) {
                    $scope.endpoint.randomiseDefinitions = false;
                }

            }

       });

    };

    function updateSeqOrderNumbers() {
        for (var s=0; s < $scope.endpoint.definitions.length; s++) {
            $scope.endpoint.definitions[s].orderNo = (s + 1);
        }
    }

    $scope.doToggleSuspendSeq = function (index) {

        $scope.endpoint.definitions[index].suspend = !$scope.endpoint.definitions[index].suspend;

        if (countActiveDefinitions($scope.endpoint.definitions) < 2) {
            $scope.endpoint.randomiseDefinitions = false;
        }

    };

    $scope.doOpenViewRule = function(rule) {

      var modalInstance = $uibModal.open({
          templateUrl: 'endpoint_info_rule.html',
          controller: 'endpointInfoRuleController',
          resolve: {
            data: function () {
              return {
                "rule" : rule
              };
            }
          }
        });

        modalInstance.result.then(function () {

        }, function () {

        });

    };

    $scope.doOpenAddRule = function() {

      var modalInstance = $uibModal.open({
          templateUrl: 'endpoint_info_rule.html',
          controller: 'endpointInfoRuleController',
          resolve: {
            data: function () {
              return { };
            }
          }
        });

        modalInstance.result.then(function (rule) {
            rule.orderNo = ( $scope.endpoint.rules.length + 1 );
            $scope.endpoint.rules.push(rule);
        }, function () {

        });

    };

    $scope.doOpenViewSequence = function(seq) {

        var modalInstance = $uibModal.open({
          templateUrl: 'endpoint_info_seq.html',
          controller: 'endpointInfoSeqController',
          resolve: {
            data: function () {
              return { "seq" : seq };
            }
          }
        });

        modalInstance.result.then(function (seqResponse) {

            for (var d=0; d < $scope.endpoint.definitions.length; d++) {

                if ( ($scope.endpoint.definitions[d].extId != null && $scope.endpoint.definitions[d].extId == seqResponse.extId )
                        || ( $scope.endpoint.definitions[d].extId == null && $scope.endpoint.definitions[d].orderNo == seqResponse.orderNo ) ) {
                    $scope.endpoint.definitions[d] = seqResponse;
                    break;
                }

            }

        }, function () {

        });

    };

    $scope.doOpenAddSequence = function() {

        var modalInstance = $uibModal.open({
          templateUrl: 'endpoint_info_seq.html',
          controller: 'endpointInfoSeqController',
          resolve: {
            data: function () {
              return {};
            }
          }
        });

        modalInstance.result.then(function (seqResponse) {
            seqResponse.orderNo = ( $scope.endpoint.definitions.length + 1 );
            $scope.endpoint.definitions.push(seqResponse);
        }, function () {

        });

    };

    $scope.doDelete = function() {

        if ($scope.extId == null) {
            return;
        }

        utils.openDeleteConfirmation("Are you sure you wish to delete this endpoint?", function (alertResponse) {

            if (alertResponse) {

                utils.showBlockingOverlay();
                restClient.doDelete($http, '/restmock/' + $scope.extId, serverCallbackFunc);

            }

       });

    };

    $scope.doPrettyPrintResponse = function() {

        if ($scope.endpoint.contentType == "application/json") {

            var formattedResponseBody = utils.prettyPrintJSON($scope.endpoint.responseBody);

            if (formattedResponseBody == null) {
                showAlert("Unable to pretty print. Please check your JSON syntax", "warning");
                return;
            }

            $scope.endpoint.responseBody = formattedResponseBody;
        }
    };

    $scope.doAdjustWSMethod = function() {
        $scope.endpoint.method = 'GET';
    };

    $scope.doSaveEndpoint = function() {

        // Validation
        if (utils.isBlank($scope.endpoint.path)) {
            showAlert("'Path' is required");
            return;
        }

        if ($scope.endpoint.mockType == MockTypeRule && !validateRule()) {
            return;
        } else if ($scope.endpoint.mockType == MockTypeSeq && !validateSeq()) {
            return;
        } else if ($scope.endpoint.mockType == MockTypeProxyHttp && !validateProxy()) {
            return;
        } else if ($scope.endpoint.mockType == MockTypeWebSocket && !validateWebSocket()) {
            return;
        }

        // Send to Server
        utils.showBlockingOverlay();

        var reqData = {
            "path" : $scope.endpoint.path,
            "method" : $scope.endpoint.method,
            "status" : $scope.endpoint.status,
            "mockType" : $scope.endpoint.mockType,
            "proxyTimeoutInMillis" : $scope.endpoint.proxyTimeout,
            "webSocketTimeoutInMillis" : $scope.endpoint.webSocketTimeout,
            "randomiseDefinitions" : $scope.endpoint.randomiseDefinitions,
            "definitions" : [],
            "rules" : []
        };

        // Handle Sequence specifics
        if ($scope.endpoint.mockType == MockTypeSeq) {

            for (var d=0; d < $scope.endpoint.definitions.length; d++) {
                reqData.definitions.push($scope.endpoint.definitions[d]);
            }

            if (countActiveDefinitions(reqData.definitions) < 2) {
                reqData.randomiseDefinitions = false;
            }

        // Handle Rule specifics
        } else if ($scope.endpoint.mockType == MockTypeRule) {

           // Default response (where a rule is not matched)
            reqData.definitions.push({
                "extId" : null,
                "orderNo" : 1,
                "responseContentType" : $scope.endpoint.contentType,
                "httpStatusCode" : $scope.endpoint.httpStatusCode,
                "responseBody" : $scope.endpoint.responseBody,
                "sleepInMillis" : 0,
                "suspend" : false,
                "responseHeaders" : {}
            });

            // Default response headers (where a rule is not matched)
            for (var r=0; r < $scope.responseHeaderList.length; r++) {
                reqData.definitions[0].responseHeaders[$scope.responseHeaderList[r].name] = $scope.responseHeaderList[r].value;
            }

            // Rules
            for (var r=0; r < $scope.endpoint.rules.length; r++) {

                // Convert all local rule arg objects to DTO
                for (var g=0; g < $scope.endpoint.rules[r].groups.length; g++) {
                    $scope.endpoint.rules[r].groups[g].conditions = utils.convertToDTO($scope.endpoint.rules[r].groups[g].conditions);
                }

                reqData.rules.push($scope.endpoint.rules[r]);
            }

        } else if ($scope.endpoint.mockType == MockTypeProxyHttp) {
            // Nothing extra to do
         } else if ($scope.endpoint.mockType == MockTypeWebSocket) {
            // Nothing extra to do
        }

        if ($scope.extId != null) {
            restClient.doPut($http, '/restmock/' + $scope.extId, reqData, serverCallbackFunc);
        } else {
            restClient.doPost($http, '/restmock', reqData, serverCallbackFunc);
        }

    };

    function validateRule() {

        if (utils.isBlank($scope.endpoint.method)) {
            showAlert("'Method' is required");
            return false;
        }

        if (utils.isBlank($scope.endpoint.contentType)) {
            showAlert("'Default Content Type' is required");
            return false;
        }

        if (utils.isBlank($scope.endpoint.httpStatusCode)
                || !utils.isNumeric($scope.endpoint.httpStatusCode)) {
            showAlert("'Default HTTP Status Code' is required and must be numeric");
            return false;
        }

        // Validate there are not unpopulated response headers
        for (var r=0; r < $scope.responseHeaderList.length; r++) {

            var rhName = $scope.responseHeaderList[r].name;
            var rhValue = $scope.responseHeaderList[r].value;

            if (utils.isBlank(rhName) || utils.isBlank(rhValue)) {
                showAlert("You have blank 'Default Response Header' fields. Please amend or remove these.");
                return false;
            }

            // Validate there are not duplicated response header keys
            var occurrences = 0;

            for (var cr=0; cr < $scope.responseHeaderList.length; cr++) {
                if (rhName == $scope.responseHeaderList[cr].name) {
                    occurrences = (occurrences + 1);
                }
            }

            // Assert only 1 occurrence of the header to be present.
            if (occurrences > 1) {
                showAlert("The 'Default Response Header' field '" + rhName + "' is defined more then once.");
                return false;
            }

        }

        return true;
    }

    function validateSeq() {

      if (utils.isBlank($scope.endpoint.method)) {
            showAlert("'Method' is required");
            return false;
        }

        if (countActiveDefinitions($scope.endpoint.definitions) == 0) {
            showAlert("At least one active 'Sequenced Response' is required");
            return false;
        }

        return true;
    }

    function validateProxy() {

       if (utils.isBlank($scope.endpoint.method)) {
            showAlert("'Method' is required");
            return false;
        }

        if (utils.isBlank($scope.endpoint.proxyTimeout)
                || !utils.isNumeric($scope.endpoint.proxyTimeout)) {
            showAlert("'Proxy Timeout' is required and must be numeric.");
            return false;
        }

        var timeout = $scope.endpoint.proxyTimeout;

        if (typeof $scope.endpoint.proxyTimeout == 'string') {
            timeout = parseInt($scope.endpoint.proxyTimeout);
        }

        if (timeout > MaxProxyTimeoutInMillis) {
            showAlert("'Proxy Timeout' cannot exceed " + MaxProxyTimeoutInMillis + " milliseconds (i.e " + (MaxProxyTimeoutInMillis / 1000) + " seconds)");
            return false;
        }

        return true;
    }

    function validateWebSocket() {

      if (utils.isBlank($scope.endpoint.webSocketTimeout)
                || !utils.isNumeric($scope.endpoint.webSocketTimeout)) {
            showAlert("'WebSocket Timeout' is required and must be numeric.");
            return false;
        }

        var timeout = $scope.endpoint.webSocketTimeout;

        if (typeof $scope.endpoint.webSocketTimeout == 'string') {
            timeout = parseInt($scope.endpoint.webSocketTimeout);
        }

        if (timeout > MaxWebSocketTimeoutInMillis) {
            showAlert("'WebSocket Timeout' cannot exceed " + MaxWebSocketTimeoutInMillis + " milliseconds (i.e " + (MaxWebSocketTimeoutInMillis / 1000) + " seconds)");
            return false;
        }

        return true;
    }

    var serverCallbackFunc = function (status, data) {

        if (status == 201 || status == 204) {

            checkAutoRefreshStatus(function(autoRefresh) {

                var locParams = {};

                if (autoRefresh != null && autoRefresh) {
                    locParams = { 'restart' : 'true' };
                }

                utils.hideBlockingOverlay();
                $location.path("/dashboard").search(locParams);
                clearEndpointData();
            });

            return;
        }

        utils.hideBlockingOverlay();
        showAlert("Oops looks like something went wrong!");
    };

    $scope.doCancel = function() {
        $location.path("/dashboard");
        clearEndpointData();
    };

    function clearEndpointData() {
        $rootScope.endpointData = null;
        $scope.extId = null;
    }

    function checkAutoRefreshStatus(callback) {

        restClient.doGet($http, '/mockedserver/config/' + RestfulServerType, function(status, data) {
            if (status == 200) {
                callback(data.autoRefresh);
                return;
            }

            callback();
        });

    }

    function countActiveDefinitions(definitions) {

        var activeDefinitions = 0;

        for (var d=0; d < definitions.length; d++) {
            if (!definitions[d].suspend) {
                activeDefinitions++;
            }
        }

        return activeDefinitions;
    }

});
