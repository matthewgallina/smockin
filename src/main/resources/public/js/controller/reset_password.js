var app = angular.module('smockinResetPasswordApp', ['ngSanitize', 'ngAnimate', 'ui.bootstrap']);

app.controller('resetPasswordController', function($scope, $window, $location, $uibModal, $timeout, $http, restClient, globalVars, auth, utils) {

    //
    // Labels
    $scope.passwordResetHeading = "Password Reset";
    $scope.passwordResetInfo = "Please enter your new password below";
    $scope.newPasswordLabel = 'New Password';
    $scope.confirmPasswordLabel = 'Confirm Password';

    $scope.newPasswordPlaceholderTxt = "Enter a new password";
    $scope.confirmPasswordPlaceholderTxt = "Confirm the new password";


    //
    // Buttons
    $scope.updateButtonLabel = 'Update Password';


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

        $timeout(closeAlertFunc, globalVars.AlertTimeoutMillis);
    }

    $scope.closeAlert = closeAlertFunc;


    //
    // Data Objects
    var token = extractResetToken();

    $scope.disableButton = false;

    $scope.passwordData = {
        "newPassword" : null,
        "confirmPassword" : null
    };


    //
    // Scoped Functions
    $scope.doUpdatePassword = function() {

        closeAlertFunc();

        // Validation
        if (utils.isBlank($scope.passwordData.newPassword)) {
            showAlert("'New Password' is required");
            return;
        }

        if (utils.isBlank($scope.passwordData.confirmPassword)) {
            showAlert("'Confirm Password' is required");
            return;
        }

        if ($scope.passwordData.newPassword != $scope.passwordData.confirmPassword) {
            showAlert("'Password' and 'Confirm Password' do not match");
            clearPasswordFields();
            return;
        }

        $scope.disableButton = true;

        var reqBody = {
            "newPassword" : $scope.passwordData.newPassword
        };

        // Send
        restClient.doPost($http, '/password/reset/token/' + token, reqBody, function(status, data) {

            if (status == 204) {
                openAlert();
                return;
            } else if (status == 400) {
                showAlert(data.message);
                $scope.disableButton = false;
                clearPasswordFields();
                return;
            }

            clearPasswordFields();
            showAlert(globalVars.GeneralErrorMessage);
            $scope.disableButton = false;
        });

    };


    //
    // Internal Functions
    function extractResetToken() {

        var searchParams = $window.location.search;

        if (searchParams == null
                || searchParams.length == 0
                || searchParams.indexOf("?rt=") == -1) {
            return null;
        }

        var paramStart = searchParams.indexOf("?rt=") + 4;
        var paramEnd = searchParams.indexOf("&");

        return (paramEnd > -1) ? searchParams.substring(paramStart, paramEnd) : searchParams.substring(paramStart);
    }

    function validateToken() {

        restClient.doGet($http, '/password/reset/token/' + token, function(status, data) {

            if (status != 204) {
                redirectToHome();
            }

        });

    }

    function checkUserMode() {

        restClient.doGet($http, '/user/mode', function(status, data) {

            if (status != 200) {
                redirectToHome();
                return;
            }

            if (data.message != globalVars.ActiveStatus) {
                redirectToHome();
                return;
            }

            validateToken();

        });

    }

    function redirectToHome() {
        $window.location.href = "/index.html";
    }

    function clearPasswordFields() {
        $scope.passwordData = {
            "newPassword" : null,
            "confirmPassword" : null
        };
    }

    function openAlert() {

        var modalInstance = $uibModal.open({
            templateUrl: 'templates/confirmation_alert.html',
            controller: 'confirmationAlertController',
            backdrop  : 'static',
            keyboard  : false,
            resolve: {
                data: function () {
                    return {
                        "heading" : "Password Reset",
                        "body" : "Your password has been successfully reset",
                        "alertType" : "success"
                    };
                }
            }
        });

        modalInstance.result.then(function () {
        }, function () {
            redirectToHome();
        });

    }


    //
    // Init Page
    if (token == null) {
        redirectToHome();
    }

    checkUserMode();

    auth.clearToken();

});
