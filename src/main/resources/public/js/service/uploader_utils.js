
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


app.service('uploadClient', function() {

    var HttpRequestTimeoutMillis = 30000;

    this.doPost = function (http, url, formData, callback) {

        http({
          method: 'POST',
          url: url,
          data: formData,
          headers: {
             'Content-Type': undefined
           },
           timeout: HttpRequestTimeoutMillis
        }).then(function successCallback(response) {
            callback(response.status, response.data);
        }, function errorCallback(response) {
            callback(response.status, response.data);
        });

    };

});
