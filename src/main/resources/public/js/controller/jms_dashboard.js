
app.controller('jmsDashboardController', function($scope, $rootScope, $routeParams, $location, $http, $uibModal, utils, globalVars, restClient, auth) {


    //
    // Constants
    var MockServerRunningStatus = globalVars.MockServerRunningStatus;
    var MockServerStoppedStatus = globalVars.MockServerStoppedStatus;
    var MockServerRestartStatus = globalVars.MockServerRestartStatus;
    var JmsServerType = globalVars.JmsServerType;
    var RestartServerRequired = $routeParams.restart;


    //
    // Labels
    $scope.mockServerStatusLabel = 'JMS Mock Server Status:';
    $scope.serverConfigLabel = '(edit settings)';
    $scope.noDataFoundMsg = 'No Data Found';
    $scope.mockServerRunning = MockServerRunningStatus;
    $scope.mockServerStopped = MockServerStoppedStatus;
    $scope.mockServerRestarting = MockServerRestartStatus;
    $scope.endpointsHeading = 'JMS Mocks';

    $scope.nameTableLabel = 'Name';
    $scope.dateCreatedTableLabel = 'Date Created';
    $scope.mockTypeTableLabel = 'JMS Mock Type';
    $scope.statusTableLabel = 'Deployment Status';
    $scope.actionTableLabel = 'Action';


    //
    // Buttons
    $scope.addEndpointButtonLabel = 'New JMS Endpoint';
    $scope.viewEndpointButtonLabel = 'View';


    //
    // Alerts
    function showAlert(msg, type) {
        $scope.$parent.showAlert(msg, type);
    }


    //
    // Data
    $scope.readOnly = (auth.isLoggedIn() && !auth.isAdmin());
    $scope.jmsServices = [];
    $scope.mockServerStatus = null;


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
              return { "serverType" : JmsServerType };
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

    $scope.doOpenJmsEndpointInfo = function(jmsEndpointData) {
        $rootScope.jmsEndpointData = jmsEndpointData;
        $location.path("/jms_endpoint");
    };

    $scope.startJmsMockServer = function() {

        if ($scope.readOnly) {
            return;
        }

        utils.showLoadingOverlay('Starting JMS Server');

        restClient.doPost($http, '/mockedserver/jms/start', {}, function(status, data) {

            utils.hideLoadingOverlay();

            if (status == 200) {
                $scope.mockServerStatus = MockServerRunningStatus;
                showAlert("JMS Server Started (on port " + String(data.port) + ")", "success");
                loadTableData();
                return;
            }

            showAlert(globalVars.GeneralErrorMessage);
        });

    }

    $scope.stopJmsMockServer = function () {

        if ($scope.readOnly) {
            return;
        }

        utils.showLoadingOverlay('Stopping JMS Server');

        restClient.doPost($http, '/mockedserver/jms/stop', {}, function(status, data) {

            utils.hideLoadingOverlay();

            if (status == 204) {
                $scope.mockServerStatus = MockServerStoppedStatus;
                showAlert("JMS Server Stopped", "success");
                loadTableData();
                return;
            }

            showAlert(globalVars.GeneralErrorMessage);
        });

    }


    //
    // Internal Functions
    function checkJmsServerStatus() {

        restClient.doGet($http, '/mockedserver/jms/status', function(status, data) {

            if (status != 200) {
                showAlert(globalVars.GeneralErrorMessage);
                return;
            }

            $scope.mockServerStatus = (data.running)?MockServerRunningStatus:MockServerStoppedStatus;
        });

    };

    /*
    function restartJmsMockServer(callback) {

        utils.showLoadingOverlay('Updating JMS Server');

        restClient.doPost($http, '/mockedserver/jms/restart', {}, function(status, data) {

            if (status == 200) {
                callback(data.port);
                return;
            }

            callback();
        });

    }
    */

    function loadTableData() {

        $scope.jmsServices = [];

        restClient.doGet($http, '/jmsmock', function(status, data) {

            if (status == 401) {
                showAlert(globalVars.AuthRequiredMessage);
                return;
            } else if (status != 200) {
                showAlert(globalVars.GeneralErrorMessage);
                return;
            }

            $scope.jmsServices = data;
        });

    }


    //
    // Init Page
    loadTableData();
    checkJmsServerStatus();

});
