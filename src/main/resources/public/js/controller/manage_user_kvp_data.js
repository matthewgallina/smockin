
app.controller('manageUserKvpDataController', function($scope, $window, $uibModal, $http, restClient, globalVars, auth) {


    //
    // Labels
    $scope.heading = 'Manage Key/Value Data';
    $scope.keyTableLabel = "Key";
    $scope.valueTableLabel = "Value";
    $scope.actionTableLabel = "Action";
    $scope.noDataFoundLabel = "No Data Found";


    //
    // Buttons
    $scope.addKvpDataButtonLabel = "Add New Key/Value Pair";
    $scope.viewKvpDataButtonLabel = "View";


    //
    // Data
    $scope.kvpData = [];


    //
    // Scoped Functions
    $scope.doViewKvpData = function(kvpData) {
        doOpenAddKvp(kvpData);
    };

    $scope.doOpenAddKvpData = function() {
        doOpenAddKvp();
    };


    //
    // Internal Functions
    function doOpenAddKvp(data) {

        var modalInstance = $uibModal.open({
            templateUrl: 'user_kvp_data.html',
            controller: 'userKvpDataController',
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
                loadTableKvpData();
            }
        }, function () {

        });

    }

    function loadTableKvpData() {

        $scope.kvpData = [];

        restClient.doGet($http, '/keyvaluedata', function(status, data) {

            if (status == 401) {
                redirectToHome();
                return;
            } else if (status != 200) {
                redirectToHome();
                return;
            }

            $scope.kvpData = data;
        });

    }

    function redirectToHome() {
        $window.location.href = "/index.html";
    }


    //
    // Init Page
    loadTableKvpData();

});
