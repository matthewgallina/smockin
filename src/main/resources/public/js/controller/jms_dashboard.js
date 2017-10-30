
app.controller('jmsDashboardController', function($scope, $http, utils, globalVars, restClient) {


    //
    // Constants
    var MockServerRunningStatus = globalVars.MockServerRunningStatus;
    var MockServerStoppedStatus = globalVars.MockServerStoppedStatus;
    var MockServerRestartStatus = globalVars.MockServerRestartStatus;


    //
    // Labels
    $scope.mockServerStatusLabel = 'JMS Mock Server Status:';
    $scope.serverConfigLabel = '(edit settings)';
    $scope.noDataFoundMsg = 'No Data Found';
    $scope.mockServerRunning = MockServerRunningStatus;
    $scope.mockServerStopped = MockServerStoppedStatus;
    $scope.mockServerRestarting = MockServerRestartStatus;
    $scope.endpointsHeading = 'Simulated JMS Queues';


    //
    // Alerts
    function showAlert(msg, type) {
        $scope.$parent.showAlert(msg, type);
    }


    //
    // Data
    $scope.mockServerStatus = null;


    //
    // Scoped Functions
    $scope.startJmsMockServer = function() {

        utils.showLoadingOverlay('Starting JMS Server');

        restClient.doPost($http, '/mockedserver/jms/start', {}, function(status, data) {

            utils.hideLoadingOverlay();

            if (status == 200) {
                $scope.mockServerStatus = MockServerRunningStatus;
                showAlert("JMS Server Started (on port " + String(data.port) + ")", "success");
                return;
            }

            showAlert(globalVars.GeneralErrorMessage);
        });

    }

    $scope.stopJmsMockServer = function () {

        utils.showLoadingOverlay('Stopping JMS Server');

        restClient.doPost($http, '/mockedserver/jms/stop', {}, function(status, data) {

            utils.hideLoadingOverlay();

            if (status == 204) {
                $scope.mockServerStatus = MockServerStoppedStatus;
                showAlert("JMS Server Stopped", "success");
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


    //
    // Init Page
    checkJmsServerStatus();

});
