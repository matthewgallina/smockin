
app.controller('authenticationController', function($scope, $http, $timeout, $uibModalInstance, restClient, globalVars, utils) {


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
    $scope.closeButtonLabel = "Cancel";


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
    $scope.doClose = function() {
        $uibModalInstance.dismiss();
    };

    $scope.doAuthenticateUser = function() {

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

            if (status == 401) {
                showAlert(globalVars.AuthErrorMessage);
            } else if (status != 200) {
                showAlert(globalVars.GeneralErrorMessage);
                return;
            }

            $uibModalInstance.close(data.message);
        });


    };

});
