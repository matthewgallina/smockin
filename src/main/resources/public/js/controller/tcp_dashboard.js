
app.controller('tcpDashboardController', function($scope, $window, $rootScope, $location, $timeout, $uibModal, $http, restClient, globalVars, utils, $routeParams, auth) {


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


    //
    // Labels
    $scope.mockServerStatusLabel = 'HTTP Mock Server Status:';
    $scope.serverConfigLabel = '(edit settings)';
    $scope.portLabel = 'port';
    $scope.noDataFoundMsg = 'No Data Found';
    $scope.mockServerRunning = MockServerRunningStatus;
    $scope.mockServerStopped = MockServerStoppedStatus;
    $scope.mockServerRestarting = MockServerRestartStatus;
    $scope.endpointsHeading = 'HTTP Mocks';
    $scope.endpointsOtherUsersHeading = 'Other User Endpoints';
    $scope.showAllEndpointsHeading = 'display other user endpoints';
    $scope.hideAllEndpointsHeading = 'hide';
    $scope.cancelExportSelectionButtonLabel = 'Cancel Export Selection';
    $scope.completeExportSelectionButtonLabel = 'Finish Export Selection';


    //
    // Buttons
    $scope.addEndpointButtonLabel = 'New HTTP Endpoint';
    $scope.importExportEndpointButtonLabel = 'Import / Export';
    $scope.viewEndpointButtonLabel = 'View';


    //
    // Alerts
    function showAlert(msg, type) {
        $scope.$parent.showAlert(msg, type);
    }


    //
    // Data Objects
    $scope.isLoggedIn = auth.isLoggedIn();
    $scope.readOnly = (auth.isLoggedIn() && !auth.isAdmin());
    $scope.mockServerStatus = '';
    $scope.restServices = [];
    $scope.otherUserRestServices = [];
    $scope.showAllEndpoints = false;
    $scope.initExportSelection = false;
    $scope.exportSelection = [];


    //
    // Endpoints Table
    $scope.pathTableLabel = 'Path';
    $scope.dateCreatedTableLabel = 'Date Created';
    $scope.createdByTableLabel = 'Created By';
    $scope.statusTableLabel = 'Deployment Status';
    $scope.mockTypeTableLabel = 'HTTP Mock Type';
    $scope.actionTableLabel = 'Action';


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

    $scope.translate = function(key) {

        for (var t=0; t < translations.length; t++) {
            if (translations[t].k == key) {
                return translations[t].v;
            }
        }

        return null;
    };

    $scope.doOpenTcpEndpointInfo = function(endpointData) {
        $rootScope.endpointData = endpointData;
        $location.path("/tcp_endpoint");
    };

    $scope.doOpenImportExport = function(mode) {

        $rootScope.endpointData = null;

        var modalInstance = $uibModal.open({
            templateUrl: 'http_import_export.html',
            controller: 'httpImportExportController',
            backdrop  : 'static',
            keyboard  : false,
            resolve: {
                data: function () {
                  return {
                            "exportSelection" : $scope.exportSelection,
                            "mode" : mode
                         };
                }
            }
        });

        modalInstance.result.then(function (response) {

            $scope.initExportSelection = false;

            if (response != null) {

                if (response.uploadCompleted != null
                        && response.uploadCompleted) {

                    loadTableData($scope.showAllEndpoints);

                } else if (response.initExportSelection != null
                            && response.initExportSelection) {

                    $scope.showAllEndpoints = true;
                    loadTableData($scope.showAllEndpoints);
                    $scope.initExportSelection = true;

                } else if (response.generatedExportData != null) {

                    console.log(response.generatedExportData.contentType);
                    console.log(response.generatedExportData.content);

                }
            }

        }, function () {

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
                loadTableData($scope.showAllEndpoints);
                return;
            }

            var errorMsg = globalVars.GeneralErrorMessage;

            if (status == 400
                    && data.message == globalVars.ERROR_PROXY_PATH_CONFLICT) {
                errorMsg = "Cannot start proxy server. Path conflicts were found. Opening priority path manager...";
                $timeout(function() {
                    openProxyPathConflictManager();
                }, 3000);
            }

            showAlert(errorMsg);
        });

    };

    $scope.doShowAllEndpoints = function(show) {
        $scope.showAllEndpoints = show;
        loadTableData($scope.showAllEndpoints);
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
                loadTableData($scope.showAllEndpoints);
                return;
            }

            showAlert(globalVars.GeneralErrorMessage);
        });

    };

    $scope.toggleExportSelection = function (externalId) {

        var exportIdx = $scope.exportSelection.indexOf(externalId);

        if (exportIdx > -1) {
            $scope.selection.splice(exportIdx, 1);
            return;
        }

        $scope.exportSelection.push(externalId);
    };

    $scope.doCancelExportSelection = function() {

        $scope.exportSelection = [];
        $scope.initExportSelection = false;

        $scope.doOpenImportExport('EXPORT');
    };

    $scope.doCompleteExportSelection = function() {

        if ($scope.exportSelection.length == 0) {
            showAlert("Please select at 1 mock to export");
            return;
        }

        $scope.initExportSelection = false;

        $scope.doOpenImportExport('EXPORT');
    };


    //
    // Internal Functions
    function loadTableData(showAll) {

        $scope.restServices = [];
        $scope.otherUserRestServices = [];

        var filterParams = (showAll) ? '?filter=all' : '';

        restClient.doGet($http, '/restmock' + filterParams, function(status, data) {

            if (status == 401) {
                showAlert(globalVars.AuthRequiredMessage);
                return;
            } else if (status != 200) {
                showAlert(globalVars.GeneralErrorMessage);
                return;
            }

            var splitData = splitUserData(data);

            $scope.otherUserRestServices = batchByBasePath(splitData.other);
            $scope.restServices = batchByBasePath(splitData.own);
        });

    }

    function splitUserData(allData) {

        var splitOutData = {
            "own" : [],
            "other" : []
        };

        if (!auth.isLoggedIn()) {

            splitOutData.own = allData;

        } else {

            for (var d=0; d < allData.length; d++) {
                if (allData[d].createdBy == auth.getUserName()) {
                    splitOutData.own.push(allData[d]);
                } else {
                    splitOutData.other.push(allData[d]);
                }
            }

        }

        return splitOutData;
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
                        $window.location.href = '/templates/main.html';
                        return;
                    }

                    showAlert(globalVars.GeneralErrorMessage);
                });

                return;
            }

            RestartServerRequired = false;
            $scope.mockServerStatus = (running)?MockServerRunningStatus:MockServerStoppedStatus;
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

    function openProxyPathConflictManager() {

        var modalInstance = $uibModal.open({
            templateUrl: 'proxy_path_conflict_manager.html',
            controller: 'proxyPathConflictManagerController',
            backdrop  : 'static',
            keyboard  : false
        });

        modalInstance.result.then(function (response) {

        }, function () {

        });

    }


    //
    // Init page
    loadTableData(false);
    loadTcpServerStatus();

});
