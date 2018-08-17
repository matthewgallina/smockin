
app.controller('userController', function($scope, $window, $uibModalInstance, $http, $timeout, utils, globalVars, restClient, data) {


    //
    // Labels
    $scope.userHeading = 'New User';
    $scope.usernameLabel = 'Username';
    $scope.fullNameLabel = 'Full Name';
    $scope.roleLabel = 'Role';
    $scope.passwordLabel = 'Password';
    $scope.confirmPasswordLabel = 'Confirm Password';
    $scope.dateCreatedLabel = 'Date Created';
    $scope.passwordResetTokenLabel = 'Password Reset Token';
    $scope.usernamePlaceholderTxt = "Enter a username";
    $scope.fullNamePlaceholderTxt = "Enter the full name";
    $scope.passwordPlaceholderTxt = "Enter a password";
    $scope.confirmPasswordPlaceholderTxt = "Confirm the above password ";


    //
    // Buttons
    $scope.cancelButtonLabel = 'Cancel';
    $scope.saveButtonLabel = 'Save';
    $scope.deleteButtonLabel = "Delete";
    $scope.passwordResetTokenButtonLabel = "Reset Password";


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
    $scope.userData = {
        "extId" : null,
        "username" : null,
        "fullName" : null,
        "role" : globalVars.RegularRole,
        "passwordResetToken" : null,
        "password" : null,
        "confirmPassword" : null
    };

    $scope.isSysAdmin = false;

    if (data != null) {

        $scope.userHeading = 'View User';

        $scope.userData.extId = data.extId;
        $scope.userData.username = data.username;
        $scope.userData.fullName = data.fullName;
        $scope.userData.role = data.role;
        $scope.userData.dateCreated = data.dateCreated;
        $scope.userData.passwordResetToken = (data.passwordResetToken != null) ? ($window.location.origin + globalVars.PasswordResetUrl + data.passwordResetToken) : null;
        $scope.userData.password = "********";
        $scope.userData.confirmPassword = "********";

        $scope.isSysAdmin = (data.role == globalVars.SysAdminRole);

    };


    //
    // Scoped Functions
    $scope.doSaveUser = function() {

        if (data != null) {
            doUpdateUser();
        } else {
            doCreateUser();
        }

    };

    $scope.doDeleteUser = function() {

        if (data == null) {
            return;
        }

        if (data.role == globalVars.SysAdminRole) {
            showAlert("The System Admin user cannot be deleted");
            return;
        }

        utils.openDeleteConfirmation("Are you sure you wish to delete this user?", function(result) {

            if (!result) {
                return;
            }

            restClient.doDelete($http, '/user/' + data.extId, function(status, data) {

                if (status == 204) {
                    $uibModalInstance.close("ok");
                    return;
                }

                showAlert(globalVars.GeneralErrorMessage);
            });

        });

    };

    $scope.doCreatePasswordResetToken = function() {

        utils.openWarningConfirmation("Are you sure you wish to reset this user's password?", function(result) {

            if (!result) {
                return;
            }

            restClient.doGet($http, '/user/' + data.extId + '/password/reset', function(status, data) {

                if (status == 200) {
                    utils.openAlert("Password Reset", "This user can now reset their password using the following link: " + $window.location.origin + globalVars.PasswordResetUrl + data.message, function() {
                        $uibModalInstance.close("ok");
                    });
                    return;
                }

                showAlert(globalVars.GeneralErrorMessage);
            });

        });

    };

    $scope.doCancel = function() {
        $uibModalInstance.dismiss('cancel');
    };


    //
    // Internal Functions
    function doCreateUser() {

        // Validation
        if (utils.isBlank($scope.userData.username)) {
            showAlert("'Username' is required");
            return;
        }

        if (utils.hasWhiteSpace($scope.userData.username)) {
            showAlert("'Username' must be a complete word without any whitespace");
            return;
        }

        if (utils.isBlank($scope.userData.fullName)) {
            showAlert("'Full Name' is required");
            return;
        }

        if (utils.isBlank($scope.userData.role)) {
            showAlert("'Role' is required");
            return;
        }

        if (utils.isBlank($scope.userData.password)) {
            showAlert("'Password' is required");
            return;
        }

        if ($scope.userData.password != $scope.userData.confirmPassword) {
            showAlert("'Password' and 'Confirm Password' do not match");
            return;
        }

        var reqBody = {
            "username" : $scope.userData.username,
            "fullName" : $scope.userData.fullName,
            "role" : globalVars.RegularRole,
            "password" : $scope.userData.password,
        };

        // Send
        restClient.doPost($http, '/user', reqBody, function(status, data) {

            if (status == 201) {
                $uibModalInstance.close("ok");
                return;
            } else if (status == 400) {
                $scope.userData.password = null;
                $scope.userData.confirmPassword = null;
                showAlert(data.message);
                return;
            }

            showAlert(globalVars.GeneralErrorMessage);
        });

    }

    function doUpdateUser() {

        // Validation
        if (utils.isBlank($scope.userData.username)) {
            showAlert("'Username' is required");
            return;
        }

        if (utils.hasWhiteSpace($scope.userData.username)) {
            showAlert("'Username' must be a complete word without any whitespace");
            return;
        }

        if (utils.isBlank($scope.userData.fullName)) {
            showAlert("'Full Name' is required");
            return;
        }

        if (utils.isBlank($scope.userData.role)) {
            showAlert("'Role' is required");
            return;
        }

        var reqBody = {
            "username" : $scope.userData.username,
            "fullName" : $scope.userData.fullName,
            "role" : $scope.userData.role
        };

        // Send
        restClient.doPut($http, '/user/' + data.extId, reqBody, function(status, data) {

            if (status == 204) {
                $uibModalInstance.close("ok");
                return;
            } else if (status == 400) {
                showAlert(data.message);
                return;
            }

            showAlert(globalVars.GeneralErrorMessage);
        });

    }

});
