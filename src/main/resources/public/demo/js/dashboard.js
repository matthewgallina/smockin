
var dashboardApp = new Vue({
  el: '#dashboard-app',
  data: {
    dashboardHeading: 'sMockin Demo - Dashboard',
    logoutLabel: 'Sign out',
    addRecordButtonLabel: 'Add Record',
    stats: []
  },
  created: function() {

    if (getAuthSession() == null) {
        window.location.href = 'login.html';
        return;
    }

    this.stats = loadStatsData();
  },
  mounted: function() {
    initPage();
  },
  methods: {
    doOpenAddRecord: function () {

    },
    doLogout: function () {
        clearAuthSession();
        window.location.href = 'login.html';
    }
  }
});


function buildCharts(statData) {

    var BackgroundColors = [
        'rgba(255, 99, 132, 0.2)',
        'rgba(54, 162, 235, 0.2)',
        'rgba(255, 206, 86, 0.2)',
        'rgba(75, 192, 192, 0.2)'
    ];
    var BorderColors = [
        'rgba(255,99,132,1)',
        'rgba(54, 162, 235, 1)',
        'rgba(255, 206, 86, 1)',
        'rgba(75, 192, 192, 1)'
    ];

    var labels = [];
    var data = [];

    for (var i=0; i < statData.length; i++) {
        labels.push(statData[i].site);
        data.push(statData[i].employees);
    }

    var ctx = document.getElementById("bar-chart").getContext('2d');

    var myChart = new Chart(ctx, {
        type: 'bar',
        data: {
            labels: labels,
            datasets: [{
                data: data,
                backgroundColor: BackgroundColors,
                borderColor: BorderColors,
                borderWidth: 1
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
             legend: {
                display: false
             },
            plugins: {
                labels: {
                    render: 'value'
                }
            },
            scales: {
                yAxes: [{
                    ticks: {
                        beginAtZero:true
                    }
                }]
            }
        }
    });


    var ctx2 = document.getElementById("donut-chart").getContext('2d');

    var myDoughnutChart = new Chart(ctx2, {
        type: 'doughnut',
        data: {
            datasets: [{
                data: data,
                backgroundColor: BackgroundColors,
                borderColor: BorderColors,
                borderWidth: 1
            }],
            labels: labels
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                labels: [
                    {
                        render: 'label',
                        arc: true,
                        fontColor: '#000',
                        position: 'outside'
                    },
                    {
                        render: 'percentage'
                    }
                ]
            },
            legend: {
                display: false
            }
         }
    });

}

function loadStatsData() {

    if (getAuthSession() == null) {
        return;
    }

    var data = [];

    data.push({ "id" : "1", "name" : "Max" });
    data.push({ "id" : "2", "name" : "Mike" });
    data.push({ "id" : "3", "name" : "James" });
    data.push({ "id" : "4", "name" : "Pete" });
    data.push({ "id" : "5", "name" : "Sally" });

    return data;
}

function initPage() {

    if (getAuthSession() == null) {
        return;
    }

    var employeeData = [];

    // Load chart data
    var statsResponseHandler = function(status, data) {

        if (status != 200) {
            return;
        }

        employeeData.push({ "site" : "London", "employees" : 371 });
        employeeData.push({ "site" : "New York", "employees" : 437 });
        employeeData.push({ "site" : "Miami", "employees" : 241 });
        employeeData.push({ "site" : "Hong Kong", "employees" : 292 });

        buildCharts(employeeData);
    }


    statsResponseHandler(200, null);

}
