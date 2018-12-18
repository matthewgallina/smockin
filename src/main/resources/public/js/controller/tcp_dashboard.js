
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
    $scope.showAllEndpointsHeading = 'display all';
    $scope.expandAllEndpointsHeading = 'expand all';
    $scope.collapseAllEndpointsHeading = 'collapse all';
    $scope.selectAllEndpointsHeading = 'select all';
    $scope.deselectAllEndpointsHeading = 'clear selection';
    $scope.hideAllEndpointsHeading = 'hide';


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
    $scope.isLoggedIn = auth.isLoggedIn();
    $scope.readOnly = (auth.isLoggedIn() && !auth.isAdmin());
    $scope.mockServerStatus = '';
    $scope.restServices = [];
    $scope.otherUserRestServices = [];
    $scope.showAllEndpoints = false;
    $scope.expandAllEndpoints = false;
    $scope.mockSelection = [];
    var deletionErrorOccurrence = false;
    var deletionAttemptCount = 0;


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

    $scope.doExport = function(mode) {

        if ($scope.mockSelection.length == 0) {
            showAlert("No mocks have been selected for export");
            return;
        }

         utils.openWarningConfirmation("Are you sure you wish to export these " + $scope.mockSelection.length + " mocks?", function (alertResponse) {

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

                    handleExportDownload(data);
                    $scope.mockSelection = [];

                });

            }

        });

    };

    $scope.doOpenImport = function() {

        $rootScope.endpointData = null;

        var modalInstance = $uibModal.open({
            templateUrl: 'http_import.html',
            controller: 'httpImportController',
            backdrop  : 'static',
            keyboard  : false
        });

        modalInstance.result.then(function (response) {

            if (response != null) {

                if (response.uploadCompleted != null
                        && response.uploadCompleted) {

                    loadTableData($scope.showAllEndpoints);
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

    $scope.doToggleExpandCollapseEndpoints = function() {
        $scope.expandAllEndpoints = !$scope.expandAllEndpoints;
    };

    $scope.doSelectAllEndpoints = function() {

       $scope.mockSelection = [];

        for (var rs=0; rs < $scope.restServices.length; rs++) {
            for (var rsd=0; rsd < $scope.restServices[rs].data.length; rsd++) {
                $scope.mockSelection.push($scope.restServices[rs].data[rsd]);
            }
        }

        for (var rs=0; rs < $scope.otherUserRestServices.length; rs++) {
            for (var rsd=0; rsd < $scope.otherUserRestServices[rs].data.length; rsd++) {
                $scope.mockSelection.push($scope.otherUserRestServices[rs].data[rsd]);
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
                loadTableData($scope.showAllEndpoints);
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
            showAlert("No mocks have been selected to delete");
            return;
        }

        utils.openDeleteConfirmation("Are you sure wish to delete these " + $scope.mockSelection.length + " mocks?", function (alertResponse) {

            if (alertResponse) {

                deletionAttemptCount = 0;
                utils.showBlockingOverlay();

                for (var m=0; m < $scope.mockSelection.length; m++) {
                    restClient.doDelete($http, '/restmock/' + $scope.mockSelection[m].extId, bulkDeleteCallbackFunc);
                }

            }

        });

    };


    //
    // Internal Functions
    function handleExportDownload(exportData) {

        var mockExportCount = $scope.mockSelection.length;
        var iFrame = jQuery('#export-download-frame');
        var iFrameDoc = iFrame[0].contentDocument || iFrame[0].contentWindow.document;

        var a = iFrameDoc.createElement('a');
        a.download = "smockin_export_" + mockExportCount + "_mocks.zip";
        a.text = "";
        a.href = "data:application/zip;base64," + exportData;

        iFrame.contents().find("body").append(a);
        iFrameDoc.close();

        iFrame.contents().find("body").append(a);

        var clickEvent = iFrameDoc.createEvent("MouseEvent");
        clickEvent.initEvent("click", true, true);
        a.dispatchEvent(clickEvent);

    }

    var bulkDeleteCallbackFunc = function (status, data) {

        if (status != 204) {
            deletionErrorOccurrence = true;
        }

        deletionAttemptCount++;

        if ($scope.mockSelection.length == deletionAttemptCount) {

            utils.hideBlockingOverlay();
            loadTableData($scope.showAllEndpoints = true);

            showAlert("The selected mocks were successfully deleted", "success");
            $scope.mockSelection = [];

            if (deletionErrorOccurrence) {
                showAlert("An error occurred. Not all mocks were deleted");
            }

            deletionAttemptCount = 0;
        }

    };

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
