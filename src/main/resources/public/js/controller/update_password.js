
app.controller('updatePasswordController', function($scope, $uibModalInstance, $http, $timeout, utils, globalVars, restClient, auth) {


    //
    // Labels
    $scope.resetPasswordHeading = 'Change Password';
    $scope.usernameLabel = 'Username';
    $scope.fullnameLabel = 'Full Name';
    $scope.currentPasswordLabel = 'Current Password';
    $scope.newPasswordLabel = 'New Password';
    $scope.confirmPasswordLabel = 'Confirm Password';

    $scope.currentPasswordPlaceholderTxt = "Enter your current password";
    $scope.newPasswordPlaceholderTxt = "Enter a new password";
    $scope.confirmPasswordPlaceholderTxt = "Confirm the new password";


    //
    // Buttons
    $scope.cancelButtonLabel = 'Cancel';
    $scope.updateButtonLabel = 'Update';


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
    $scope.passwordData = {
        "currentPassword" : null,
        "newPassword" : null,
        "confirmPassword" : null
    };

    $scope.disableButton = false;
    $scope.fullname = auth.getFullName();
    $scope.username = auth.getUserName();


    //
    // Scoped Functions
    $scope.doUpdatePassword = function() {

        closeAlertFunc();

        // Validation
        if (utils.isBlank($scope.passwordData.currentPassword)) {
            showAlert("'Current Password' is required");
            return;
        }

        if (utils.isBlank($scope.passwordData.newPassword)) {
            showAlert("'New Password' is required");
            return;
        }

        if ($scope.passwordData.newPassword != $scope.passwordData.confirmPassword) {
            showAlert("'Password' and 'Confirm Password' do not match");
            clearPasswordFields();
            return;
        }

        $scope.disableButton = true;

        var reqBody = {
            "currentPassword" : $scope.passwordData.currentPassword,
            "newPassword" : $scope.passwordData.newPassword
        };

        // Send
        restClient.doPatch($http, '/user/password', reqBody, function(status, data) {

            if (status == 204) {
                utils.openAlert("Password Update Status", "Your password has been successfully updated", function() {
                    $uibModalInstance.close();
                });
                return;
            } else if (status == 400) {
                showAlert(data.message);
                $scope.disableButton = false;
                clearPasswordFields();
                return;
            }

            $scope.disableButton = false;
            clearPasswordFields();
            showAlert(globalVars.GeneralErrorMessage);
        });

    };

    $scope.doCancel = function() {
        $uibModalInstance.dismiss('cancel');
    };

    //
    // Internal Functions
    function clearPasswordFields() {
        $scope.passwordData = {
            "currentPassword" : null,
            "newPassword" : null,
            "confirmPassword" : null
        };
    }

});
