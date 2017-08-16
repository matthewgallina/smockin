
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

    if (data.name == 'rule_response') {
        var rulesDir = helpDir + "/rule_response";

        addSlide(rulesDir + "/dashboard.png", "Click the 'Add Endpoint' button");
        addSlide(rulesDir + "/add_endpoint_default.png", "Fill in the default (403 unauthorized) response, then select 'Rules' and click 'Add Rule'");
        addSlide(rulesDir + "/add_rule.png", "Enter the response for this rule, then click 'Add Rule Condition'");
        addSlide(rulesDir + "/add_condition_1.png", "Add the first argument (i.e 'username' == 'admin'), then click 'Add Arg'");
        addSlide(rulesDir + "/add_condition_2.png", "Add the second argument (i.e'password' == 'letmein'), then click 'Add Arg' and finally click 'Done'");
        addSlide(rulesDir + "/final_rule.png", "Review the rule and then click 'Add Rule'");
        addSlide(rulesDir + "/final_endpoint.png", "Review the endpoint and then click 'Save'");
        addSlide(rulesDir + "/start_mock_server.png", "Stop/Start the mock server to publish the new endpoint");
        addSlide(rulesDir + "/curl.png", "Finally call into the endpoint both with and without user details to see it in action");

    } else if (data.name == 'seq_response') {

        var seqDir = helpDir + "/seq_response";

        addSlide(seqDir + "/dashboard.png", "Click the 'Add Endpoint' button");
        addSlide(seqDir + "/add_endpoint.png", "Fill in the path and method, then select 'Sequenced' and click 'Add Seq Response'");
        addSlide(seqDir + "/add_seq.png", "Fill in the status code and response for this 1st response, then click 'Add'");
        addSlide(seqDir + "/final_endpoint.png", "Repeat this twice more to give 3 responses in total. Once done review and click 'Save'");
        addSlide(seqDir + "/start_mock_server.png", "Stop/Start the mock server to publish the new endpoint");
        addSlide(seqDir + "/curl.png", "Finally make 3 calls into the endpoint to see the sequence in action");

    } else if (data.name == 'proxy_response') {

        var proxyDir = helpDir + "/proxy_response";

        addSlide(proxyDir + "/dashboard.png", "Click the 'Add Endpoint' button");
        addSlide(proxyDir + "/endpoint.png", "Fill in the path and method, then select 'Proxied', set a timeout value and finally click 'Save'");
        addSlide(proxyDir + "/start_mock_server.png", "Stop/Start the mock server to publish the new endpoint");
        addSlide(proxyDir + "/curl1.png", "Calling the /wait endpoint, will result in the caller having to wait for a response");
        addSlide(proxyDir + "/curl_timeout.png", "If nothing is sent to the server, then the call will internally timeout after 30 seconds resulting with a 404");
        addSlide(proxyDir + "/curl3.png", "Call the /wait endpoint again but this time post a response from a separate client (below window)");
        addSlide(proxyDir + "/curl4.png", "The response posted from the below client, is received by the caller at the top");

    } else if (data.name == 'dynamic_response_content') {

        var drcDir = helpDir + "/dynamic_response_content";

        addSlide(drcDir + "/dashboard.png", "Click the 'Add Endpoint' button");
        addSlide(drcDir + "/endpoint.png", "Fill in the path and method, then select 'Sequenced' and click 'Add Seq Response'");
        addSlide(drcDir + "/add_response.png", "Add a response with the 3 dynamic field references, then click 'Add'");
        addSlide(drcDir + "/final_endpoint.png", "Review the endpoint and then click 'Save'");
        addSlide(drcDir + "/start_mock_server.png", "Stop/Start the mock server to publish the new endpoint");
        addSlide(drcDir + "/curl1.png", "Finally call into endpoint providing all 3 fields");
        addSlide(drcDir + "/curl2.png", "The response containing all of the values");

    }

    $scope.caption = slides[0].text

});