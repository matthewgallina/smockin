var app = angular.module('smockinApp', ['ngRoute', 'ngAnimate', 'ngSanitize', 'ui.bootstrap']);

app.controller('mainController', function($scope, $location, $http, restClient, globalVars, auth) {

    //
    // Internal Functions
    function checkUserModeAndAccess() {

        restClient.doGet($http, '/user/mode', function(status, data) {

            if (status != 200) {
                return;
            }

            if (data.message == globalVars.ActiveStatus) {

                if (auth.isLoggedIn()) {
                    return;
                }

                auth.doRequestAuth(function() {
                    $location.path("/dashboard");
                });

            } else {
                auth.clearToken();
            }

        });

    }


    //
    // Init
    checkUserModeAndAccess();

});
