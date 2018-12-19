# sMockin version 1.6.0

Intelligent API simulation for application development and QA testing

    Visit us: https://www.smockin.com

    Contact: info@smockin.com



WHAT'S NEW IN VERSION 1.5

    User Accounts for your Team

        A single installation of sMockin can now be used across your development and QA teams
        with the introduction of user accounts. Each user can create and manage their own mocks
        which are served from their own unique context path. User's can also view, copy and access each
        others mock definitions.

        Please see the 'MULTI USER SUPPORT' section below for further details on how to enable
        this feature.

    RAML API Importer

        Following demand for this feature, you can now create HTTP mocks based on RAML API files by
        uploading them into sMockin using the new RAML Import facility.

        For those using other API Documentation libraries (API Blueprint, OAS, etc.), there are a
        number of online API conversion tools available for creating RAML files such as
        https://apimatic.io/transformer.

    PROXY SERVER

        sMockin can now be enabled as a proxy server and sit between your client and backend server stack.

        When run in this way, sMockin will intercept all requests and attempt to match this to any active 
        mock endpoints. Where a match is made, the proxy server will proceed to return the mock instead 
        to the caller.

        Please note this specific feature is still in BETA and only will work with mocks served from the root 
        context path of the mock server.

    LIVE LOGGING

        Monitor in real time all HTTP mock and proxy traffic heading in and out of the sMockin. 

        Useful for assesing the mock (or real, via proxy) data driving your client or application.

        HTTP activity can be viewed both through the UI and/or from a seperate log file kept in 
        the .smockin config directory (under your machine's user home).



OVERVIEW

    sMockin is a framework with a rich UI used for setting up API simulations and mocks in a bid to aid 
    development and QA testing in the ever growing realm of the 'Internet of Things'.

    Whether you are a mobile developer who needs to simulate RESTFul endpoints or an engineer working
    with complex SOA/IoT/microservice infrastructure, sMockin can help by mimicking any services that 
    would be otherwise too difficult or time consuming to set up.

    With the ability to vary content using easy to set up sequences and rules, sMockin can help
    simulate all of your use case scenarios.

    sMockin runs as a small web app which can be either installed locally to a developers machine 
    or hosted centrally with mutliple user accounts on a development / QA team server.



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



MULTI USER SUPPORT

    Once sMockin is installed, you can enable multi user mode by editing the 'app.properties' file under 
    the .smockin directory within your user home directory (i.e .smockin/app.properties) 
    From here add/amend the MULTI_USER_MODE argument setting this to true:

        MULTI_USER_MODE = TRUE

    Once saved, simply shutdown and then restart sMockin.

    On start up, you can verify this is enabled by looking for the below message in the terminal window:

        "The application is running in 'Multi User Mode'"

    Once running in this mode, the sMockin dashboard will only be accessible by logging in 
    with user credentials.

    You can login and begin creating other user accounts using the 'system admin' account:

        username: admin
        password: admin

    Please note any mocks that we're created prior to enabling 'Multi User Mode' will be
    automatically owned by the 'system admin' user.

    In 'multiple user mode', mock endpoints are uniquely distinguished between users by being served 
    from a context path based on the user's username. To give an example of this, say the user 'bob' 
    creates the mock HTTP endpoint 'GET /hello', once the mock server has been started this will be 
    accessed like so:

        curl -i -X GET http://localhost:8001/bob/hello

    The system admin user, however will continue to access their endpoints directly from the root 
    context path like so:

        curl -i -X GET http://localhost:8001/hello



RAML SUPPORT

    The RAML Importer can be found under the HTTP section of the dashboard and supports
    the following file types:

        - .raml
        - .zip

    The zip archive format should be used for any raml files that have external file
    references (i.e 'include'). 
    
    In order for the .zip archive to be accepted, it must be
    packed 'including' a root directory and with the .raml file located directly within the
    root dir like so:

    - your_raml_upload.zip
        - your_root_raml_dir
            - your_raml.api
            - some_dir_1
                - Foo.json
            - some_dir_2
                - Foo.raml

    (Please note an example of this can be found under src/test/resources/raml_200.zip)



PROXY SERVER MODE

    sMockin can be enabled as a proxy server, by clicking on 'edit settings' under the HTTP tab and then 
    enabling the option 'Enable Proxy Server (mock interceptor) on port 8010 (BETA)'.
    
    The proxy server is automatically run alongside the regular HTTP mock server, so simply click play 
    button to launch this.
    
    To see this in action, create mock of an existing or missing webservice (i.e /hello). 
    Once done, start the mock server and run the following call to point to the proxy server:

    curl -i -X GET http://yourserver/hello --proxy http://localhost:8010



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

    Therefore as long as this settings dir is preserved, then updating sMockin is simply a case of deleting
    the current application and replacing this with a later version.

    The bundled 'start' script will automatically handle any data migration that may need to take place.

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
        RAML Parser 2               -       https://github.com/raml-org/raml-java-parser
        Jasypt                      -       http://www.jasypt.org/
        Java-JWT                    -       https://github.com/auth0/java-jwt
        JWT-Decode                  -       https://github.com/auth0/jwt-decode
        JQuery                      -       https://jquery.com/
        Little Proxy                -       https://github.com/adamfisk/LittleProxy



ABOUT

    sMockin is designed and actively maintained by MG Tech Software Ltd.

    Why the name sMockin? Whilst it may sound like an old english middle ages embroidery technique, the name
    actually came about more in relation to a classic Jim Carey movie quote 'Smokin'. We then added the extra
    letter to play on the concept of software mocking and there you have it. 
    
    Yes, way too much thought went into that...
