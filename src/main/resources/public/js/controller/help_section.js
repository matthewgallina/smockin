
app.controller('helpSectionController', function($scope, $location, $uibModal) {

    //
    // Labels
    $scope.helpHeading = 'Help';
    $scope.contentsHeading = 'Table of Contents';

    $scope.inboundParamsInResponseTitle = 'Including inbound parameters in your response';
    $scope.mockTypesSectionTitle = 'Return types explained';


    //
    // Functions
    $scope.openImageViewer = function(name) {

        var modalInstance = $uibModal.open({
          templateUrl: 'help/help_slides.html',
          controller: 'helpSlidesController',
          resolve: {
            data: function () {
              return {
                "name" : name
              };
            }
          }
        });

    };

});

app.controller('helpSlidesController', function($scope, $uibModalInstance, data) {

    //
    // Labels
    $scope.closeButtonLabel = 'Close';

    //
    // Data Objects
    $scope.active = 0;
    $scope.slideInterval = 0;
    var slideIndex = 0;
    var slides = $scope.slides = [];


    //
    // Functions
    function addSlide(img, txt) {
        slides.push({
          id: slideIndex++,
          text: txt,
          image: img
        });
    }


    //
    // Functions
    $scope.doClose = function() {
        $uibModalInstance.close();
    };


    //
    // Listeners
    $scope.$watch('active', function(newIndex, oldIndex) {
        if (Number.isFinite(newIndex) && newIndex !== oldIndex) {
            $scope.caption = slides[newIndex].text;
        }
    });


    //
    // On page load
    var helpDir = "../../image/help";

    if (data.name == 'rules') {
        var rulesDir = helpDir + "/rule";

        addSlide(rulesDir + "/dashboard.png", "Click the 'Add Endpoint' button");
        addSlide(rulesDir + "/add_endpoint_default.png", "Fill in the default (403 unauthorized) response, Then select 'Rules' and click 'Add Rule'");
        addSlide(rulesDir + "/add_rule.png", "Enter the response for this rule, then click 'Add Rule Condition'");
        addSlide(rulesDir + "/add_condition_1.png", "Add the first argument (i.e 'username' == 'admin'), then click 'Add Arg'");
        addSlide(rulesDir + "/add_condition_2.png", "Add the second argument (i.e'password' == 'letmein'), then click 'Add Arg' and finally click 'Done'");
        addSlide(rulesDir + "/final_rule.png", "Review the rule and then click 'Add Rule'");
        addSlide(rulesDir + "/final_endpoint.png", "Review the endpoint and then click 'Save'");
        addSlide(rulesDir + "/start_mock_server.png", "Stop/Start the mock server to publish the new endpoint.");
        addSlide(rulesDir + "/curl.png", "Finally call into the endpoint to see it in action");
    }

    $scope.caption = slides[0].text

});