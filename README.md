# sMockin Version 1.5.0

Intelligent API simulation for application development and QA testing

    Visit us: http://www.smockin.com

    Contact: info@smockin.com


OVERVIEW

    sMockin is a light framework used for setting up API simulations in a bid to aid development
    and QA testing in the ever growing realm of the 'Internet of Things'.

    Whether you are a mobile developer who needs to simulate RESTFul endpoints or an engineer working
    with complex SOA/IoT infrastructure, sMockin can help by mimicking any services that would be
    otherwise too difficult or time consuming to set up.

    With the ability to vary content using easy to set up sequences and rules, sMockin can help
    simulate all of your use case scenarios.

    Written in Java using Spring Boot, the application runs as a small web app which can be hosted either 
    locally on a developers machine or centrally on a development teams' server.

    Features include:

        - Internal Mocking Servers used to serve up your simulations.

        - An Admin UI for:
            - Creating and editing your mocked endpoints.
            - Applying 'rules' or 'sequenced responses' to your mocked endpoints.
            - Configuring, Starting and Stopping the Internal Mocking Server.

        - Support for:
               - REST
               - Web Sockets
               - SSE
               - JMS
               - FTP



REQUIREMENTS

    - Java 8
    - Maven 3

    Please Note
        -   All bash scripts were written and tested on GNU Bash version 3.2.57(1)-release.
        -   All BAT files were were written and tested on Windows 7.



QUICK START

    Linux / OSX

        - run install.sh
        - run start.sh
        - (run shutdown.sh when you wish to terminate the application)

    Windows

        - run install.bat
        - run start.bat
        - (Simply close both Command Prompt windows, when you wish to terminate the application)


    - Open the Admin UI: http://localhost:8000/index.html

    - Select the 'TCP' tab.

    - Click 'New TCP Endpoint' and enter the following details:
        Path:    /hello
        Method:  GET

    - Next select the 'HTTP sequenced Based' response type, click 'Add Seq Response' and add the following:
        Content Type:       application/json
        HTTP Status Code:   200
        Response Body:      { "message" : "hello world" }

    - Click 'Add' and then 'Save'.

    - From the main TCP dashboard click on the 'Play' symbol to start the mock server.

    - Finally to test the service, send a request to http://localhost:8001/hello
        e.g. 
            curl -i -X GET http://localhost:8001/hello


    Further information on running, configuring and trouble shooting sMockin, can be found within the
    README.txt file bundled within this project.



TROUBLESHOOTING

    sMockin's log files can be found here within your user home directory: .smockin/log/smockin.log

    If you are struggling to get sMockin to run using the provided scripts, then we recommend running it
    from source instead.

    e.g

        git clone https://github.com/mgtechsoftware/smockin.git

        mvn3 clean install

        ./install.sh

        To start sMockin using the in-mem DB:
            mvn3 spring-boot:run

        Or to run sMockin with remote debugging (port 8008):
            ./start.sh -DEBUG



UPDATING TO LATER VERSIONS

    Moving from an older version of sMockin to a later version is simple.

    All config and DB data is stored under the settings dir '.smockin' located under your user home.

    Therefore as long as this settings dir is preserved then updating sMockin is simply a case of deleting
    the current application and replacing this with a later version.

    The bundled 'start' script will always automatically handle any data migration that may need to take place.

    (Please note the above text, is only applicable to mainline 'master' releases of sMockin where neither the
    code base or database of the previous/outgoing version have been subject to personal / customised changes).



LICENCE


    sMockin is licensed according to the terms of the Apache License, Version 2.0.

    The full text of this license can be found at https://www.apache.org/licenses/LICENSE-2.0



ACKNOWLEDGEMENTS / THIRD PARTIES

    sMockin is built upon the following open source frameworks:

        Spring Boot                 -       https://projects.spring.io/spring-boot
        Hibernate                   -       http://hibernate.org
        Spark                       -       http://sparkjava.com
        Maven                       -       https://maven.apache.org
        AngularJS                   -       https://angularjs.org
        UI Bootstrap                -       https://angular-ui.github.io/bootstrap
        vkBeautify                  -       https://github.com/vkiryukhin/vkBeautify
        H2                          -       http://www.h2database.com
        HikariCP                    -       https://brettwooldridge.github.io/HikariCP
        Apache Commons              -       https://commons.apache.org/
        Apache Http Components      -       https://hc.apache.org/
        Apache Commons IO           -       https://commons.apache.org/proper/commons-io/
        JUnit                       -       http://junit.org
        Mockito                     -       http://site.mockito.org/
        Apache ActiveMQ             -       http://activemq.apache.org/
        Apache FtpServer            -       https://mina.apache.org/ftpserver-project/



ABOUT

    sMockin is designed and actively maintained by MG Tech Software Ltd.

    Why the name sMockin? Whilst it may sound like an old english middle ages embroidery technique, the name
    actually came about more in relation to a classic Jim Carey movie quote 'Smokin'. We then added the extra
    letter to play on the concept of software mocking and there you have it. 
    
    Yes, way too much thought went into that...
