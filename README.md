# SMOCKIN Version 1.3.0

The Web Service Simulation Framework for application development and QA testing

http://www.smockin.com


OVERVIEW

    SMOCKIN is a light framework that can be used for setting up mock RESTful web service simulations.
    for use in either development or QA testing.

    Whether you are a mobile developer who needs to simulate a remote server or working with a complex SOA, 
    SMOCKIN can help by simulating any services that may be otherwise difficult / time consuming to set up.

    With the ability to vary the output of your web service by using easy to set up sequences and rules, 
    SMOCKIN can help to simulate all of your different use cases.

    Written in Java using Spring Boot, the application runs as a small web app which can be hosted either 
    locally on a developers machine or centrally on a development teams' server.

    Features include:

        - An Internal Mocking Server which is used to serve your mocked endpoints.

        - Admin UI for:
            - Creating and editing your mocked endpoints.
            - Applying 'rules' or 'sequenced responses' to your mocked endpoints.
            - Configuring, Starting and Stopping the Internal Mocking Server.



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

    - Create a new mock endpoint:
        - Path     =    /hello
        - Method   =    GET
        - Add a sequenced response type:
            - Content Type       =    application/json
            - HTTP Status Code   =    200
            - Response Body      =    { "message" : "hello world" }

    - Start / Restart the mock server
    
    - Send a request to http://localhost:8001/hello
        e.g. 
            curl -i -X GET http://localhost:8001/hello


    Further information on running, configuring and trouble shooting SMOCKIN, can be found within the README.txt file
    bundled within this project.



UPDATING SMOCKIN TO LATER VERSIONS

    Moving from an older version of SMOCKIN to a later version is simple.

    All config and DB data is stored under the settings dir '.smockin' located under your user home.

    Therefore as long as this settings dir is preserved then updating SMOCKIN is simply a case of deleting the
    current application and replacing this with a later version.

    The bundled 'start' script will always automatically handle any data migration that may need to take place.

    (Please note the above text, is only applicable to mainline releases of SMOCKIN and where neither the code base or
    database of the previous/outgoing version have been subject to local/customised changes).



LICENCE


    Smockin is licensed according to the terms of the Apache License, Version 2.0.

    The full text of this license can be found at https://www.apache.org/licenses/LICENSE-2.0



ACKNOWLEDGEMENTS / THIRD PARTIES

    SMOCKIN is built upon the following open source frameworks:

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



ABOUT

    SMOCKIN is designed and actively maintained by MG Tech Software Ltd.

    Why the name SMOCKIN? Whilst it may sound like an old english middle ages embroidery technique, the name
    actually came about more in relation to a classic Jim Carey movie quote 'Smokin'. We then added the extra
    letter to play on the concept of software mocking and there you have it. 
    
    Yes, way too much thought went into that...
