
app.controller('tcpDashboardController', function($scope, $rootScope, $window, $location, $timeout, $uibModal, $http, restClient, globalVars, utils, $routeParams, auth) {


    //
    // Constants
    var MockServerRunningStatus = globalVars.MockServerRunningStatus;
    var MockServerStoppedStatus = globalVars.MockServerStoppedStatus;
    var MockServerRestartStatus = globalVars.MockServerRestartStatus;
    var RestfulServerType = globalVars.RestfulServerType;
    var RestartServerRequired = $routeParams.restart;

    var translations = [];
    translations.push({"k" : "SEQ", "v" : "HTTP Sequenced Based"});
    translations.push({ "k" : "RULE", "v" : "HTTP Rules Based" });
    translations.push({ "k" : "PROXY_HTTP", "v" : "HTTP External Feed" });
    translations.push({ "k" : "PROXY_WS", "v" : "WebSocket Proxied" });
    translations.push({ "k" : "PROXY_SSE", "v" : "SSE Proxied" });
    translations.push({ "k" : "CUSTOM_JS", "v" : "Custom JavaScript" });
    translations.push({ "k" : "RULE_WS", "v" : "Websocket Rules Based" });
    translations.push({ "k" : "STATEFUL", "v" : "Stateful REST" });


    //
    // Labels
    $scope.mockServerStatusLabel = 'HTTP Mock Server Status:';
    $scope.serverConfigLabel = 'server config';
    $scope.serverProxyMappingsLabel = 'proxy settings';
    $scope.portLabel = 'port';
    $scope.noDataFoundMsg = 'No Data Found';
    $scope.mockServerRunning = MockServerRunningStatus;
    $scope.mockServerStopped = MockServerStoppedStatus;
    $scope.mockServerRestarting = MockServerRestartStatus;
    $scope.endpointsHeading = 'HTTP Mocks';
    $scope.endpointsOtherUsersHeading = 'HTTP Mocks By Other Users';
    $scope.expandAllEndpointsHeading = 'expand all';
    $scope.collapseAllEndpointsHeading = 'collapse all';
    $scope.selectAllEndpointsHeading = 'select all';
    $scope.deselectAllEndpointsHeading = 'clear selection';
    $scope.hideAllEndpointsHeading = 'hide';
    $scope.searchFilterPlaceHolderTxt = 'Quick Search...';
    $scope.enabledLabel = "Enabled";
    $scope.disabledLabel = "Disabled";
    $scope.allMethodsLabel = "ALL METHODS";
    $scope.ngrokUrlLabel = 'Ngrok URL:';


    //
    // Table Labels
    $scope.pathTableLabel = 'Path';
    $scope.dateCreatedTableLabel = 'Date Created';
    $scope.statusTableLabel = 'Deployment Status';
    $scope.mockTypeTableLabel = 'HTTP Mock Type';
    $scope.actionTableLabel = 'Action';


    //
    // Buttons
    $scope.addEndpointButtonLabel = 'New HTTP Endpoint';
    $scope.importEndpointButtonLabel = 'Import...';
    $scope.exportEndpointButtonLabel = 'Export';
    $scope.viewEndpointButtonLabel = 'View';
    $scope.bulkDeleteEndpointsButtonLabel = 'Delete';


    //
    // Alerts
    function showAlert(msg, type) {
        $scope.$parent.showAlert(msg, type);
    }


    //
    // Data Objects
    var allRestServices = [];
    $scope.isLoggedIn = auth.isLoggedIn();
    $scope.readOnly = (auth.isLoggedIn() && !auth.isAdmin());
    $scope.mockServerStatus = '';
    $scope.restServices = [];
    $scope.mockSelection = [];
    $scope.searchFilter = null;
    $scope.activeStatus = globalVars.ActiveStatus;
    var deletionErrorOccurrence = false;
    var deletionAttemptCount = 0;


    //
    // Scoped Functions
    $scope.doOpenServerConfig = function() {

     var modalInstance = $uibModal.open({
          templateUrl: 'server_config.html',
          controller: 'serverConfigController',
          backdrop  : 'static',
          keyboard  : false,
          resolve: {
            data: function () {
              return { "serverType" : RestfulServerType };
            }
          }
        });

        modalInstance.result.then(function (response) {
            if (response != null
                    && response.restartReq
                    && !$scope.readOnly) {
                RestartServerRequired = true;
                loadTcpServerStatus();
            }
        }, function () {

        });

    };

    $scope.doOpenServerProxyMappings = function() {

     var modalInstance = $uibModal.open({
          templateUrl: 'server_proxy_mappings.html',
          controller: 'serverProxyMappingsController',
          backdrop  : 'static',
          keyboard  : false,
          resolve: {
            data: function () {
              return { "serverType" : RestfulServerType };
            }
          }
        });

        modalInstance.result.then(function (response) {
            if (response != null
                    && response.restartReq
                    && !$scope.readOnly) {
                RestartServerRequired = true;
                loadTcpServerStatus();
            }
        }, function () {

        });

    };

    $scope.translate = function(key) {

        for (var t=0; t < translations.length; t++) {
            if (translations[t].k == key) {
                return translations[t].v;
            }
        }

        return null;
    };

    $scope.doOpenTcpEndpointInfo = function(endpointDataExtId) {
        $location.path("/tcp_endpoint").search({"eid" : endpointDataExtId});
    };

    $scope.doExport = function(mode) {

        if ($scope.mockSelection.length == 0) {
            showAlert("No HTTP mocks have been selected for export");
            return;
        }

        var msgSuffix = ($scope.mockSelection.length == 1)
            ? "this HTTP mock?"
            : ("these " + $scope.mockSelection.length + " HTTP mocks?");

        utils.openWarningConfirmation("Are you sure you wish to export " + msgSuffix, function (alertResponse) {

            if (alertResponse) {

                var req = [];

                for (var m=0; m < $scope.mockSelection.length; m++) {
                    req.push($scope.mockSelection[m].extId);
                }

                restClient.doPost($http, '/mock/export/RESTFUL', req, function(status, data) {

                    if (status != 200) {
                        showAlert(globalVars.GeneralErrorMessage);
                        return;
                    }

                    utils.handleExportDownload(data, "smockin_export_" + $scope.mockSelection.length + "_mocks.zip", "application/zip");
                    $scope.mockSelection = [];

                });

            }

        });

    };

    $scope.doOpenImport = function() {

        var modalInstance = $uibModal.open({
            templateUrl: 'http_import.html',
            controller: 'httpImportController',
            backdrop  : 'static',
            keyboard  : false,
            resolve: {
                data: function () {
                    return { "serverType" : RestfulServerType };
                }
            }
        });

        modalInstance.result.then(function (response) {

            if (response != null) {

                if (response.uploadCompleted != null
                        && response.uploadCompleted) {

                    loadTableData();
                }

            } else {

                $scope.mockSelection = [];
            }

        }, function () {

            $scope.mockSelection = [];
        });

    };

    $scope.startTcpMockServer = function() {

        if ($scope.readOnly) {
            return;
        }

        utils.showLoadingOverlay('Starting HTTP Server');

        restClient.doPost($http, '/mockedserver/rest/start', {}, function(status, data) {

            utils.hideLoadingOverlay();

            if (status == 200) {
                $scope.mockServerStatus = MockServerRunningStatus;

                var alertMsg = "HTTP Server Started (on port " + String(data.port) + ")";

                if (data.nativeProperties != null
                        && data.nativeProperties.PROXY_SERVER_ENABLED == "TRUE") {
                    alertMsg += " with proxy server (on port 8010)";
                }

                showAlert(alertMsg, "success");
                loadTableData();
                return;
            }

            showAlert(globalVars.GeneralErrorMessage);
        });

    };

    $scope.doExpandAllEndpoints = function() {
        doToggleAccordion(true);
    };

    $scope.doCollapseAllEndpoints = function() {
        doToggleAccordion(false);
    };

    $scope.doSelectAllEndpoints = function() {

       $scope.mockSelection = [];

        for (var rs=0; rs < $scope.restServices.length; rs++) {
            for (var rsd=0; rsd < $scope.restServices[rs].data.length; rsd++) {
                $scope.mockSelection.push($scope.restServices[rs].data[rsd]);
            }
        }

    }

    $scope.doClearAllEndpoints = function() {

        $scope.mockSelection = [];
    };

    $scope.stopTcpMockServer = function () {

        if ($scope.readOnly) {
            return;
        }

        utils.showLoadingOverlay('Stopping HTTP Server');

        restClient.doPost($http, '/mockedserver/rest/stop', {}, function(status, data) {

            utils.hideLoadingOverlay();

            if (status == 204) {
                $scope.mockServerStatus = MockServerStoppedStatus;
                showAlert("HTTP Server Stopped", "success");
                loadTableData();
                return;
            }

            showAlert(globalVars.GeneralErrorMessage);
        });

    };

    $scope.doesSelectionContain = function(extId) {

        for (var m=0; m < $scope.mockSelection.length; m++) {
            if ($scope.mockSelection[m].extId == extId) {
                return true;
            }
        }

        return false;
    };

    $scope.toggleSelection = function (mock) {

        for (var m=0; m < $scope.mockSelection.length; m++) {
            if ($scope.mockSelection[m].extId == mock.extId) {
                $scope.mockSelection.splice(m, 1);
                return;
            }
        }

        $scope.mockSelection.push(mock);
    };

    $scope.doDeleteSelection = function() {

        if ($scope.mockSelection.length == 0) {
            showAlert("No HTTP mocks have been selected to delete");
            return;
        }

        var msgSuffix = ($scope.mockSelection.length == 1)
            ? "this HTTP mock?"
            : ("these " + $scope.mockSelection.length + " HTTP mocks?");

        utils.openDeleteConfirmation("Are you sure wish to delete " + msgSuffix, function (alertResponse) {

            if (alertResponse) {

                deletionAttemptCount = 0;
                utils.showBlockingOverlay();

                for (var m=0; m < $scope.mockSelection.length; m++) {
                    restClient.doDelete($http, '/restmock/' + $scope.mockSelection[m].extId, bulkDeleteCallbackFunc);
                }

            }

        });

    };

    $scope.filterHttpMocks = function() {

        $scope.restServices = [];
        $scope.doClearAllEndpoints();

        if ($scope.searchFilter == null
                || $scope.searchFilter.trim() == 0) {

            $scope.restServices = allRestServices;
            return;
        }

        for (var rs=0; rs < allRestServices.length; rs++) {
            for (var rsd=0; rsd < allRestServices[rs].data.length; rsd++) {
                if (allRestServices[rs].data[rsd].path.indexOf($scope.searchFilter) > -1) {
                    batchData($scope.restServices, allRestServices[rs].data[rsd], allRestServices[rs].basePath);
                }
            }
        }

    };

    $scope.copyToClipBoard = function(copyText) {

      navigator.clipboard.writeText(copyText);

      showAlert("URL copied to clipboard", "success");
    };


    //
    // Internal Functions
    function doToggleAccordion(isOpen) {

        for (var rs=0; rs < $scope.restServices.length; rs++) {
            $scope.restServices[rs].isOpen = isOpen;
        }

    }

    var bulkDeleteCallbackFunc = function (status, data) {

        if (status != 204) {
            deletionErrorOccurrence = true;
        }

        deletionAttemptCount++;

        if ($scope.mockSelection.length == deletionAttemptCount) {

            utils.hideBlockingOverlay();

            loadTableData();

            showAlert("The selected HTTP mocks were successfully deleted", "success");
            $scope.mockSelection = [];

            if (deletionErrorOccurrence) {
                showAlert("An error occurred. Not all HTTP mocks were deleted");
            }

            deletionAttemptCount = 0;
        }

    };

    function loadTableData() {

        allRestServices = [];
        $scope.restServices = [];

        restClient.doGet($http, '/restmock', function(status, data) {

            if (status == 401) {
                showAlert(globalVars.AuthRequiredMessage);
                return;
            } else if (status != 200) {
                showAlert(globalVars.GeneralErrorMessage);
                return;
            }

            var filteredData = filterOutStatefulMocks(data);
            allRestServices = batchByBasePath(filteredData);
            $scope.restServices = batchByBasePath(filteredData);
        });

    }

    function filterOutStatefulMocks(allData) {

        var filteredData = [];

        for (var d=0; d < allData.length; d++) {

            var rec = allData[d];

            if (rec.mockType == globalVars.MockTypeDefinitions.MockTypeStateful
                    && !rec.statefulParent) {
                continue;
            }

            filteredData.push(rec);
        }

        return filteredData;
    }

    function batchByBasePath(allData) {

        var batched = [];

        for (var d=0; d < allData.length; d++) {

            var rec = allData[d];
            var path = rec.path;

            var basePathIndex1 = path.indexOf("/", 1);

            var basePath;

            if (basePathIndex1 > -1 && (basePathIndex1 + 1) < path.length) {
                basePath = path.substring(0, basePathIndex1);
            } else {
                basePath = path;
            }

            batchData(batched, rec, basePath);
        }

        return batched;
    }

    function batchData(batched, rec, basePath) {

        var currentBatch = null;

        for (var b=0; b < batched.length; b++) {
            if (batched[b].basePath == basePath) {
                currentBatch = batched[b];
                break;
            }
        }

        if (currentBatch == null) {

            currentBatch = {
                "basePath" : basePath,
                "isOpen" : false,
                "data" : []
            };

            batched.push(currentBatch);
        }

        currentBatch.data.push(rec);
    }

    function loadTcpServerStatus() {

        utils.checkRestServerStatus(function(running, port) {

            if (running == null) {
                showAlert(globalVars.GeneralErrorMessage);
                return;
            }

            if (RestartServerRequired
                    && running
                    && !$scope.readOnly) {

                $scope.mockServerStatus = MockServerRestartStatus;

                restartTcpMockServer(function(port) {

                    if (port != null) {
                        utils.hideLoadingOverlay();
                        $scope.mockServerStatus = MockServerRunningStatus;
                        return;
                    }

                    showAlert(globalVars.GeneralErrorMessage);
                });

                return;
            }

            RestartServerRequired = false;

            $scope.mockServerStatus = (running)
                ? MockServerRunningStatus
                : MockServerStoppedStatus;
        });

    }

    function restartTcpMockServer(callback) {

        if ($scope.readOnly) {
            return;
        }

        utils.showLoadingOverlay('Updating HTTP Server');

        restClient.doPost($http, '/mockedserver/rest/restart', {}, function(status, data) {

            if (status == 200) {
                callback(data.port);
                return;
            }

            callback();
        });

    }

    function checkTunnelState() {

        $rootScope.activeTunnelURL = null;

        restClient.doGet($http, '/tunnel', function(status, data) {

            if (status == 401) {
                showAlert(globalVars.AuthRequiredMessage);
                return;
            } else if (status != 200) {
                showAlert(globalVars.GeneralErrorMessage);
                return;
            }

            if (data.enabled) {
                $rootScope.activeTunnelURL = data.uri;
            }

        });

    }


    //
    // Init page
    loadTableData();
    loadTcpServerStatus();
    checkTunnelState();

});
