
app.controller('s3DashboardController', function($scope, $window, $rootScope, $location, $timeout, $uibModal, $http, restClient, globalVars, utils, $routeParams, auth) {


    //
    // Constants
    var MockServerRunningStatus = globalVars.MockServerRunningStatus;
    var MockServerStoppedStatus = globalVars.MockServerStoppedStatus;
    var MockServerRestartStatus = globalVars.MockServerRestartStatus;
    var RestartServerRequired = $routeParams.restart;


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


    //
    // Table Labels
    $scope.bucketTableLabel = 'Bucket Name';
    $scope.dateCreatedTableLabel = 'Date Created';
    $scope.statusTableLabel = 'Deployment Status';
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


    //
    // Scoped Functions
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
                        $window.location.href = '/templates/main.html';
                        return;
                    }

                    showAlert(globalVars.GeneralErrorMessage);
                });

                return;
            }

            RestartServerRequired = false;
            $scope.mockServerStatus = (running) ? MockServerRunningStatus : MockServerStoppedStatus;
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


    //
    // Init page
    loadTableData();
    loadS3ServerStatus();

});
