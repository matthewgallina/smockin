
app.directive('formFileModel', function ($parse) {
    return {
        restrict: 'A', //the directive can be used as an attribute only

        link: function (scope, element, attrs) {
            var model = $parse(attrs.formFileModel),
                modelSetter = model.assign; // Define a setter for formFileModel

            // Bind change event on the element
            element.bind('change', function () {
                // Call apply on scope, it checks for value changes and reflect them on UI
                scope.$apply(function () {
                    //set the model value
                    modelSetter(scope, element[0].files[0]);
                });
            });
        }
    };
});


app.service('uploadClient', function(auth) {

    var HttpRequestTimeoutMillis = 30000;

    //
    // Private functions
    function applyAuthHeader(reqHeaders) {

        if (reqHeaders == null) {
            reqHeaders = {};
        }

        var token = auth.getToken();

        if (token != null) {
            reqHeaders.Authorization = 'Bearer ' + token;
        }

        return reqHeaders;
    }

    function handleAuthResponse(status) {
        if (status == 401) {
            auth.clearToken();
        }
    }


    //
    // Service Level Functions
    this.doPost = function (http, url, formData, callback) {

        http({
          method: 'POST',
          url: url,
          data: formData,
          headers: applyAuthHeader({
             'Content-Type': undefined
           }),
           timeout: HttpRequestTimeoutMillis
        }).then(function successCallback(response) {
            callback(response.status, response.data);
        }, function errorCallback(response) {
            callback(response.status, response.data);
        });

    };

});
