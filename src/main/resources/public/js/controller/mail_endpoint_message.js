
app.controller('mailEndpointMessageController', function($scope, $uibModalInstance, data) {

    //
    // Labels
    $scope.heading = 'Mail Message';
    $scope.senderLabel = 'From';
    $scope.dateReceivedLabel = 'Date Received';
    $scope.subjectLabel = 'Subject';
    $scope.bodyLabel = 'Message Body';


    //
    // Buttons
    $scope.closeButtonLabel = 'Close';


    //
    // Data Objects
    $scope.mailMessage = data;


    //
    // Scoped Functions
    $scope.doClose = function() {
        $uibModalInstance.dismiss('cancel');
    };

});
