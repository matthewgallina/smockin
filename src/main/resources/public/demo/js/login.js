
Vue.prototype.$timeoutPromise = null;

var loginApp = new Vue({
  el: '#login-app',
  data: {
    loginLabel: 'sMockin Demo - Login',
    loginErrorMessage: null,
    usernamePlaceholder: 'Enter username...',
    passwordPlaceholder: 'Enter password...',
    username: null,
    password: null,
    loginButtonLabel : 'Login'
  },
  methods: {

    doLogin: function () {

        if (isBlank(this.username)) {
            this.showAlert('Please enter your username');
            return;
        }
        if (isBlank(this.password)) {
            this.showAlert('Please enter your password');
            return;
        }

        doAuthenticateUser(this.username, this.password, this.$http, this.showAlert);
    },

    showAlert: function (msg) {
      this.loginErrorMessage = msg;

      if (this.timeoutPromise != null) {
        clearTimeout(this.timeoutPromise);
      }

      this.timeoutPromise = setTimeout(this.hideAlert, 4000);
    },

    hideAlert: function () {
      this.loginErrorMessage = null;
    }

  }
});

function doAuthenticateUser(username, password, httpClient, showAlert) {

    httpClient.post('http://localhost:8001/auth', '', { params: { 'username' : username, 'password' : password }, headers: {'Content-Type': 'application/x-www-form-urlencoded' } })
        .then(response => {
            setAuthSession(response.body.token);
            window.location.href = 'dashboard.html';
        }, response => {

            if (response.status == 400) {
                showAlert('...');
                return;
            }

            showAlert('Access Denied. Please try again');
        });

}
