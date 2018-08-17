
app.controller('manageUsersController', function($scope, $window, $uibModal, $http, restClient, globalVars, auth) {

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
            backdrop  : 'static',
            keyboard  : false,
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
                redirectToHome();
                return;
            } else if (status != 200) {
                redirectToHome();
                return;
            }

            $scope.users = data;
        });

    }

    function redirectToHome() {
        $window.location.href = "/index.html";
    }

    //
    // Init Page
    if (!auth.isLoggedIn() || !auth.isAdmin()) {
        redirectToHome();
        return;
    }

    loadTableData();

});
