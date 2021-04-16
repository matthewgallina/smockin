
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

    if (data.nextButtonLabel == null) {
        $scope.continueButtonLabel = null;
        $scope.cancelButtonLabel = 'Close';
    }


    //
    // Scoped Functions
    $scope.doContinue = function() {
        $uibModalInstance.close();
    };

    $scope.doCancel = function() {
        $uibModalInstance.dismiss();
    };


    //
    // Internal Functions
    function initPage() {

        // Change width of second modal
        jQuery(function() {
            jQuery('.modal-dialog').first().addClass("confirmation-modal");
        });

    }


    //
    // Init Page
    initPage();

});
