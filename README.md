# http-to-messaging-adapter
Accepts HTTP requests and transforms them into Kafka messages.

Example output:
```JSON
{
  "id": "UUID",
  "source": "/http-to-messaging-adapter",
  "type": "httpEnvelop",
  "specversion": "0.2",
  "time": "<TIME>",
  "schemaurl": null,
  "contenttype": "application/json",
  "data": {
    "header": { "<List of HTTP Header>" },
    "base64body": "<base64 encoded HTTP body>"
  },
  "route": [
    {
      "type": "topic",
      "id": "req-start",
      "timestamp": "<TIME>"
    }
  ],
  "istestmessage": false,
  "iserrormessage": false,
  "returntopic": "result",
  "adapterRequestUrl": "example.com:80/",
  "adapterRequestMethod": "POST"
}
```
The component accepts HTTP requests, holds them open and warps them into Kakfa messages. The messages can be processed on the way and a final component can execute the request. The executing component than has to return the HTTP response for the request. The response message which contains the HTTP response must include the attributes `correlationid` and `httpResponseStatus`. The `correlationid` is used to correlate the request message with the response. The data attribute contains the HTTP header and the base64 encoded body for both the request and the response.

# MICO Settings
- Kafka-enabled:true
- Port Mapping: Any port to the internal port 8081. For example 8081:8081. Protocol: HTTP
- Needs a connection in the UI to the HTTP interface of the target micoservice via the BACKEND_REST_API environment variable
