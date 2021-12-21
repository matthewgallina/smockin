
app.controller('s3DashboardController', function($scope, $window, $rootScope, $location, $timeout, $uibModal, $http, restClient, globalVars, utils, $routeParams, auth) {


    //
    // Constants
    var MockServerRunningStatus = globalVars.MockServerRunningStatus;
    var MockServerStoppedStatus = globalVars.MockServerStoppedStatus;
    var MockServerRestartStatus = globalVars.MockServerRestartStatus;
    var RestartServerRequired = $routeParams.restart;
    var S3ServerType = globalVars.S3ServerType;


    //
    // Labels
    $scope.mockServerStatusLabel = 'S3 Mock Server Status:';
    $scope.serverConfigLabel = 'server config';
    $scope.noDataFoundMsg = 'No Data Found';
    $scope.selectAllEndpointsHeading = 'select all';
    $scope.deselectAllEndpointsHeading = 'clear selection';
    $scope.enabledLabel = "Enabled";
    $scope.disabledLabel = "Disabled";
    $scope.searchFilterPlaceHolderTxt = 'Quick Search...';
    var syncModeNoneTableLabel = 'No Sync';
    var syncModeOneWayTableLabel = 'One Way';
    var syncModeBiDirectionalTableLabel = 'Bi-Directional';


    //
    // Table Labels
    $scope.bucketTableLabel = 'Bucket Name';
    $scope.dateCreatedTableLabel = 'Date Created';
    $scope.statusTableLabel = 'Deployment Status';
    $scope.syncModeTableLabel = 'Sync Mode';
    $scope.actionTableLabel = 'Action';


    //
    // Buttons
    $scope.addS3BucketButtonLabel = 'New S3 Bucket';
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
    var allS3Services = [];
    $scope.isLoggedIn = auth.isLoggedIn();
    $scope.readOnly = (auth.isLoggedIn() && !auth.isAdmin());
    $scope.mockServerStatus = '';
    $scope.s3Services = [];
    $scope.mockSelection = [];
    $scope.searchFilter = null;
    $scope.activeStatus = globalVars.ActiveStatus;
    $scope.mockServerRunning = MockServerRunningStatus;
    $scope.mockServerStopped = MockServerStoppedStatus;
    $scope.mockServerRestarting = MockServerRestartStatus;
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
              return { "serverType" : S3ServerType };
            }
          }
        });

        modalInstance.result.then(function (response) {
            if (response != null
                    && response.restartReq
                    && !$scope.readOnly) {
                RestartServerRequired = true;
                loadS3ServerStatus();
            }
        }, function () {

        });

    };

    $scope.doOpenS3EndpointInfo = function(endpointDataExtId) {
        $location.path("/s3_endpoint").search({
            "dv" : globalVars.S3ServerMode,
            "eid" : endpointDataExtId
        });
    };

    $scope.doSelectAllEndpoints = function() {

       $scope.mockSelection = [];

        for (var rs=0; rs < $scope.s3Services.length; rs++) {
            $scope.mockSelection.push($scope.s3Services[rs]);
        }

    }

    $scope.doClearAllEndpoints = function() {

        $scope.mockSelection = [];
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

    $scope.filterS3Mocks = function() {

        $scope.s3Services = [];
        $scope.doClearAllEndpoints();

        if ($scope.searchFilter == null
                || $scope.searchFilter.trim() == 0) {

            $scope.s3Services = allS3Services;
            return;
        }

        for (var rs=0; rs < allS3Services.length; rs++) {
            if (allS3Services[rs].bucket.indexOf($scope.searchFilter) > -1) {
                $scope.s3Services.push(allS3Services[rs]);
            }
        }

    };

    $scope.startS3MockServer = function() {

        if ($scope.readOnly) {
            return;
        }

        utils.showLoadingOverlay('Starting S3 Server');

        restClient.doPost($http, '/mockedserver/s3/start', {}, function(status, data) {

            utils.hideLoadingOverlay();

            if (status == 200) {
                $scope.mockServerStatus = MockServerRunningStatus;

                var alertMsg = "S3 Server Started (on port " + String(data.port) + ")";

                showAlert(alertMsg, "success");
                loadTableData();
                return;
            }

            showAlert(globalVars.GeneralErrorMessage);
        });

    };

    $scope.stopS3MockServer = function () {

        if ($scope.readOnly) {
            return;
        }

        utils.showLoadingOverlay('Stopping S3 Server');

        restClient.doPost($http, '/mockedserver/s3/stop', {}, function(status, data) {

            utils.hideLoadingOverlay();

            if (status == 204) {
                $scope.mockServerStatus = MockServerStoppedStatus;
                showAlert("S3 Server Stopped", "success");
                loadTableData();
                return;
            }

            showAlert(globalVars.GeneralErrorMessage);
        });

    };

    $scope.doExport = function(mode) {

        if ($scope.mockSelection.length == 0) {
            showAlert("No S3 mocks have been selected for export");
            return;
        }

        var msgSuffix = ($scope.mockSelection.length == 1)
            ? "this S3 mock?"
            : ("these " + $scope.mockSelection.length + " S3 mocks?");

        utils.openWarningConfirmation("Are you sure you wish to export " + msgSuffix, function (alertResponse) {

            if (alertResponse) {

                var req = [];

                for (var m=0; m < $scope.mockSelection.length; m++) {
                    req.push($scope.mockSelection[m].extId);
                }

                restClient.doPost($http, '/mock/export/S3', req, function(status, data) {

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

    $scope.doDeleteSelection = function() {

        if ($scope.mockSelection.length == 0) {
            showAlert("No S3 mocks have been selected to delete");
            return;
        }

        var msgSuffix = ($scope.mockSelection.length == 1)
            ? "this S3 mock?"
            : ("these " + $scope.mockSelection.length + " S3 mocks?");

        utils.openDeleteConfirmation("Are you sure wish to delete " + msgSuffix, function (alertResponse) {

            if (alertResponse) {

                deletionAttemptCount = 0;
                utils.showBlockingOverlay();

                for (var m=0; m < $scope.mockSelection.length; m++) {
                    restClient.doDelete($http, '/s3mock/bucket/' + $scope.mockSelection[m].extId, bulkDeleteCallbackFunc);
                }

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
                    return { "serverType" : S3ServerType };
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

    $scope.translateSyncMode = function (syncMode) {

        if (syncMode == globalVars.SyncModeNone) {
            return syncModeNoneTableLabel;
        }
        if (syncMode == globalVars.SyncModeOneWay) {
            return syncModeOneWayTableLabel;
        }
        if (syncMode == globalVars.SyncModeBiDirectional) {
            return syncModeBiDirectionalTableLabel;
        }
    };


    //
    // Internal Functions
    function loadTableData() {

        allS3Services = [];
        $scope.s3Services = [];

        restClient.doGet($http, '/s3mock/bucket', function(status, data) {

            if (status == 401) {
                showAlert(globalVars.AuthRequiredMessage);
                return;
            } else if (status != 200) {
                showAlert(globalVars.GeneralErrorMessage);
                return;
            }

            allS3Services = data;
            $scope.s3Services = data;
        });

    }

    function loadS3ServerStatus() {

        utils.checkS3ServerStatus(function(running, port) {

            if (running == null) {
                showAlert(globalVars.GeneralErrorMessage);
                return;
            }

            if (RestartServerRequired
                    && running
                    && !$scope.readOnly) {

                $scope.mockServerStatus = MockServerRestartStatus;

                restartS3MockServer(function(port) {

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

    function restartS3MockServer(callback) {

        if ($scope.readOnly) {
            return;
        }

        utils.showLoadingOverlay('Updating S3 Server');

        restClient.doPost($http, '/mockedserver/s3/restart', {}, function(status, data) {

            if (status == 200) {
                callback(data.port);
                return;
            }

            callback();
        });

    }

    var bulkDeleteCallbackFunc = function (status, data) {

        if (status != 204) {
            deletionErrorOccurrence = true;
        }

        deletionAttemptCount++;

        if ($scope.mockSelection.length == deletionAttemptCount) {

            utils.hideBlockingOverlay();

            loadTableData();

            showAlert("The selected S3 mocks were successfully deleted", "success");
            $scope.mockSelection = [];

            if (deletionErrorOccurrence) {
                showAlert("An error occurred. Not all S3 mocks were deleted");
            }

            deletionAttemptCount = 0;
        }

    };


    //
    // Init page
    loadTableData();
    loadS3ServerStatus();

});
