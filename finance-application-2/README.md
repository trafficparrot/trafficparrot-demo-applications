This example application is used in the partial passthrough and proxy HTTP mocks demo of Traffic Parrot.

[![Watch the video](https://img.youtube.com/vi/booQROrGlPA/maxresdefault.jpg)](https://youtu.be/booQROrGlPA)


### Here are the details needed to replicate the demo

**API URL**

/stockquote

**Request matcher**

{{ equal (dataSource '.csv'
'SELECT symbol
FROM stocks.csv
WHERE symbol = :1'
request.query.symbol
single=true
default=false) request.query.symbol }}

**Mocked response template**

{
"Status": "SUCCESS",
"Name": "{{request.query.symbol}}",
"Symbol": "{{request.query.symbol}}",
"LastPrice": {{ dataSource '.csv' 'SELECT price FROM stocks.csv WHERE symbol = :1' request.query.symbol single=true default=false }}
}

**Real API proxy URL**

http://us-central1-traffic-parrot-production.cloudfunctions.net
