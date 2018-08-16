
app.controller('navbarController', function($scope, $window, $location, $uibModal, auth, $http, restClient) {

    //
    // Labels
    $scope.navbarLabel = (auth.isLoggedIn()) ? auth.getFullName() : "Tools";
    $scope.helpLink = "Help"


    //
    // Buttons / Links
    $scope.httpClientLink = "Open HTTP Client";
    $scope.wsClientLink = "Open WS Client";
    $scope.myAccountLink = "Change Password";
    $scope.manageUsersLink = "Manage Users";
    $scope.logoutLink = "Logout";
    $scope.helpLink = "Help";


    //
    // Data Objects
    $scope.isLoggedIn = auth.isLoggedIn();
    $scope.isAdmin = auth.isAdmin();
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

        if (!auth.isLoggedIn()) {
            return;
        }

        $uibModal.open({
            templateUrl: 'update_password.html',
            controller: 'updatePasswordController',
            backdrop  : 'static',
            keyboard  : false
        });

    };

    $scope.doOpenManageUsers = function() {

        if (!auth.isLoggedIn() || !$scope.isAdmin) {
            return;
        }

        $location.path("/manage_users");

    };

    $scope.doLogout = function() {

        if (!auth.isLoggedIn()) {
            $window.location.reload();
            return;
        }

        var requestBody = {};

        restClient.doPost($http, '/logout', requestBody, function(status, data) {

            if (status != 204) {
                return;
            }

            auth.clearToken();
            $window.location.reload();
        });

    };

    $scope.doOpenHelp = function() {
        $window.open('http://help.smockin.com', '_blank');
    };

});
