
app.controller('manageUsersController', function($scope, $uibModal, $http, restClient, globalVars) {

    //
    // Labels
    $scope.usersHeading = 'Manage Users';
    $scope.usernameTableLabel = "Username";
    $scope.fullNameTableLabel = "Full Name";
    $scope.roleTableLabel = "Role";
    $scope.dateCreatedTableLabel = "Date Created";
    $scope.actionTableLabel = "Action";


    //
    // Buttons
    $scope.addUserButtonLabel = "Add User";
    $scope.viewUserButtonLabel = "View";


    //
    // Data
    $scope.users = [];


    //
    // Scoped Functions
    $scope.doViewUser = function(userData) {
        doOpenUser(userData);
    };

    $scope.doOpenNewUser = function() {
        doOpenUser();
    };


    //
    // Internal Functions
    function doOpenUser(data) {

        var modalInstance = $uibModal.open({
            templateUrl: 'user.html',
            controller: 'userController',
            resolve: {
                data: function () {
                  return data;
                }
            }
        });

        modalInstance.result.then(function (response) {
            if (response != null) {
                loadTableData();
            }
        }, function () {

        });

    }

    function loadTableData() {

        $scope.users = [];

        restClient.doGet($http, '/user', function(status, data) {

            if (status == 401) {
                showAlert(globalVars.AuthRequiredMessage);
                return;
            } else if (status != 200) {
                showAlert(globalVars.GeneralErrorMessage);
                return;
            }

            $scope.users = data;
        });

    }


    //
    // Init Page
    loadTableData();

});
