
app.controller('navbarController', function($scope, $window, $uibModal) {


    //
    // Labels
    $scope.toolsLabel = "Tools";
    $scope.helpLink = "Help"


    //
    // Buttons / Links
    $scope.httpClientLink = "Open HTTP Client";
    $scope.wsClientLink = "Open WS Client";
    $scope.myAccountLink = "My Account";
    $scope.logoutLink = "Logout";
    $scope.helpLink = "Help";


    //
    // Data Objects
    var httpClientState = null;
    var wsClientState = null;


    //
    // Scoped Functions
    $scope.doOpenHttpClient = function() {

        var modalInstance = $uibModal.open({
            templateUrl: 'http_client.html',
            controller: 'httpClientController',
            resolve: {
                data: function () {
                    return {
                        "state" : httpClientState
                    };
                }
            },
            backdrop  : 'static',
            keyboard  : false
        });

        modalInstance.result.then(function (state) {
            httpClientState = state;
        }, function () {
        });

    };

    $scope.doOpenWebSocketClient = function() {

        var modalInstance = $uibModal.open({
            templateUrl: 'ws_client.html',
            controller: 'wsClientController',
            resolve: {
                data: function () {
                    return {
                        "state" : wsClientState
                    };
                }
            },
            backdrop  : 'static',
            keyboard  : false
        });

        modalInstance.result.then(function (state) {
            wsClientState = state;
        }, function () {
        });

    };

    $scope.doOpenMyAccount = function() {

        $uibModal.open({
            templateUrl: 'my_account.html',
            controller: 'myAccountController',
            resolve: {
                data: function () {
                    return null;
                }
            },
            backdrop  : 'static',
            keyboard  : false
        });

    };

    $scope.doLogout = function() {

        // TODO

    };

    $scope.doOpenHelp = function() {
        $window.open('http://help.smockin.com', '_blank');
    };

});
