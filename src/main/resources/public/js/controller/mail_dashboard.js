
app.controller('mailDashboardController', function($scope, $window, $rootScope, $location, $timeout, $uibModal, $http, restClient, globalVars, utils, $routeParams, auth) {


    //
    // Constants
    var MockServerRunningStatus = globalVars.MockServerRunningStatus;
    var MockServerStoppedStatus = globalVars.MockServerStoppedStatus;
    var MockServerRestartStatus = globalVars.MockServerRestartStatus;
    var RestartServerRequired = $routeParams.restart;
    var MailServerType = globalVars.MailServerType;


    //
    // Labels
    $scope.mockServerStatusLabel = 'Mail Mock Server Status:';
    $scope.serverConfigLabel = 'server config';
    $scope.noDataFoundMsg = 'No Data Found';
    $scope.selectAllEndpointsHeading = 'select all';
    $scope.deselectAllEndpointsHeading = 'clear selection';
    $scope.enabledLabel = "Enabled";
    $scope.disabledLabel = "Disabled";
    $scope.searchFilterPlaceHolderTxt = 'Quick Search...';


    //
    // Table Labels
    $scope.inboxTableLabel = 'Email Address';
    $scope.dateCreatedTableLabel = 'Date Created';
    $scope.statusTableLabel = 'Deployment Status';
    $scope.messageCountTableLabel = 'Messages Received';
    $scope.actionTableLabel = 'Action';


    //
    // Buttons
    $scope.addInboxButtonLabel = 'New Inbox';
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
    var allMailServices = [];
    $scope.isLoggedIn = auth.isLoggedIn();
    $scope.readOnly = (auth.isLoggedIn() && !auth.isAdmin());
    $scope.mockServerStatus = '';
    $scope.mailServices = [];
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
              return { "serverType" : MailServerType };
            }
          }
        });

        modalInstance.result.then(function (response) {
            if (response != null
                    && response.restartReq
                    && !$scope.readOnly) {
                RestartServerRequired = true;
                loadMailServerStatus();
            }
        }, function () {

        });

    };

    $scope.doOpenMailInboxEndpointInfo = function(endpointDataExtId) {
        $location.path("/mail_endpoint").search({
            "dv" : globalVars.MailServerMode,
            "eid" : endpointDataExtId
        });
    };

    $scope.doSelectAllEndpoints = function() {

       $scope.mockSelection = [];

        for (var rs=0; rs < $scope.mailServices.length; rs++) {
            $scope.mockSelection.push($scope.mailServices[rs]);
        }

    }

    $scope.doClearAllEndpoints = function() {

        $scope.mockSelection = [];
    };

    $scope.doesSelectionContain = function(extId) {

        for (var m=0; m < $scope.mockSelection.length; m++) {
            if ($scope.mockSelection[m].externalId == extId) {
                return true;
            }
        }

        return false;
    };

    $scope.toggleSelection = function (mock) {

        for (var m=0; m < $scope.mockSelection.length; m++) {
            if ($scope.mockSelection[m].externalId == mock.extId) {
                $scope.mockSelection.splice(m, 1);
                return;
            }
        }

        $scope.mockSelection.push(mock);
    };

    $scope.filterMailMocks = function() {

        $scope.mailServices = [];
        $scope.doClearAllEndpoints();

        if ($scope.searchFilter == null
                || $scope.searchFilter.trim() == 0) {

            $scope.mailServices = allMailServices;
            return;
        }

        for (var rs=0; rs < allMailServices.length; rs++) {
            if (allMailServices[rs].address.indexOf($scope.searchFilter) > -1) {
                $scope.mailServices.push(allMailServices[rs]);
            }
        }

    };

    $scope.startMailMockServer = function() {

        if ($scope.readOnly) {
            return;
        }

        utils.showLoadingOverlay('Starting Mail Server');

        restClient.doPost($http, '/mockedserver/mail/start', {}, function(status, data) {

            utils.hideLoadingOverlay();

            if (status == 200) {
                $scope.mockServerStatus = MockServerRunningStatus;

                var alertMsg = "Mail Server Started (on port " + String(data.port) + ")";

                showAlert(alertMsg, "success");
                loadTableData();
                return;
            }

            showAlert(globalVars.GeneralErrorMessage);
        });

    };

    $scope.stopMailMockServer = function () {

        if ($scope.readOnly) {
            return;
        }

        utils.showLoadingOverlay('Stopping Mail Server');

        restClient.doPost($http, '/mockedserver/mail/stop', {}, function(status, data) {

            utils.hideLoadingOverlay();

            if (status == 204) {
                $scope.mockServerStatus = MockServerStoppedStatus;
                showAlert("Mail Server Stopped", "success");
                loadTableData();
                return;
            }

            showAlert(globalVars.GeneralErrorMessage);
        });

    };

    $scope.doExport = function(mode) {

        if ($scope.mockSelection.length == 0) {
            showAlert("No mail mocks have been selected for export");
            return;
        }

        var msgSuffix = ($scope.mockSelection.length == 1)
            ? "this mail mock?"
            : ("these " + $scope.mockSelection.length + " mail mocks?");

        utils.openWarningConfirmation("Are you sure you wish to export " + msgSuffix, function (alertResponse) {

            if (alertResponse) {

                var req = [];

                for (var m=0; m < $scope.mockSelection.length; m++) {
                    req.push($scope.mockSelection[m].externalId);
                }

                restClient.doPost($http, '/mock/export/mail', req, function(status, data) {

                    if (status != 200) {
                        showAlert(globalVars.GeneralErrorMessage);
                        return;
                    }

                    utils.handleExportDownload(data, "smockin_mail_export_" + $scope.mockSelection.length + "_mocks.zip", "application/zip");
                    $scope.mockSelection = [];

                });

            }

        });

    };

    $scope.doDeleteSelection = function() {

        if ($scope.mockSelection.length == 0) {
            showAlert("No mail mocks have been selected to delete");
            return;
        }

        var msgSuffix = ($scope.mockSelection.length == 1)
            ? "this mail mock?"
            : ("these " + $scope.mockSelection.length + " mail mocks?");

        utils.openDeleteConfirmation("Are you sure wish to delete " + msgSuffix, function (alertResponse) {

            if (alertResponse) {

                deletionAttemptCount = 0;
                utils.showBlockingOverlay();

                for (var m=0; m < $scope.mockSelection.length; m++) {
                    restClient.doDelete($http, '/mailmock/' + $scope.mockSelection[m].externalId, bulkDeleteCallbackFunc);
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
                    return { "serverType" : MailServerType };
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


    //
    // Internal Functions
    function loadTableData() {

        allMailServices = [];
        $scope.mailServices = [];

        restClient.doGet($http, '/mailmock', function(status, data) {

            if (status == 401) {
                showAlert(globalVars.AuthRequiredMessage);
                return;
            } else if (status != 200) {
                showAlert(globalVars.GeneralErrorMessage);
                return;
            }

            allMailServices = data;
            $scope.mailServices = data;
        });

    }

    function loadMailServerStatus() {

        utils.checkMailServerStatus(function(running, port) {

            if (running == null) {
                showAlert(globalVars.GeneralErrorMessage);
                return;
            }

            if (RestartServerRequired
                    && running
                    && !$scope.readOnly) {

                $scope.mockServerStatus = MockServerRestartStatus;

                restartMailMockServer(function(port) {

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

    function restartMailMockServer(callback) {

        if ($scope.readOnly) {
            return;
        }

        utils.showLoadingOverlay('Updating Mail Server');

        restClient.doPost($http, '/mockedserver/mail/restart', {}, function(status, data) {

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

            showAlert("The selected mail mocks were successfully deleted", "success");
            $scope.mockSelection = [];

            if (deletionErrorOccurrence) {
                showAlert("An error occurred. Not all mail mocks were deleted");
            }

            deletionAttemptCount = 0;
        }

    };


    //
    // Init page
    loadTableData();
    loadMailServerStatus();

});
