
app.controller('dashboardController', function($scope, $window, $rootScope, $location, $uibModal, $http, $timeout, restClient, globalVars, utils, $routeParams) {


    //
    // Constants
    var MockServerRunningStatus = 'Running';
    var MockServerStoppedStatus = 'Stopped';
    var MockServerRestartStatus = 'Restarting';
    var RestfulServerType = globalVars.RestfulServerType;
    var AlertTimeoutMillis = globalVars.AlertTimeoutMillis;
    var RestartServerRequired = $routeParams.restart;

    var translations = [];
    translations.push({"k" : "SEQ", "v" : "Sequence Based"});
    translations.push({ "k" : "RULE", "v" : "Rules Based" });


    //
    // Labels
    $scope.mockServerStatusLabel = 'REST Mock Server Status:';
    $scope.serverConfigLabel = '(edit settings)';
    $scope.portLabel = 'port';
    $scope.noDataFoundMsg = 'No Data Found';
    $scope.mockServerRunning = MockServerRunningStatus;
    $scope.mockServerStopped = MockServerStoppedStatus;
    $scope.mockServerRestarting = MockServerRestartStatus;


    //
    // Buttons
    $scope.addEndpointLabel = 'Add Endpoint';
    $scope.viewEndpointLabel = 'View';
    $scope.endpointsHeading = 'Mocked RESTful Endpoints';


    //
    // Alerts
    $scope.alerts = [];

    var closeAlertFunc = function() {
        $scope.alerts = [];
    };

    function showAlert(msg, type) {

        if (type == null) {
            type = 'danger';
        }

        $scope.alerts = [];
        $scope.alerts.push({ "type" : type, "msg" : msg });

        $timeout(closeAlertFunc, AlertTimeoutMillis);
    }

    $scope.closeAlert = closeAlertFunc;


    //
    // Data Objects
    $scope.mockServerStatus = '';


    //
    // Endpoints Table
    $scope.pathTableLabel = 'Path';
    $scope.methodTableLabel = 'Method';
    $scope.dateCreatedTableLabel = 'Date Created';
    $scope.statusTableLabel = 'Status';
    $scope.mockTypeTableLabel = 'Mock Type';
    $scope.actionTableLabel = 'Action';


    //
    // Functions
    $scope.doOpenServerConfig = function() {

     var modalInstance = $uibModal.open({
          templateUrl: 'server_config.html',
          controller: 'serverConfigController',
          resolve: {
            data: function () {
              return { "serverType" : RestfulServerType };
            }
          }
        });

        modalInstance.result.then(function (response) {
            if (response != null && response.restartReq) {
                RestartServerRequired = true;
                loadServerStatus();
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

    $scope.doOpenEndpointInfo = function(endpointData) {
        $rootScope.endpointData = endpointData;
        $location.path("/endpoint");
    };

    function loadTableData() {

        restClient.doGet($http, '/restmock', function(status, data) {

            if (status == 200) {
                $scope.restServices = data;
                return;
            }

            showAlert("Oops looks like something went wrong!");
        });

    }

    function loadServerStatus() {

        utils.checkServerStatus(function(running) {

            if (running == null) {
                showAlert("Oops looks like something went wrong!");
                return;
            }

            if (RestartServerRequired && running) {

                $scope.mockServerStatus = MockServerRestartStatus;

                restartMockServer(function(port) {

                    if (port != null) {
                        $window.location.href = '/templates/main.html';
                        return;
                    }

                    showAlert("Oops looks like something went wrong!");
                });

                return;
            }

            RestartServerRequired = false;
            $scope.mockServerStatus = (running)?MockServerRunningStatus:MockServerStoppedStatus;
        });

    }

    $scope.startMockServer = function() {

        utils.showLoadingOverlay('Starting Server');

        restClient.doPost($http, '/mockedserver/rest/start', {}, function(status, data) {

            utils.hideLoadingOverlay();

            if (status == 200) {
                $scope.mockServerStatus = MockServerRunningStatus;
                showAlert("REST Server Started (on port " + String(data.port) + ")", "success");
                return;
            }

            showAlert("Oops looks like something went wrong!");
        });

    }

    $scope.stopMockServer = function () {

        utils.showLoadingOverlay('Stopping Server');

        restClient.doPost($http, '/mockedserver/rest/stop', {}, function(status, data) {

            utils.hideLoadingOverlay();

            if (status == 204) {
                $scope.mockServerStatus = MockServerStoppedStatus;
                showAlert("Server Stopped", "success");
                return;
            }

            showAlert("Oops looks like something went wrong!");
        });

    }

    function restartMockServer(callback) {

        utils.showLoadingOverlay('Updating Server');

        restClient.doPost($http, '/mockedserver/rest/restart', {}, function(status, data) {

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
    loadServerStatus();

});
