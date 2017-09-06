
app.controller('helpSectionController', function($scope, $location, $uibModal) {

    //
    // Labels
    $scope.helpHeading = 'Help';
    $scope.contentsHeading = 'Table of Contents';

    $scope.inboundParamsInResponseTitle = 'Including inbound parameters and dynamic content in your response';
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

        addSlide(rulesDir + "/dashboard.png", "1. Click the 'Add Endpoint' button");
        addSlide(rulesDir + "/add_endpoint_default.png", "2. Fill in the default (403 unauthorized) response, then select 'Rules' and click 'Add Rule'");
        addSlide(rulesDir + "/add_rule.png", "3. Enter the response for this rule, then click 'Add Rule Condition'");
        addSlide(rulesDir + "/add_condition_1.png", "4. Add the first argument (i.e 'username' == 'admin'), then click 'Add Arg'");
        addSlide(rulesDir + "/add_condition_2.png", "5. Add the second argument (i.e 'password' == 'letmein'), then click 'Add Arg' and finally click 'Done'");
        addSlide(rulesDir + "/final_rule.png", "6. Review the rule and then click 'Add Rule'");
        addSlide(rulesDir + "/final_endpoint.png", "7. Review the endpoint and then click 'Save'");
        addSlide(rulesDir + "/start_mock_server.png", "8. Stop/Start the mock server to publish the new endpoint");
        addSlide(rulesDir + "/curl.png", "9. Finally call into the endpoint both with and without user details to see it in action");

    } else if (data.name == 'seq_response') {

        var seqDir = helpDir + "/seq_response";

        addSlide(seqDir + "/dashboard.png", "1. Click the 'Add Endpoint' button");
        addSlide(seqDir + "/add_endpoint.png", "2. Fill in the path and method, then select 'Sequenced' and click 'Add Seq Response'");
        addSlide(seqDir + "/add_seq.png", "3. Fill in the status code and body for this 1st response, then click 'Add'");
        addSlide(seqDir + "/final_endpoint.png", "4. Repeat twice more to give 3 responses in total. Once done review and click 'Save'");
        addSlide(seqDir + "/start_mock_server.png", "5. Stop/Start the mock server to publish the new endpoint");
        addSlide(seqDir + "/curl.png", "6. Finally make 3 calls into the endpoint to see the sequence in action");

    } else if (data.name == 'proxy_response') {

        var proxyDir = helpDir + "/proxy_response";

        addSlide(proxyDir + "/dashboard.png", "1. Click the 'Add Endpoint' button");
        addSlide(proxyDir + "/endpoint.png", "2. Fill in the path and method, then select 'Proxied', set a timeout value and finally click 'Save'");
        addSlide(proxyDir + "/start_mock_server.png", "3. Stop/Start the mock server to publish the new endpoint");
        addSlide(proxyDir + "/curl1.png", "4. Call the /wait endpoint. This will result in the terminal waiting on a response");
        addSlide(proxyDir + "/curl_timeout.png", "5. The call will internally timeout after 30 seconds resulting with a 404");
        addSlide(proxyDir + "/curl3.png", "6. Call the /wait endpoint again. This time post a response from a separate client (see below window)");
        addSlide(proxyDir + "/curl4.png", "7. The response posted is received by the waiting client (in top window)");

    } else if (data.name == 'dynamic_response_content') {

        var drcDir = helpDir + "/dynamic_response_content";

        addSlide(drcDir + "/dashboard.png", "1. Click the 'Add Endpoint' button");
        addSlide(drcDir + "/endpoint.png", "2. Fill in the path and method, then select 'Sequenced' and click 'Add Seq Response'");
        addSlide(drcDir + "/add_response.png", "3. Add a response with the 3 dynamic field references, then click 'Add'");
        addSlide(drcDir + "/final_endpoint.png", "4. Review the endpoint and then click 'Save'");
        addSlide(drcDir + "/start_mock_server.png", "5. Stop/Start the mock server to publish the new endpoint");
        addSlide(drcDir + "/curl1.png", "6. Finally call into endpoint providing all 3 fields");
        addSlide(drcDir + "/curl2.png", "7. The response containing all of the values");

    } else if (data.name == 'ws_response') {

        var wsDir = helpDir + "/ws_response";

        addSlide(wsDir + "/dashboard.png", ".1 Click the 'Add Endpoint' button");
        addSlide(wsDir + "/new_ws_endpoint.png", "2. Fill in the path, select 'WebSocket', set a timeout value and click 'Save'");
        addSlide(wsDir + "/start_mock_server.png", "3. Stop/Start the mock server to publish the new endpoint");
        addSlide(wsDir + "/open_ws_client.png", "4. Click 'Tools' and 'Open WS Client'");
        addSlide(wsDir + "/connect_ws_client.png", "5. Fill in the websocket path and click the 'Connect' button");
        addSlide(wsDir + "/curl.png", "6. From a terminal window, post a message to the websocket path via the /ws endpoint");
        addSlide(wsDir + "/client_response_1.png", "7. The message should appear in the client (note the message will also be received by any other connected clients)");
        addSlide(wsDir + "/client_ws_id.png", "8. To get the ID of the client connection, enter the message 'SMOCKIN_ID' and click 'Send'");
        addSlide(wsDir + "/client_response_2.png", "9. The remote server should return the ID back to the client");
        addSlide(wsDir + "/client_id_list.png", "10. A list of connected clients can also be found under the endpoint definition");
        addSlide(wsDir + "/curl2.png", "11. IDs can be used to send messages to a individual clients");

    }

    $scope.caption = slides[0].text

});