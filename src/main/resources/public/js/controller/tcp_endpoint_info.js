
app.controller('tcpEndpointInfoController', function($scope, $rootScope, $location, $uibModal, $http, $timeout, utils, globalVars, restClient, auth) {


    //
    // Constants
    var MockTypeDefinitions = {
        MockTypeSeq : 'SEQ',
        MockTypeRule : 'RULE',
        MockTypeProxyHttp : 'PROXY_HTTP',
        MockTypeWebSocket : 'PROXY_WS',
        MockTypeProxySse : 'PROXY_SSE'
    };

    var TimeoutDefinitions = {
        AlertTimeoutMillis : globalVars.AlertTimeoutMillis,
        MinTimeoutInMillis : 10000, // 10 secs
        MaxProxyTimeoutInMillis : 1800000, // 30 mins
        MaxWebSocketTimeoutInMillis : 3600000 // 1 hour
    };

    var isNew = ($rootScope.endpointData == null);

    var RestfulServerType = globalVars.RestfulServerType;
    var HttpPathPlaceHolderTxt = 'e.g. (/hello) (path vars: /hello/:name/greeting) (wildcards: /hello/*/greeting)';
    var WebSocketPathPlaceHolderTxt = 'e.g. (/hello/connect)';
    var SsePathPlaceHolderTxt = 'e.g. (/hello)';


    //
    // Labels
    $scope.mockTypeSeq = MockTypeDefinitions.MockTypeSeq;
    $scope.mockTypeRule = MockTypeDefinitions.MockTypeRule;
    $scope.mockTypeProxyHttp = MockTypeDefinitions.MockTypeProxyHttp;
    $scope.mockTypeWebSocket = MockTypeDefinitions.MockTypeWebSocket;
    $scope.mockTypeProxySse = MockTypeDefinitions.MockTypeProxySse;
    $scope.endpointHeading = (isNew) ? 'New HTTP Endpoint' : 'HTTP Endpoint';
    $scope.pathPlaceHolderTxt = HttpPathPlaceHolderTxt;
    $scope.pathLabel = 'Path';
    $scope.methodLabel = 'Method';
    $scope.selectDropDownLabel = 'Select...';
    $scope.defaultContentTypeLabel = 'Default Content Type';
    $scope.defaultContentTypePlaceholderTxt = 'e.g. (application/json)';
    $scope.defaultHttpStatusCodeLabel = 'Default HTTP Status Code';
    $scope.defaultHttpStatusCodePlaceholderTxt = 'e.g. (200, 201, 404)';
    $scope.defaultResponseBodyLabel = 'Default Response Body';
    $scope.proxyPathLabel = 'Exact Path';
    $scope.proxyContentTypeLabel = 'Content Type';
    $scope.proxyHttpStatusCodeLabel = 'HTTP Status Code';
    $scope.proxyResponseBodyLabel = 'Response Body';
    $scope.noRulesFound = 'No Rules Found';
    $scope.noSeqFound = 'No Sequenced Responses Found';
    $scope.orderNoLabel = 'Seq';
    $scope.ruleLabel = 'Order';
    $scope.statusCodeLabel = 'Code';
    $scope.frequencyLabel = 'Freq';
    $scope.responseBodyLabel = 'Response Body';
    $scope.responseHeadersLabel = 'Default Response Headers';
    $scope.responseHeaderNameLabel = 'Name';
    $scope.responseHeaderValueLabel = 'Value';
    $scope.serverRestartInstruction = '(Please note, the tcp mock server will need to be restarted for changes to take effect)';
    $scope.endpointStatusLabel = 'Status:';
    $scope.proxyTimeoutLabel = 'Long Polling Timeout (in millis)';
    $scope.webSocketTimeoutLabel = 'Idle Timeout (in millis)';
    $scope.proxyTimeoutPlaceholderTxt = 'Duration the server will hold the request open with no activity (zero for no timeout)';
    $scope.webSocketTimeoutPlaceholderTxt = 'Duration the server will keep the socket open whilst idle (zero for no timeout)';
    $scope.shuffleSequenceLabel = 'Shuffle Responses';
    $scope.activeClientConnectionsLabel = 'Active Client Connections';
    $scope.noActiveWsClientsFound = 'No Websocket Clients Found';
    $scope.noActiveSseClientsFound = 'No SSE Clients Found';
    $scope.clientIdHeading = 'Session Id';
    $scope.clientJoinDateHeading = 'Joining Date';
    $scope.manageHttpProxyQueueLabel = 'HTTP Proxy Queue Tools';
    $scope.sendProxiedResponseLabel = 'Post a proxied response';
    $scope.sseHeartbeatLabel = 'SSE Heartbeat (in millis)';
    $scope.sseHeartbeatPlaceholderTxt = 'Interval at which responses are pushed to the client';
    $scope.pushIdOnConnectLabel = 'Send Session Id on connect';
    $scope.actualPathPrefixLabel = "actual path:";


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
    $scope.refreshClientsLinkLabel = 'refresh';
    $scope.clearProxyButtonLabel = 'Clear Pending Responses';
    $scope.clearProxyResponseFieldsButtonLabel = 'Clear Fields';
    $scope.postProxyResponseButtonLabel = 'Post Response';
    $scope.messageButtonLabel = 'Message';


    //
    // Alerts
    var alertPromise = null;
    $scope.alerts = [];

    var closeAlertFunc = function() {

        if (alertPromise != null) {
            $timeout.cancel( alertPromise );
            alertPromise = null;
        }

        $scope.alerts = [];
    };

    function showAlert(msg, type) {

        if (type == null) {
            type = 'danger';
        }

        closeAlertFunc();
        $scope.alerts.push({ "type" : type, "msg" : msg });

        alertPromise = $timeout(closeAlertFunc, TimeoutDefinitions.AlertTimeoutMillis);
    }

    $scope.closeAlert = closeAlertFunc;


    //
    // Data Objects
    $scope.activeStatus = globalVars.ActiveStatus;
    $scope.inActiveStatus = globalVars.InActiveStatus;

    $scope.contentTypes = globalVars.ContentMimeTypes;

    $scope.httpMethods = [
        'GET',
        'POST',
        'PUT',
        'DELETE',
        'PATCH'
    ];

    $scope.responseBodyLimit = 100;

    $scope.mockTypes = [
       { "name" : "HTTP Sequenced Based", "value" : MockTypeDefinitions.MockTypeSeq },
       { "name" : "HTTP Rules Based", "value" : MockTypeDefinitions.MockTypeRule },
       { "name" : "HTTP Proxied", "value" : MockTypeDefinitions.MockTypeProxyHttp },
       { "name" : "WebSocket Proxied", "value" : MockTypeDefinitions.MockTypeWebSocket },
       { "name" : "SSE Proxied", "value" : MockTypeDefinitions.MockTypeProxySse }
    ];

    var extId = null;

    $scope.isNew = isNew;

    $scope.responseHeaderList = [];

    $scope.activeWsClients = [];
    $scope.activeSseClients = [];

    $scope.endpoint = {
        "path" : null,
        "method" : null,
        "contentType" : null,
        "httpStatusCode" : 200,
        "responseBody" : null,
        "status" : globalVars.ActiveStatus,
        "proxyTimeout" : 0,
        "webSocketTimeout" : 0,
        "sseHeartbeat" : 0,
        "wsPushIdOnConnect" : false,
        "ssePushIdOnConnect" : false,
        "mockType" : lookupMockType(MockTypeDefinitions.MockTypeSeq),
        "randomiseDefinitions" : false,
        "definitions" : [],
        "rules" : []
    };

    $scope.proxyEndpoint = {
        "path" : null,
        "contentType" : null,
        "httpStatusCode" : null,
        "responseBody" : null
    };

    $scope.defaultCtxPathPrefix = (auth.isLoggedIn() && !auth.isSysAdmin()) ? ('/' + auth.getUserName()) : null;

    // Populate form if viewing existing record...
    if (!isNew) {

        var endpoint = $rootScope.endpointData;

        extId = endpoint.extId;

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
            "sseHeartbeat" : endpoint.sseHeartBeatInMillis,
            "wsPushIdOnConnect" : endpoint.proxyPushIdOnConnect,
            "ssePushIdOnConnect" : endpoint.proxyPushIdOnConnect,
            "mockType" : lookupMockType(endpoint.mockType),
            "randomiseDefinitions" : endpoint.randomiseDefinitions,
            "definitions" : endpoint.definitions,
            "rules" : endpoint.rules,
            "createdBy" : endpoint.createdBy
        };

        $scope.defaultCtxPathPrefix = (!utils.isBlank(endpoint.userCtxPath)) ? ('/' + endpoint.userCtxPath) : null;

        if (endpoint.mockType == MockTypeDefinitions.MockTypeSeq
                || endpoint.mockType == MockTypeDefinitions.MockTypeRule) {

            $scope.endpoint.contentType = endpoint.definitions[0].responseContentType;
            $scope.endpoint.httpStatusCode = endpoint.definitions[0].httpStatusCode;
            $scope.endpoint.responseBody = endpoint.definitions[0].responseBody;

            angular.forEach(endpoint.definitions[0].responseHeaders, function(v, k) {
                $scope.responseHeaderList.push({ 'name' : k, 'value' : v });
            });

        }

        if (endpoint.mockType == MockTypeDefinitions.MockTypeProxyHttp) {
            $scope.proxyEndpoint.path = $scope.endpoint.path;
        }

    }

    $scope.readOnly = (!isNew && auth.isLoggedIn() && auth.getUserName() != $scope.endpoint.createdBy);


    //
    // Scoped Functions
    $scope.doSelectMockType = function(et) {

        $scope.endpoint.mockType = et;

        switch (et.value) {
            case MockTypeDefinitions.MockTypeWebSocket :
                $scope.pathPlaceHolderTxt = WebSocketPathPlaceHolderTxt;
                $scope.endpoint.method = 'GET';
            case MockTypeDefinitions.MockTypeProxySse :
                $scope.pathPlaceHolderTxt = SsePathPlaceHolderTxt;
                $scope.endpoint.method = 'GET';
                break;
            default :
                $scope.pathPlaceHolderTxt = HttpPathPlaceHolderTxt;
                break;
        }

    };

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

    $scope.doPostProxyResponse = function() {

        closeAlertFunc();

        // Validation
        if (utils.isBlank($scope.proxyEndpoint.path)) {
            showAlert("'Path' for proxy response is required");
            return false;
        }
        if ($scope.proxyEndpoint.path.indexOf("*") > -1) {
            showAlert("'Path' for proxy response cannot contain any wildcard * path variables");
            return false;
        }
        if (utils.isBlank($scope.proxyEndpoint.contentType)) {
            showAlert("'Content Type' for proxy response is required");
            return false;
        }
        if (utils.isBlank($scope.proxyEndpoint.httpStatusCode)
                || !utils.isNumeric($scope.proxyEndpoint.httpStatusCode)) {
            showAlert("'HTTP Status Code' for proxy response is required and must be numeric");
            return false;
        }

        // Send proxy response
        var reqData = {
            "path" : $scope.proxyEndpoint.path,
            "method" : $scope.endpoint.method,
            "responseContentType" : $scope.proxyEndpoint.contentType,
            "httpStatusCode" : $scope.proxyEndpoint.httpStatusCode,
            "body" : $scope.proxyEndpoint.responseBody
        };

        restClient.doPost($http, '/proxy', reqData, function(status, data) {

            if (status != 204) {
                 showAlert(globalVars.GeneralErrorMessage);
                 return;
            }

            showAlert("Proxy response successfully posted", "success");

        });

    };

    $scope.doClearProxyFields = function() {

        $scope.proxyEndpoint = {
            "path" : $scope.endpoint.path,
            "contentType" : null,
            "httpStatusCode" : null,
            "responseBody" : null
        };

    };

    $scope.doClearProxyQueue = function() {

        utils.openDeleteConfirmation("Clear all unconsumed proxied responses?", function (alertResponse) {

            if (alertResponse) {

                var reqData = {
                    "path" : null
                };

                restClient.doPatch($http, '/proxy/clear', reqData, function(status, data) {

                     if (status != 204) {
                         showAlert(globalVars.GeneralErrorMessage);
                         return;
                     }
                 });
            }

        });

    };

    var doRefreshActiveWsClientsFunc = function() {

        $scope.activeWsClients = [];

        restClient.doGet($http, '/ws/' + extId + '/client', function(status, data) {

            if (status != 200) {
                showAlert(globalVars.GeneralErrorMessage);
                return;
            }

            for (var d=0; d < data.length; d++) {
                $scope.activeWsClients.push(data[d]);
            }

        });

    };

    $scope.doRefreshActiveWsClients = doRefreshActiveWsClientsFunc;

    var doRefreshActiveSseClientsFunc = function() {

        $scope.activeSseClients = [];

        restClient.doGet($http, '/sse/' + extId + '/client', function(status, data) {

            if (status != 200) {
                showAlert(globalVars.GeneralErrorMessage);
                return;
            }

            for (var d=0; d < data.length; d++) {
                $scope.activeSseClients.push(data[d]);
            }

        });

    };

    $scope.doRefreshActiveSseClients = doRefreshActiveSseClientsFunc;

    $scope.doOpenPushWSMessageView = function(sessionId) {

        var modalInstance = $uibModal.open({
          templateUrl: 'ws_send_message.html',
          controller: 'wsSendMessageController',
          backdrop  : 'static',
          keyboard  : false,
          resolve: {
            data: function () {
              return {
                "path" : getUserCtxPushPath($scope.endpoint),
                "sessionId" : sessionId
              };
            }
          }
        });

        modalInstance.result.then(function () {
            showAlert("Websocket message sent", "success");
        }, function () {

        });

    };

    $scope.doOpenPushSseMessageView = function(sessionId) {

        var modalInstance = $uibModal.open({
          templateUrl: 'sse_send_message.html',
          controller: 'sseSendMessageController',
          backdrop  : 'static',
          keyboard  : false,
          resolve: {
            data: function () {
              return {
                "path" : getUserCtxPushPath($scope.endpoint),
                "sessionId" : sessionId
              };
            }
          }
        });

        modalInstance.result.then(function () {
            showAlert("SSE message sent", "success");
        }, function () {

        });

    };

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
          backdrop  : 'static',
          keyboard  : false,
          resolve: {
            data: function () {
              return {
                "rule" : rule,
                "createdBy" : $scope.endpoint.createdBy
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
          backdrop  : 'static',
          keyboard  : false,
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
          backdrop  : 'static',
          keyboard  : false,
          resolve: {
            data: function () {
              return {
                        "seq" : seq,
                        "createdBy" : $scope.endpoint.createdBy
                     };
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
          backdrop  : 'static',
          keyboard  : false,
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

        if (isNew) {
            return;
        }

        utils.openDeleteConfirmation("Are you sure you wish to delete this endpoint?", function (alertResponse) {

            if (alertResponse) {

                utils.showBlockingOverlay();
                restClient.doDelete($http, '/restmock/' + extId, serverCallbackFunc);

            }

       });

    };

    $scope.doSaveEndpoint = function() {

        // Validation
        if (utils.isBlank($scope.endpoint.path)) {
            showAlert("'Path' is required");
            return;
        }

        if ($scope.endpoint.mockType.value == MockTypeDefinitions.MockTypeRule && !validateRule()) {
            return;
        } else if ($scope.endpoint.mockType.value == MockTypeDefinitions.MockTypeSeq && !validateSeq()) {
            return;
        } else if ($scope.endpoint.mockType.value == MockTypeDefinitions.MockTypeProxyHttp && !validateProxy()) {
            return;
        } else if ($scope.endpoint.mockType.value == MockTypeDefinitions.MockTypeWebSocket && !validateWebSocket()) {
            return;
        } else if ($scope.endpoint.mockType.value == MockTypeDefinitions.MockTypeProxySse && !validateSSE()) {
            return;
        }

        // Send to Server
        utils.showBlockingOverlay();

        var reqData = {
            "path" : $scope.endpoint.path,
            "method" : $scope.endpoint.method,
            "status" : $scope.endpoint.status,
            "mockType" : $scope.endpoint.mockType.value,
            "proxyTimeoutInMillis" : $scope.endpoint.proxyTimeout,
            "webSocketTimeoutInMillis" : $scope.endpoint.webSocketTimeout,
            "sseHeartBeatInMillis" : $scope.endpoint.sseHeartbeat,
            "proxyPushIdOnConnect" : false,
            "randomiseDefinitions" : $scope.endpoint.randomiseDefinitions,
            "definitions" : [],
            "rules" : []
        };

        // Handle Sequence specifics
        if ($scope.endpoint.mockType.value == MockTypeDefinitions.MockTypeSeq) {

            for (var d=0; d < $scope.endpoint.definitions.length; d++) {
                reqData.definitions.push($scope.endpoint.definitions[d]);
            }

            if (countActiveDefinitions(reqData.definitions) < 2) {
                reqData.randomiseDefinitions = false;
            }

        // Handle Rule specifics
        } else if ($scope.endpoint.mockType.value == MockTypeDefinitions.MockTypeRule) {

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

        } else if ($scope.endpoint.mockType.value == MockTypeDefinitions.MockTypeProxyHttp) {
            // Nothing extra to do
         } else if ($scope.endpoint.mockType.value == MockTypeDefinitions.MockTypeWebSocket) {

            reqData.proxyPushIdOnConnect = $scope.endpoint.wsPushIdOnConnect;

        } else if ($scope.endpoint.mockType.value == MockTypeDefinitions.MockTypeProxySse) {

            reqData.proxyPushIdOnConnect = $scope.endpoint.ssePushIdOnConnect;

        }

        if (!isNew) {
            restClient.doPut($http, '/restmock/' + extId, reqData, serverCallbackFunc);
        } else {
            restClient.doPost($http, '/restmock', reqData, serverCallbackFunc);
        }

    };

    $scope.doCancel = function() {

        $location.path("/dashboard").search({
            'tab' : 'HTTP'
        });

        clearEndpointData();
    };


    //
    // Internal Functions
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
                showAlert("You have blank 'Default Response Header' fields. Please amend or remove these");
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
                showAlert("The 'Default Response Header' field '" + rhName + "' is defined more then once");
                return false;
            }

        }

        return true;
    }

    function getUserCtxPushPath(endpoint) {
        return (endpoint.userCtxPath.length > 0) ? "/" + (endpoint.userCtxPath + endpoint.path) : endpoint.path;
    }

    function updateRuleOrderNumbers() {
        for (var r=0; r < $scope.endpoint.rules.length; r++) {
            $scope.endpoint.rules[r].orderNo = (r + 1);
        }
    }

    function updateSeqOrderNumbers() {
        for (var s=0; s < $scope.endpoint.definitions.length; s++) {
            $scope.endpoint.definitions[s].orderNo = (s + 1);
        }
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
            showAlert("'Proxy Timeout' is required and must be numeric");
            return false;
        }

        var timeout = $scope.endpoint.proxyTimeout;

        if (typeof $scope.endpoint.proxyTimeout == 'string') {
            timeout = parseInt($scope.endpoint.proxyTimeout);
        }

        if (timeout > 0
                && timeout < TimeoutDefinitions.MinTimeoutInMillis) {
            showAlert("'Proxy Timeout' must be at least " + TimeoutDefinitions.MinTimeoutInMillis + " milliseconds (i.e " + (TimeoutDefinitions.MinTimeoutInMillis / 1000) + " seconds)");
            return false;
        }

        if (timeout > TimeoutDefinitions.MaxProxyTimeoutInMillis) {
            showAlert("'Proxy Timeout' cannot exceed " + TimeoutDefinitions.MaxProxyTimeoutInMillis + " milliseconds (i.e " + (TimeoutDefinitions.MaxProxyTimeoutInMillis / 1000) + " seconds)");
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

        if (timeout > 0
                && timeout < TimeoutDefinitions.MinTimeoutInMillis) {
            showAlert("'WebSocket Timeout' must be at least " + TimeoutDefinitions.MinTimeoutInMillis + " milliseconds (i.e " + (TimeoutDefinitions.MinTimeoutInMillis / 1000) + " seconds)");
            return false;
        }

        if (timeout > TimeoutDefinitions.MaxWebSocketTimeoutInMillis) {
            showAlert("'WebSocket Timeout' cannot exceed " + TimeoutDefinitions.MaxWebSocketTimeoutInMillis + " milliseconds (i.e " + (TimeoutDefinitions.MaxWebSocketTimeoutInMillis / 1000) + " seconds)");
            return false;
        }

        return true;
    }

    function validateSSE() {

      if (utils.isBlank($scope.endpoint.sseHeartbeat)
                || !utils.isNumeric($scope.endpoint.sseHeartbeat)
                || $scope.endpoint.sseHeartbeat < 1000) {
            showAlert("'SSE Heartbeat' is required and must be numeric and at least 1 second (1000 millis)");
            return false;
        }

        return true;
    }

    var serverCallbackFunc = function (status, data) {

        if (status == 201 || status == 204) {

            checkAutoRefreshStatus(function(autoRefresh) {

                var locParams = {
                    'tab' : 'HTTP'
                };

                if (autoRefresh != null && autoRefresh) {
                    locParams.restart = 'true';
                }

                utils.hideBlockingOverlay();
                $location.path("/dashboard").search(locParams);
                clearEndpointData();
            });

            return;
        }

        utils.hideBlockingOverlay();

        if (status == 400) {
            showAlert(data.message);
            return;
        } else if (status == 409) {
            showAlert("'" + $scope.endpoint.path + "' is already in use");
            return;
        }

        showAlert(globalVars.GeneralErrorMessage);
    };

    function clearEndpointData() {
        $rootScope.endpointData = null;
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

    function lookupMockType(mockType) {
        for (var i=0; i < $scope.mockTypes.length; i++) {
            if (mockType == $scope.mockTypes[i].value) {
                return $scope.mockTypes[i];
            }
        }

        return null;
    }

    // TODO Remove jQuery and replace with directive
    function applyPathFieldEvent() {

        var typingTimer;
        var doneTypingInterval = 500;
        var pathField = jQuery('#path');

        //on keyup, start the countdown
        pathField.on('keyup', function() {
            clearTimeout(typingTimer);
            typingTimer = setTimeout(doneTyping, doneTypingInterval);
        });

        //on keydown, clear the countdown
        pathField.on('keydown', function() {
            clearTimeout(typingTimer);
        });

        function doneTyping() {
            var pathFieldValue = pathField.val();

            if (!pathFieldValue.startsWith("/")) {
                $scope.endpoint.path = "/" + $scope.endpoint.path;
                $scope.$apply();
            }
        }

    }


    //
    // Init Page
    if (!isNew
            && !$scope.readOnly
            && $scope.endpoint.mockType.value == MockTypeDefinitions.MockTypeWebSocket) {
        doRefreshActiveWsClientsFunc();
    } else if (!isNew
            && !$scope.readOnly
            && $scope.endpoint.mockType.value == MockTypeDefinitions.MockTypeProxySse) {
        doRefreshActiveSseClientsFunc();
    }

    applyPathFieldEvent();

});
