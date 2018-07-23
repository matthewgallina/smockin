
app.controller('authenticationController', function($scope, $window, $http, $timeout, $uibModalInstance, restClient, globalVars, utils, auth) {


    //
    // Constants / Vars
    var AlertTimeoutMillis = globalVars.AlertTimeoutMillis;


    //
    // Labels
    $scope.authHeading = "Login";
    $scope.usernameLabel = "Username";
    $scope.passwordLabel = "Password";
    $scope.usernamePlaceholderTxt = "Enter your username";
    $scope.passwordPlaceholderTxt = "Enter your password";


    //
    // Buttons
    $scope.loginButtonLabel = "Login";


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
    // Data
    $scope.auth = {
        username: null,
        password: null
    };


    //
    // Scoped Functions
    $scope.doAuthenticateUser = function() {

        // clear current token
        auth.clearToken();

        // Validation
        if (utils.isBlank($scope.auth.username)) {
            showAlert("Please enter your username");
            return;
        }

        if (utils.isBlank($scope.auth.password)) {
            showAlert("Please enter your password");
            return;
        }

        // Call Server
        var authReqData = {
            "username" : $scope.auth.username,
            "password" : $scope.auth.password
        };

        restClient.doPost($http, '/auth', authReqData, function(status, data) {

            $scope.auth.password = "";

            if (status == 401) {
                showAlert(globalVars.AuthErrorMessage);
                return;
            } else if (status != 200) {
                showAlert(globalVars.GeneralErrorMessage);
                return;
            }

            auth.saveToken(data.message);

            $window.location.reload();
        });


    };

});
