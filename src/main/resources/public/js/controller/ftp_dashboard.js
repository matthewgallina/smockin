
app.controller('ftpDashboardController', function($scope, $rootScope, $routeParams, $location, $http, $uibModal, utils, globalVars, restClient, auth) {


    //
    // Constants
    var MockServerRunningStatus = globalVars.MockServerRunningStatus;
    var MockServerStoppedStatus = globalVars.MockServerStoppedStatus;
    var MockServerRestartStatus = globalVars.MockServerRestartStatus;
    var FtpServerType = globalVars.FtpServerType;
    var RestartServerRequired = $routeParams.restart;


    //
    // Labels
    $scope.mockServerStatusLabel = 'FTP Mock Server Status:';
    $scope.serverConfigLabel = '(edit settings)';
    $scope.noDataFoundMsg = 'No Data Found';
    $scope.mockServerRunning = MockServerRunningStatus;
    $scope.mockServerStopped = MockServerStoppedStatus;
    $scope.mockServerRestarting = MockServerRestartStatus;
    $scope.endpointsHeading = 'FTP Repos';
    $scope.searchFilterPlaceHolderTxt = 'Quick Search...';


    //
    // Table Labels
    $scope.nameTableLabel = 'Username';
    $scope.dateCreatedTableLabel = 'Date Created';
    $scope.statusTableLabel = 'Deployment Status';
    $scope.actionTableLabel = 'Action';


    //
    // Buttons
    $scope.addEndpointButtonLabel = 'New FTP Repository';
    $scope.viewEndpointButtonLabel = 'View';


    //
    // Alerts
    function showAlert(msg, type) {
        $scope.$parent.showAlert(msg, type);
    }


    //
    // Data
    $scope.readOnly = (auth.isLoggedIn() && !auth.isAdmin());
    var allFtpServices = [];
    $scope.ftpServices = [];
    $scope.mockServerStatus = null;
    $scope.searchFilter = null;


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
              return { "serverType" : FtpServerType };
            }
          }
        });

        modalInstance.result.then(function (response) {
            if (response != null
                    && response.restartReq
                    && !$scope.readOnly) {
                RestartServerRequired = true;

            }
        }, function () {

        });

    };

    $scope.doOpenFtpInfo = function(ftpData) {
        $rootScope.ftpEndpointData = ftpData;
        $location.path("/ftp_endpoint");
    };


    $scope.startFtpMockServer = function() {

        if ($scope.readOnly) {
            return;
        }

        utils.showLoadingOverlay('Starting FTP Server');

        restClient.doPost($http, '/mockedserver/ftp/start', {}, function(status, data) {

            utils.hideLoadingOverlay();

            if (status == 200) {
                $scope.mockServerStatus = MockServerRunningStatus;
                showAlert("FTP Server Started (on port " + String(data.port) + ")", "success");
                loadTableData();
                return;
            }

            showAlert(globalVars.GeneralErrorMessage);
        });

    }

    $scope.stopFtpMockServer = function () {

        if ($scope.readOnly) {
            return;
        }

        utils.showLoadingOverlay('Stopping FTP Server');

        restClient.doPost($http, '/mockedserver/ftp/stop', {}, function(status, data) {

            utils.hideLoadingOverlay();

            if (status == 204) {
                $scope.mockServerStatus = MockServerStoppedStatus;
                showAlert("FTP Server Stopped", "success");
                loadTableData();
                return;
            }

            showAlert(globalVars.GeneralErrorMessage);
        });

    }

    $scope.filterFtpMocks = function() {

        $scope.ftpServices = [];

        if ($scope.searchFilter == null
                || $scope.searchFilter.trim() == 0) {

            $scope.ftpServices = allFtpServices;
            return;
        }

        for (var rs=0; rs < allFtpServices.length; rs++) {
            if (allFtpServices[rs].name.indexOf($scope.searchFilter) > -1) {
                $scope.ftpServices.push(allFtpServices[rs]);
            }
        }

    };


    //
    // Internal Functions
    function checkFtpServerStatus() {

        restClient.doGet($http, '/mockedserver/ftp/status', function(status, data) {

            if (status != 200) {
                showAlert(globalVars.GeneralErrorMessage);
                return;
            }

            $scope.mockServerStatus = (data.running)?MockServerRunningStatus:MockServerStoppedStatus;
        });

    };

    /*
    function restartFtpMockServer(callback) {

        utils.showLoadingOverlay('Updating FTP Server');

        restClient.doPost($http, '/mockedserver/ftp/restart', {}, function(status, data) {

            if (status == 200) {
                callback(data.port);
                return;
            }

            callback();
        });

    }
    */

    function loadTableData() {

        allFtpServices = [];
        $scope.ftpServices = [];

        restClient.doGet($http, '/ftpmock', function(status, data) {

            if (status == 401) {
                showAlert(globalVars.AuthRequiredMessage);
                return;
            } else if (status != 200) {
                showAlert(globalVars.GeneralErrorMessage);
                return;
            }

            allFtpServices = data;
            $scope.ftpServices = data;
        });

    }


    //
    // Init Page
    loadTableData();
    checkFtpServerStatus();

});
