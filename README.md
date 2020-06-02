<p align="center">
  <img src="/public/image/smockin_logo.png" width=400 />
</p>

<p align="center">
  version 2.7.0
</p>

<br />

### Dynamic REST API simulation and for application development & QA testing

   - Visit us: https://www.smockin.com

   - Contact: info@smockin.com

<br />

**An Announcement** regarding the upcoming release of **version 2.8** of sMockin. 

This will feature the ability to save  Key / Value pair data which can be recalled in your mock responses.

Version 2.8 will also feature a change to the way **Response Variables** are expressed using a new syntax format. If you are currently using **Response Variables**, then you will need to modify any existing declarations inline with this new format.

For example, 

**${REQ_PARAM=firstName}** will now be expressed as **$requestParameter(firstName)**

More details to follow...

<br />
<br />

New in **version 2.7** of sMockin

- Introducing **'Stateful REST'** mocking. Mocked endpoints using this feature, can cache and manage JSON state based on the RESTful instructions they receive, helping to mimic 'real world' data behavior.

<br />

To give it a go, simply create a new mock (e.g /pets) selecting the 'Stateful REST' type and save.

<br />

Then run the following calls to see it immediately action:

> curl -i -X GET http://localhost:8001/pets

> curl -i -X POST http://localhost:8001/pets -d '{ "name" : "fido", "age" : 4, "type" : "DOG" }'

> curl -i -X POST http://localhost:8001/pets -d '{ "name" : "minty", "age" : 6, "type" : "CAT" }'

<br />

Your next call to GET /pets should return the following:

> curl -i -X GET http://localhost:8001/pets

```
[
  {
    "name": "fido",
    "age": 4,
    "type": "DOG",
    "id": "223af502-ae81-4274-9101-4886821ea823"
  },
  {
    "name": "minty",
    "age": 6,
    "type": "CAT",
    "id": "0a1c837a-8cd5-4a3c-b2e8-a519933e99d5"
  }
]
```

<br />

You can proceed to GET, PUT, PATCH and DELETE your mock using the generated id...

> curl -i -X GET http://localhost:8001/pets/0a1c837a-8cd5-4a3c-b2e8-a519933e99d5

> curl -i -X PUT http://localhost:8001/pets/0a1c837a-8cd5-4a3c-b2e8-a519933e99d5 -d '{ "id" : "0a1c837a-8cd5-4a3c-b2e8-a519933e99d5", "name" : "Minty", "age" : 7, "type" : "CAT" }'

> curl -i -X PATCH http://localhost:8001/pets/0a1c837a-8cd5-4a3c-b2e8-a519933e99d5 - d '{ "op" : "REPLACE", "path" : "/name", "value" : "Minty Mi" }'

> curl -i -X DELETE http://localhost:8001/pets/0a1c837a-8cd5-4a3c-b2e8-a519933e99d5

<br />



<br />

### OVERVIEW

sMockin is an API mocking tool used to dynamically simulate HTTP endpoints.

Featuring a rich UI and an inbuilt HTTP mock server, mocking your existing or new API model can be done quickly and without any coding or scripting.

Whether you are a developer who needs to simulate RESTFul endpoints or an engineer working with complex SOA / IoT / microservice infrastructure, sMockin can help by mimicking any services that are either unavailable or otherwise too difficult or time consuming to set up.

sMockin runs as a small web app which can be either installed locally onto a personal machine or hosted centrally and used by multiple users.

<br/>

![dashboard](https://raw.githubusercontent.com/mgtechsoftware/smockin/master/public/image/dashboard.png)

![client call](https://raw.githubusercontent.com/mgtechsoftware/smockin/master/public/image/client-call.png)

<br/>

### KEY FEATURES

* Create dynamic API mocks to mimic real world application behaviour.
* Run sMockin centrally and create user accounts for your team.
* Import / Export mocks to share between your team & version control.
* Monitor and log traffic going to the HTTP mock server.
* A complete UI solution requiring zero coding.


<br/>

### REQUIREMENTS

   - Java 8
   - Maven 3

   Please Note
      -   All bash scripts were written and tested on GNU Bash version 3.2.57(1)-release.
      -   All BAT files were were written and tested on Windows 7.

<br/>

### QUICK START

Clone this repo:

```
git clone https://github.com/mgtechsoftware/smockin.git
```

Change the current directory to **smockin** and build the project:

```
cd smockin

mvn clean install
```

Run the **install** and **start** scripts (for windows, use the equivalent .bat files):

```
./install.sh

./start.sh
```

Finally from your browser open the dashboard:

```
http://localhost:8000/index.html
```

(If running sMockin for the first time then please allow 20 - 30 secs for the app to fully start up.)

<br />

For further details, please consult the [Installation](https://github.com/mgtechsoftware/smockin/wiki/Installation) and [Getting Started](https://github.com/mgtechsoftware/smockin/wiki/API-Mock-Tutorial:-Getting-Started) guides in the Wiki. 

You can also check out the latest help guide at [help.smockin.com](https://help.smockin.com)

<br/>

### TROUBLESHOOTING

Please consult the [Configuration & Troubleshooting](https://github.com/mgtechsoftware/smockin/wiki/Configuration-&-Troubleshooting) guide.

<br/>

### LICENCE


sMockin is licensed in accordance with the terms of the Apache License, Version 2.0.

The full text of this license can be found at https://www.apache.org/licenses/LICENSE-2.0

<br/>

### ACKNOWLEDGEMENTS / THIRD PARTIES

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
        Code Mirror                 -       https://codemirror.net/
<br/>

### ABOUT

sMockin is designed and actively maintained by MG Tech Software Ltd.

Why the name sMockin? Whilst it may sound like an old english middle ages embroidery technique, the name actually came about more in relation to a classic Jim Carey movie quote 'Smokin'. We then added the extra letter to play on the concept of software mocking and there you have it. 

Yes, way too much thought went into that...
