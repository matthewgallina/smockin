
app.controller('helpSectionController', function($scope, $location, $uibModal) {

    //
    // Labels
    $scope.helpHeading = 'Help';
    $scope.contentsHeading = 'Table of Contents';

    $scope.inboundParamsInResponseTitle = 'Including inbound parameters in your response';
    $scope.mockTypesSectionTitle = 'Return types explained';

    // Functions
    $scope.openImageViewer = function(url) {

        var modalInstance = $uibModal.open({
          templateUrl: 'help/help_image_viewer.html',
          controller: 'helpImageViewerController',
          resolve: {
            data: function () {
              return {
                "url" : url
              };
            }
          }
        });

    };

});

app.controller('helpImageViewerController', function($scope, $uibModalInstance, data) {

    //
    // Labels
    $scope.closeButtonLabel = 'Close';


    //
    // Data Objects
    $scope.imagePath = data.url;


    //
    // Functions
    $scope.doClose = function() {
        $uibModalInstance.close();
    };

});