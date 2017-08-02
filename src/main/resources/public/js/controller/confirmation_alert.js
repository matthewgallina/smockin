
app.controller('confirmationAlertController', function($scope, $location, $uibModalInstance, data) {


    //
    // Labels
    $scope.heading = data.heading;
    $scope.body = data.body;
    $scope.alertType = data.alertType;


    //
    // Buttons
    $scope.continueButtonLabel = data.nextButtonLabel;
    $scope.cancelButtonLabel = 'Cancel';


    //
    // Functions
    $scope.doContinue = function() {
        $uibModalInstance.close();
    };

    $scope.doCancel = function() {
        $uibModalInstance.dismiss();
    };

});
