
app.service('restClient', function(auth) {

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
    this.doGet = function (http, url, callback) {

        http({
          method: 'GET',
          url: url,
          headers: applyAuthHeader(),
          timeout: HttpRequestTimeoutMillis
        }).then(function successCallback(response) {
            callback(response.status, response.data);
        }, function errorCallback(response) {
            handleAuthResponse(response.status);
            callback(response.status, response.data);
        });

    };

    this.doPost = function (http, url, reqData, callback) {

        http({
          method: 'POST',
          url: url,
          data: reqData,
          headers: applyAuthHeader({
            'Content-Type': 'application/json'
           }),
           timeout: HttpRequestTimeoutMillis
        }).then(function successCallback(response) {
            callback(response.status, response.data);
        }, function errorCallback(response) {
            handleAuthResponse(response.status);
            callback(response.status, response.data);
        });

    };

    this.doPut = function (http, url, reqData, callback) {

        http({
          method: 'PUT',
          url: url,
          data: reqData,
          headers: applyAuthHeader({
            'Content-Type': 'application/json'
          }),
          timeout: HttpRequestTimeoutMillis
        }).then(function successCallback(response) {
            callback(response.status, response.data);
        }, function errorCallback(response) {
            handleAuthResponse(response.status);
            callback(response.status, response.data);
        });

    };

    this.doPatch = function (http, url, reqData, callback) {

        http({
          method: 'PATCH',
          url: url,
          data: reqData,
          headers: applyAuthHeader({
            'Content-Type': 'application/json',
          }),
          timeout: HttpRequestTimeoutMillis
        }).then(function successCallback(response) {
            callback(response.status, response.data);
        }, function errorCallback(response) {
            handleAuthResponse(response.status);
            callback(response.status, response.data);
        });

    };

    this.doDelete = function (http, url, callback) {

        http({
          method: 'DELETE',
          url: url,
          headers: applyAuthHeader(),
          timeout: HttpRequestTimeoutMillis
        }).then(function successCallback(response) {
            callback(response.status, response.data);
        }, function errorCallback(response) {
            handleAuthResponse(response.status);
            callback(response.status, response.data);
        });

    };

});
