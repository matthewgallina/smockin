<p align="center">
  <img src="/public/image/smockin_logo.png" width=400 />
</p>

<p align="center">
  version 2.11.0
</p>

<br />

### Dynamic REST API simulation and for application development & QA testing

   - Visit us: https://www.smockin.com

   - Contact us or follow us on Twitter: <b>@smockin_com</b>

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
<br />


### RECENT RELEASE CHANGES

<br />

**New in version 2.10.0**

- The sMockin proxy server now supports multiple url destinations which can be mapped to specific paths.

For example, say you are running 3 independent microservices for each of the following services:

```
/v1/product   -> localhost:9001
/v1/customer  -> localhost:9002
/v1/address   -> localhost:9003
```

You can now add these 'Path to URL' mappings to sMockin and then decide whether swap out any traffic between these services and the end user with mocked responses using various strategies.

This can be highly useful for either temporarily modifying an existing API or adding (mocking) an entirely new endpoint.

<br />
<br />


**New in version 2.9**

- Added ability to run sMockin as a proxy server so this can sit between any application and the downstream server it would normally make API calls too.

- Proxy mode can be run in 2 ways:

**Option A** sMockin will attempt to find a mock first and only forward the request to your server downstream if a mock could not be found.

**Option B** sMockin will forward the request to the server downstream first and then try to find a suitable mock if the downstream server returns a 404.

- Please note, proxy mode is not available when running sMockin in 'multi user' mode.

<br />
<br />


**New in version 2.8**

- Added the ability to save Key / Value pair data which can be recalled in your mock responses.

To add Key / Value data, click on the drop down box list from the top right hand corner of the UI and select **Manage Key/Value Data**. 

To then reference your Key / Value data in your mock responses, this can be achieved as follows:

For **Response Variables** syntax:

```
$lookUpKvp(kvpKeyName)
```

Or if using a **Custom JavaScript** based mock:

```
lookUpKvp('kvpKeyName');
```

For greater flexibility, you can also reference inbound request values as part of your lookup like so:

**Response Variables** syntax:

```
$lookUpKvp($requestParameter(firstName))
```

**Custom JavaScript** syntax:

```
lookUpKvp(request.parameters.firstName);
```

<br />

- Changes to the syntax format used in **Response Variables**.

For example: 

```
${REQ_PARAM=firstName} is now be expressed as $requestParameter(firstName)
${REQ_HEAD=firstName} is expressed as $requestHeader(firstName)
${REQ_PARAM=firstName} is expressed as $requestParameter(firstName)
${PATH_VAR=firstName} is expressed as $pathVar(firstName)
${RANDOM_NUMBER=1 TO 10} is expressed as $randomNumber(1, 10) or $randomNumber(10)
${ISO_DATETIME} is expressed as $isoDatetime
${ISO_DATE} is expressed as $isoDate
${UUID} is expressed as $uuid

$requestBody has been added as a new option
```

More details to follow at https://help.smockin.com


<br />
<br />


**New in version 2.7**

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
<br/>


### ABOUT

sMockin is designed and actively maintained by Matthew Gallina.

Why the name sMockin? Whilst it may sound like an old english middle ages embroidery technique, the name actually came about more in relation to a classic Jim Carey movie quote 'Smokin'.

...and yes, far too much time and thought went into it.
