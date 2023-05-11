
app.controller('tcpEndpointInfoController', function($scope, $location, $uibModal, $http, $timeout, utils, globalVars, restClient, auth) {


    //
    // Constants
    var MockTypeDefinitions = globalVars.MockTypeDefinitions;

    var TimeoutDefinitions = {
        AlertTimeoutMillis : globalVars.AlertTimeoutMillis,
        MinTimeoutInMillis : 10000, // 10 secs
        MaxProxyTimeoutInMillis : 1800000, // 30 mins
        MaxWebSocketTimeoutInMillis : 3600000, // 1 hour
        FormatValidationTimeoutMillis: globalVars.FormatValidationTimeoutMillis
    };

    var extId = $location.search()["eid"];
    var isNew = (extId == null);

    var RestfulServerType = globalVars.RestfulServerType;
    var HttpPathPlaceHolderTxt = 'e.g. (/hello) (path vars: /hello/:name/greeting) (wildcards: /hello/*/greeting)';
    var WebSocketPathPlaceHolderTxt = 'e.g. (/hello/connect)';
    var SsePathPlaceHolderTxt = 'e.g. (/hello)';

    var GetMethod = 'GET';
    var PostMethod = 'POST';
    var PutMethod = 'PUT';
    var DeleteMethod = 'DELETE';
    var AllMethods = 'ALL METHODS';

    $scope.JsonContentType = globalVars.JsonContentType;
    $scope.XmlContentType = globalVars.XmlContentType;

    var jsEditor = null;
    var DefaultCustomJsSyntax = "function handleResponse(request, response) {\n\n  // Reading the Request...\n  // request.path;\n  // request.pathVars.name;\n  // request.body;\n  // var jsonObj = JSON.parse(request.body);\n  // request.headers['X-Inbound-Header'];\n  // request.parameters['first-name'];\n  // request.parameters.lastName;\n\n  // Parsing XML... (e.g. <xml foo=\"FOO\"><bar><baz>BAZ</baz></bar></xml>)\n  // var data = fromXML(request.body);\n  // var bazValue = data.xml.bar.baz; // returns \"BAZ\"\n  // var fooValue = data.xml[\"@foo\"]; // returns \"FOO\"\n\n  // Setting the Response...\n  // response.contentType = 'application/json';\n  // response.status = 200;\n  // response.body = '{ \"msg\" : \"hello world...\" }';\n  // response.headers['X-Outbound-Header'] = 'foobar';\n\n  // Reading in Key/Value data...\n  // lookUpKvp('name');\n  // lookUpKvp(request.parameters.lastname);\n\n  return response;\n}";


    //
    // Labels
    $scope.mockTypeSeq = MockTypeDefinitions.MockTypeSeq;
    $scope.mockTypeRule = MockTypeDefinitions.MockTypeRule;
    $scope.mockTypeProxyHttp = MockTypeDefinitions.MockTypeProxyHttp;
    $scope.mockTypeWebSocket = MockTypeDefinitions.MockTypeWebSocket;
    $scope.mockTypeProxySse = MockTypeDefinitions.MockTypeProxySse;
    $scope.mockTypeCustomJs = MockTypeDefinitions.MockTypeCustomJs;
    $scope.mockTypeRuleWs = MockTypeDefinitions.MockTypeRuleWs;
    $scope.mockTypeStateful = MockTypeDefinitions.MockTypeStateful;
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
    $scope.initialDataStateLabel = 'Initial Data State';
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
    $scope.pushBodyLabel = 'Body';
    $scope.responseHeadersLabel = 'Default Response Headers';
    $scope.responseHeaderNameLabel = 'Name';
    $scope.responseHeaderValueLabel = 'Value';
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
    $scope.manageHttpProxyQueueLabel = 'HTTP External Feed Tools';
    $scope.sseHeartbeatLabel = 'SSE Heartbeat (in millis)';
    $scope.sseHeartbeatPlaceholderTxt = 'Interval at which responses are pushed to the client';
    $scope.pushIdOnConnectLabel = 'Send Session Id on connect';
    $scope.actualPathPrefixLabel = "actual path:";
    $scope.enableRandomLatencyLabel = "Delay responses";
    $scope.latencyRangeLabel = "Latency Range (in millis)";
    $scope.latencyRangeMinLabel = "min";
    $scope.latencyRangeMaxLabel = "max";
    $scope.enabledLabel = "Enabled";
    $scope.disabledLabel = "Disabled";
    $scope.formatJsonLabel = 'Validate & Format JSON';
    $scope.formatXmlLabel = 'Validate & Format XML';
    $scope.customJsSyntaxLabel = 'Javascript Logic';
    $scope.editCustomJSLabel = 'Open Editor';
    $scope.proxyPushToQueueEndpoint = 'http://localhost:8000/proxy/' + extId;
    $scope.option1Label = 'Option 1';
    $scope.option2Label = 'Option 2';
    $scope.option1Text = 'Endpoint for pushing responses to queue';
    $scope.option2Text = 'Input form for pushing responses to queue';
    $scope.statefulIdFieldNameLabel = 'ID Field Name';
    $scope.statefulIdFieldLocationLabel = 'JSON Path to ID Field';
    $scope.statefulIdFieldLocationPlaceholderLabel = 'e.g use data.id where your json format is { data : [{ "id" : "123" }] }';


    //
    // Buttons
    $scope.saveButtonLabel = 'Save';
    $scope.deleteButtonLabel = 'Delete';
    $scope.closeButtonLabel = 'Close';
    $scope.addRuleButtonLabel = 'Add Rule';
    $scope.addSequenceButtonLabel = 'Add Seq Response';
    $scope.viewButtonLabel = "View";
    $scope.removeResponseHeaderButtonLabel = 'X';
    $scope.addResponseHeaderButtonLabel = 'New Row';
    $scope.refreshClientsLinkLabel = 'refresh';
    $scope.clearProxyButtonLabel = 'Clear All Responses In Queue';
    $scope.clearProxyResponseFieldsButtonLabel = 'Clear Fields';
    $scope.postProxyResponseButtonLabel = 'Post Response';
    $scope.messageButtonLabel = 'Message';
    $scope.clearStateButtonLabel = 'Reset Data State';


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

        window.scrollTo({ top: 0, behavior: 'smooth' });
    }

    $scope.closeAlert = closeAlertFunc;


    //
    // Data Objects
    $scope.activeStatus = globalVars.ActiveStatus;
    $scope.inActiveStatus = globalVars.InActiveStatus;

    $scope.contentTypes = globalVars.ContentMimeTypes;
    $scope.httpMethods = globalVars.httpMethods;

    $scope.responseBodyLimit = 100;

    $scope.mockTypes = [
       { "name" : "HTTP Sequenced Based", "value" : MockTypeDefinitions.MockTypeSeq },
       { "name" : "HTTP Rules Based", "value" : MockTypeDefinitions.MockTypeRule },
       { "name" : "HTTP External Feed", "value" : MockTypeDefinitions.MockTypeProxyHttp },
       { "name" : "Custom JavaScript", "value" : MockTypeDefinitions.MockTypeCustomJs },
       { "name" : "SSE Proxied", "value" : MockTypeDefinitions.MockTypeProxySse },
       { "name" : "WebSocket Proxied", "value" : MockTypeDefinitions.MockTypeWebSocket },
       { "name" : "WebSocket Rules Based", "value" : MockTypeDefinitions.MockTypeRuleWs },
       { "name" : "Stateful REST", "value" : MockTypeDefinitions.MockTypeStateful }
    ];

    $scope.isNew = isNew;
    $scope.lastUpdateAction = null;

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
        "randomiseLatency" : false,
        "randomiseLatencyRangeMinMillis" : 0,
        "randomiseLatencyRangeMaxMillis" : 0,
        "definitions" : [],
        "customJsSyntax" : null,
        "rules" : [],
        "statefulIdFieldName" : "id",
        "statefulIdFieldLocation" : null
    };

    $scope.proxyEndpoint = {
        "contentType" : null,
        "httpStatusCode" : null,
        "responseBody" : null
    };

    $scope.defaultCtxPathPrefix = (auth.isLoggedIn() && !auth.isSysAdmin()) ? ('/' + auth.getUserName()) : null;

    // Populate form if viewing existing record...
    if (!isNew) {

        var handleLoadedMock = function(endpoint) {

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
                "randomiseLatency" : endpoint.randomiseLatency,
                "randomiseLatencyRangeMinMillis" : endpoint.randomiseLatencyRangeMinMillis,
                "randomiseLatencyRangeMaxMillis" : endpoint.randomiseLatencyRangeMaxMillis,
                "definitions" : endpoint.definitions,
                "customJsSyntax" : endpoint.customJsSyntax,
                "rules" : endpoint.rules,
                "createdBy" : endpoint.createdBy,
                "statefulIdFieldName" : endpoint.statefulIdFieldName,
                "statefulIdFieldLocation" : endpoint.statefulIdFieldLocation
            };

            $scope.defaultCtxPathPrefix = (!utils.isBlank(endpoint.userCtxPath)) ? ('/' + endpoint.userCtxPath) : null;

            if (endpoint.mockType == MockTypeDefinitions.MockTypeSeq
            		|| endpoint.mockType == MockTypeDefinitions.MockTypeRuleWs
                    || endpoint.mockType == MockTypeDefinitions.MockTypeRule) {

                $scope.endpoint.contentType = endpoint.definitions[0].responseContentType;
                $scope.endpoint.httpStatusCode = endpoint.definitions[0].httpStatusCode;
                $scope.endpoint.responseBody = endpoint.definitions[0].responseBody;

                angular.forEach(endpoint.definitions[0].responseHeaders, function(v, k) {
                    $scope.responseHeaderList.push({ 'name' : k, 'value' : v });
                });

            }

            if (endpoint.mockType == MockTypeDefinitions.MockTypeCustomJs) {
            	initJSEditor(endpoint.customJsSyntax);
            }

            if (endpoint.mockType == MockTypeDefinitions.MockTypeStateful) {
                $scope.endpoint.method = AllMethods;
                $scope.endpoint.contentType = globalVars.JsonContentType;
                $scope.endpoint.responseBody = endpoint.statefulDefaultResponseBody;
            }

            $scope.readOnly = (auth.isLoggedIn() && auth.getUserName() != $scope.endpoint.createdBy);

        };

        loadMockData(extId, handleLoadedMock);
    }

    $scope.readOnly = (!isNew && auth.isLoggedIn() && auth.getUserName() != $scope.endpoint.createdBy);


    //
    // Scoped Functions
    $scope.doSelectMockType = function(et) {

        if ($scope.endpoint.mockType.value == MockTypeDefinitions.MockTypeWebSocket
                || $scope.endpoint.mockType.value == MockTypeDefinitions.MockTypeProxySse
                || $scope.endpoint.mockType.value == MockTypeDefinitions.MockTypeRuleWs
                || $scope.endpoint.mockType.value == MockTypeDefinitions.MockTypeStateful) {
            $scope.endpoint.method = null;
        }

        $scope.endpoint.mockType = et;

        switch (et.value) {
            case MockTypeDefinitions.MockTypeWebSocket :
                $scope.pathPlaceHolderTxt = WebSocketPathPlaceHolderTxt;
                $scope.endpoint.method = GetMethod;
                break;
                
            case MockTypeDefinitions.MockTypeProxySse :
                $scope.pathPlaceHolderTxt = SsePathPlaceHolderTxt;
                $scope.endpoint.method = GetMethod;
                break;
                
            case MockTypeDefinitions.MockTypeCustomJs :

                if ($scope.endpoint.customJsSyntax == null) {
                    $scope.endpoint.customJsSyntax = DefaultCustomJsSyntax;
                }

                initJSEditor($scope.endpoint.customJsSyntax);

                break;
            case MockTypeDefinitions.MockTypeRuleWs :
            	$scope.pathPlaceHolderTxt = WebSocketPathPlaceHolderTxt;
                $scope.endpoint.method = GetMethod;

            	break;
            case MockTypeDefinitions.MockTypeStateful :
                $scope.endpoint.method = AllMethods;
                $scope.endpoint.contentType = "application/json";
                $scope.endpoint.responseBody = "[]";

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
        if (utils.isBlank($scope.proxyEndpoint.contentType)) {
            showAlert("'Content Type' for response is required");
            return false;
        }
        if (utils.isBlank($scope.proxyEndpoint.httpStatusCode)
                || !utils.isNumeric($scope.proxyEndpoint.httpStatusCode)) {
            showAlert("'HTTP Status Code' for response is required and must be numeric");
            return false;
        }

        // Send external feed response
        var reqData = {
            "path" : $scope.endpoint.path,
            "method" : $scope.endpoint.method,
            "responseContentType" : $scope.proxyEndpoint.contentType,
            "httpStatusCode" : $scope.proxyEndpoint.httpStatusCode,
            "body" : $scope.proxyEndpoint.responseBody
        };

        restClient.doPost($http, '/proxy/' + extId, reqData, function(status, data) {

            if (status != 204) {
                 showAlert(globalVars.GeneralErrorMessage);
                 return;
            }

            showAlert("Response successfully posted", "success");

        });

    };

    $scope.doClearProxyFields = function() {

        $scope.proxyEndpoint = {
            "contentType" : null,
            "httpStatusCode" : null,
            "responseBody" : null
        };

    };

    $scope.doClearProxyQueue = function() {

        utils.openDeleteConfirmation("Clear all unconsumed responses?", function (alertResponse) {

            if (alertResponse) {

                var reqData = {
                    "path" : null
                };

                restClient.doPatch($http, '/proxy/' + extId + '/clear', reqData, function(status, data) {

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
                "mockType" : $scope.endpoint.mockType.value,
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
              return {
                  "mockType" : $scope.endpoint.mockType.value
              };
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

                $scope.lastUpdateAction = DeleteMethod;
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

        if (($scope.endpoint.mockType.value == MockTypeDefinitions.MockTypeRule || $scope.endpoint.mockType.value == MockTypeDefinitions.MockTypeRuleWs) && !validateRule()) {
            return;
        } else if ($scope.endpoint.mockType.value == MockTypeDefinitions.MockTypeSeq && !validateSeq()) {
            return;
        } else if ($scope.endpoint.mockType.value == MockTypeDefinitions.MockTypeProxyHttp && !validateProxy()) {
            return;
        } else if ($scope.endpoint.mockType.value == MockTypeDefinitions.MockTypeWebSocket && !validateWebSocket()) {
            return;
        } else if ($scope.endpoint.mockType.value == MockTypeDefinitions.MockTypeProxySse && !validateSSE()) {
            return;
        } else if ($scope.endpoint.mockType.value == MockTypeDefinitions.MockTypeCustomJs && !validateCustomJS()) {
            return;
        } else if ($scope.endpoint.mockType.value == MockTypeDefinitions.MockTypeStateful && !validateStateful()) {
            return;
        }

        if ($scope.endpoint.randomiseLatency) {

            if ($scope.endpoint.randomiseLatencyRangeMinMillis < 0) {
                showAlert("Latency 'min' value cannot be smaller then zero");
                return;
            }
            if ($scope.endpoint.randomiseLatencyRangeMaxMillis < 100) {
                showAlert("Latency 'max' value cannot be shorter than 100 milliseconds");
                return;
            }
            if ($scope.endpoint.randomiseLatencyRangeMinMillis > $scope.endpoint.randomiseLatencyRangeMaxMillis) {
                showAlert("Latency 'min' value cannot be greater than 'max' value");
                return;
            }

        } else {

            $scope.endpoint.randomiseLatencyRangeMinMillis = 0;
            $scope.endpoint.randomiseLatencyRangeMaxMillis = 0;
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
            "randomiseLatency" : $scope.endpoint.randomiseLatency,
            "randomiseLatencyRangeMinMillis" : $scope.endpoint.randomiseLatencyRangeMinMillis,
            "randomiseLatencyRangeMaxMillis" : $scope.endpoint.randomiseLatencyRangeMaxMillis,
            "definitions" : [],
            "rules" : [],
            "customJsSyntax" : null,
            "statefulDefaultResponseBody" : null
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
        } else if ($scope.endpoint.mockType.value == MockTypeDefinitions.MockTypeRule || $scope.endpoint.mockType.value == MockTypeDefinitions.MockTypeRuleWs) {

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
            angular.copy($scope.endpoint.rules, reqData.rules); // deep copy rules array

            for (var r=0; r < reqData.rules.length; r++) {

                // Convert all local rule arg objects to DTO
                for (var g=0; g < reqData.rules[r].groups.length; g++) {
                    reqData.rules[r].groups[g].conditions = utils.convertToDTO(reqData.rules[r].groups[g].conditions);
                }
            }

        } else if ($scope.endpoint.mockType.value == MockTypeDefinitions.MockTypeProxyHttp) {

            // Nothing extra to do

        } else if ($scope.endpoint.mockType.value == MockTypeDefinitions.MockTypeWebSocket) {

            reqData.proxyPushIdOnConnect = $scope.endpoint.wsPushIdOnConnect;

        } else if ($scope.endpoint.mockType.value == MockTypeDefinitions.MockTypeProxySse) {

            reqData.proxyPushIdOnConnect = $scope.endpoint.ssePushIdOnConnect;

        } else if ($scope.endpoint.mockType.value == MockTypeDefinitions.MockTypeCustomJs) {

            reqData.customJsSyntax = jsEditor.getValue();

        } else if ($scope.endpoint.mockType.value == MockTypeDefinitions.MockTypeStateful) {

            // Use GET as a placeholder for stateful mock parent
            reqData.method = 'GET';
            reqData.statefulDefaultResponseBody = (!utils.isBlank($scope.endpoint.responseBody)) ? $scope.endpoint.responseBody : "[]";
            reqData.statefulIdFieldName = (!utils.isBlank($scope.endpoint.statefulIdFieldName)) ? $scope.endpoint.statefulIdFieldName : "id";
//            reqData.statefulIdFieldLocation = (!utils.isBlank($scope.endpoint.statefulIdFieldLocation)) ? $scope.endpoint.statefulIdFieldLocation : reqData.statefulIdFieldName;
            reqData.statefulIdFieldLocation = reqData.statefulIdFieldName;
        }

        if (!isNew) {
            $scope.lastUpdateAction = PutMethod;
            restClient.doPut($http, '/restmock/' + extId, reqData, serverCallbackFunc);
        } else {
            $scope.lastUpdateAction = PostMethod;
            restClient.doPost($http, '/restmock', reqData, serverCallbackFunc);
        }

    };

    $scope.doClose = function() {

        if ($scope.lastUpdateAction == null) {
            $location.path("/dashboard").search({});
            return;
        }

        utils.showBlockingOverlay();

        checkAutoRefreshStatus(function(autoRefresh) {

            var locParams = {};

            if (autoRefresh != null && autoRefresh) {
                locParams.restart = 'true';
            }

            utils.hideBlockingOverlay();
            $location.path("/dashboard").search(locParams);
        });

    };

    $scope.doFormatJson = function() {

        $scope.closeAlert();

        if ($scope.endpoint.responseBody == null) {
            return;
        }

        var validationOutcome = utils.validateJson($scope.endpoint.responseBody);

        if (validationOutcome != null) {
            showAlert(validationOutcome, 'danger', TimeoutDefinitions.FormatValidationTimeoutMillis);
            return;
        }

        $scope.endpoint.responseBody = utils.formatJson($scope.endpoint.responseBody);
    };

    $scope.doFormatXml = function() {

        $scope.closeAlert();

        if ($scope.endpoint.responseBody == null) {
            return;
        }

        var validationOutcome = utils.validateAndFormatXml($scope.endpoint.responseBody);

        if (validationOutcome == null) {
            showAlert("Unable to format XML. Invalid syntax", 'danger', TimeoutDefinitions.FormatValidationTimeoutMillis);
            return;
        }

        if (validationOutcome[0] == 'ERROR') {
            showAlert("Unable to format XML: " + validationOutcome[1], 'danger', TimeoutDefinitions.FormatValidationTimeoutMillis);
            return;
        }

        $scope.endpoint.responseBody = validationOutcome[1];
    };

    $scope.doFormatProxyJson = function() {

        $scope.closeAlert();

        if ($scope.proxyEndpoint.responseBody == null) {
            return;
        }

        var validationOutcome = utils.validateJson($scope.proxyEndpoint.responseBody);

        if (validationOutcome != null) {
            showAlert(validationOutcome, 'danger', TimeoutDefinitions.FormatValidationTimeoutMillis);
            return;
        }

        $scope.proxyEndpoint.responseBody = utils.formatJson($scope.proxyEndpoint.responseBody);
    };

    $scope.doFormatProxyXml = function() {

        $scope.closeAlert();

        if ($scope.proxyEndpoint.responseBody == null) {
            return;
        }

        var validationOutcome = utils.validateAndFormatXml($scope.proxyEndpoint.responseBody);

        if (validationOutcome == null) {
            showAlert("Unable to format XML. Invalid syntax", 'danger', TimeoutDefinitions.FormatValidationTimeoutMillis);
            return;
        }

        if (validationOutcome[0] == 'ERROR') {
            showAlert("Unable to format XML: " + validationOutcome[1], 'danger', TimeoutDefinitions.FormatValidationTimeoutMillis);
            return;
        }

        $scope.proxyEndpoint.responseBody = validationOutcome[1];
    };

    $scope.doExpandJsEditor = function() {

        var modalInstance = $uibModal.open({
          templateUrl: 'js_editor.html',
          controller: 'jsEditorController',
          backdrop  : 'static',
          keyboard  : false,
          resolve: {
            data: function () {
              return {
                "customJsSyntax" : jsEditor.getValue()
              };
            }
          }
        });

        modalInstance.result.then(function (editorContent) {
            jsEditor.setValue(editorContent);
        }, function () {

        });

    };

    $scope.doClearDataState = function() {

        utils.openWarningConfirmation("Clear all data state for this mock?", function (alertResponse) {

            if (alertResponse) {

                var reqData = {};

                restClient.doPost($http, '/stateful/' + extId + '/clear', reqData, function(status, data) {

                    if (status != 204) {
                         showAlert(globalVars.GeneralErrorMessage);
                         return;
                    }

                    showAlert("State data has been successfully cleared", "success");

                });

            }

        });

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
        return ($scope.defaultCtxPathPrefix != null)
            ? ($scope.defaultCtxPathPrefix + endpoint.path)
            : endpoint.path;
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

    function validateCustomJS() {

      if (utils.isBlank($scope.endpoint.method)) {
        showAlert("'Method' is required");
        return false;
      }

      if (utils.isBlank(jsEditor.getValue())) {
        showAlert("'Javascript logic' is required");
        return false;
      }

      return true;
    }

    function validateStateful() {

        var path = $scope.endpoint.path;
        var lastSlash = path.lastIndexOf("/");

        if (lastSlash != -1 && path.substring(lastSlash).indexOf(":") != -1) {
            showAlert("Stateful mock cannot end with a path variable");
            return false;
        }

        if (!utils.isBlank($scope.endpoint.responseBody)
                && utils.validateJson($scope.endpoint.responseBody) != null) {
            showAlert("'Initial Data State' must contain a valid JSON value");
            return false;
        }

        if ($scope.endpoint.statefulIdFieldName != null
                && !utils.isAlpha($scope.endpoint.statefulIdFieldName)) {
            showAlert("'ID Field Name' may only contain a single character name");
            return false;
        }

        /*
        if (!utils.isBlank($scope.endpoint.statefulIdFieldName)
                && !utils.isBlank($scope.endpoint.statefulIdFieldLocation)) {

            var locPos = $scope.endpoint.statefulIdFieldLocation.lastIndexOf(".");

            var idLocationValue = (locPos > -1)
                ? $scope.endpoint.statefulIdFieldLocation.substring(locPos + 1)
                : $scope.endpoint.statefulIdFieldLocation;

            if (idLocationValue != $scope.endpoint.statefulIdFieldName) {
                showAlert("ID field name '" + $scope.endpoint.statefulIdFieldName + "' and ID in location '" + idLocationValue + "' do not match");
                return false;
            }
        }
        */

        return true;
    }

    var serverCallbackFunc = function (status, data) {

        if (status == 201 || status == 204) {

            if ($scope.lastUpdateAction == PutMethod) {
                utils.hideBlockingOverlay();
                showAlert("Changes saved", "success");
                return;
            }

            checkAutoRefreshStatus(function(autoRefresh) {

                var locParams = {};

                if (autoRefresh != null && autoRefresh) {
                    locParams.restart = 'true';
                }

                utils.hideBlockingOverlay();
                $location.path("/dashboard").search(locParams);
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

    /*
    function countActiveRules(rules) {

        var activeRules = 0;

        for (var r=0; r < rules.length; r++) {
            if (!rules[r].suspend) {
                activeRules++;
            }
        }

        return activeRules;
    }
    */

    function lookupMockType(mockType) {
        for (var i=0; i < $scope.mockTypes.length; i++) {
            if (mockType == $scope.mockTypes[i].value) {
                return $scope.mockTypes[i];
            }
        }

        return null;
    }

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

    function loadMockData(extId, callback) {

        restClient.doGet($http, '/restmock/' + extId, function(status, data) {

            if (status != 200) {
                showAlert(globalVars.GeneralErrorMessage);
                return;
            }

            callback(data);
        });

    }

    function initJSEditor(content) {

        if (jsEditor != null) {
            return;
        }

        jsEditor = CodeMirror.fromTextArea(document.getElementById('custom-js-syntax'), {
          lineNumbers: true,
          mode: "javascript",
          readOnly: 'nocursor'
        });

        jsEditor.setOption("theme", "darcula");

        updateJSEditor(content);
    }

    function updateJSEditor(content) {
        $timeout(function () {
            if (content != null) {
                jsEditor.setValue(content);
            }
            jsEditor.refresh();
        }, 500);
    }


    //
    // Init Page
    if (!isNew
            && !$scope.readOnly
            && ($scope.endpoint.mockType.value == MockTypeDefinitions.MockTypeWebSocket || 
            	$scope.endpoint.mockType.value == MockTypeDefinitions.MockTypeRuleWs
            		)) {
        doRefreshActiveWsClientsFunc();
    } else if (!isNew
            && !$scope.readOnly
            && $scope.endpoint.mockType.value == MockTypeDefinitions.MockTypeProxySse) {
        doRefreshActiveSseClientsFunc();
    }

    applyPathFieldEvent();

});
