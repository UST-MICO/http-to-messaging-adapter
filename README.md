# http-to-messaging-adapter
Accepts HTTP requests and transforms them into Kafka messages.

Example output:
```JSON
{
  "id": "232fbcd7-795c-42a8-b3c7-17dd0ce2f8d2",
  "source": "/http-to-messaging-adapter",
  "type": null,
  "specversion": "0.2",
  "time": "2019-09-25T20:40:45.011+02:00",
  "schemaurl": null,
  "contenttype": null,
  "data": {
    "header": {
      "content-length": "4",
      "accept-language": "de,en-US;q=0.7,en;q=0.3",
      "cookie": "cookie",
      "host": "localhost:8081",
      "content-type": "text/plain;charset=UTF-8",
      "connection": "keep-alive",
      "headerkey": "headerValue",
      "accept-encoding": "gzip, deflate",
      "user-agent": "Browser",
      "accept": "*/*"
    },
    "base64body": "Qm9keQ=="
  },
  "correlationid": null,
  "createdfrom": null,
  "route": null,
  "routingslip": null,
  "istestmessage": false,
  "filteroutbeforetopic": null,
  "iserrormessage": false,
  "errormessage": null,
  "errortrace": null,
  "expirydate": null,
  "sequenceid": null,
  "sequencenumber": null,
  "sequencesize": null,
  "returntopic": "transform-request",
  "dataref": null,
  "subject": null,
  "adapterRequestUrl": "/testPath/testPath2?key=value&key1=value1",
  "adapterRequestMethod": "POST"
}
```

The response message needs following attributes `correlationid` and `httpResponseStatus`.
