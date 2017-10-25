
app.service('restClient', function() {

    this.doGet = function (http, url, callback) {

        http({
          method: 'GET',
          url: url,
          timeout: 8000
        }).then(function successCallback(response) {
            callback(response.status, response.data);
        }, function errorCallback(response) {
            callback(response.status, response.data);
        });

    };

    this.doPost = function (http, url, reqData, callback) {

        http({
          method: 'POST',
          url: url,
          data: reqData,
          headers: {
             'Content-Type': 'application/json'
           },
           timeout: 8000
        }).then(function successCallback(response) {
            callback(response.status, response.data);
        }, function errorCallback(response) {
            callback(response.status, response.data);
        });

    };

    this.doPut = function (http, url, reqData, callback) {

        http({
          method: 'PUT',
          url: url,
          data: reqData,
          headers: {
             'Content-Type': 'application/json'
           },
           timeout: 8000
        }).then(function successCallback(response) {
            callback(response.status, response.data);
        }, function errorCallback(response) {
            callback(response.status, response.data);
        });

    };

    this.doPatch = function (http, url, reqData, callback) {

        http({
          method: 'PATCH',
          url: url,
          data: reqData,
          headers: {
             'Content-Type': 'application/json'
           },
           timeout: 8000
        }).then(function successCallback(response) {
            callback(response.status, response.data);
        }, function errorCallback(response) {
            callback(response.status, response.data);
        });

    };

    this.doDelete = function (http, url, callback) {

        http({
          method: 'DELETE',
          url: url,
          timeout: 8000
        }).then(function successCallback(response) {
            callback(response.status, response.data);
        }, function errorCallback(response) {
            callback(response.status, response.data);
        });

    };

});
