Feature: XQ Karate wrapper POC

Scenario: use framework capabilities without Java.type in the feature
  * match xqFramework == 'xq-karate-support'
  * match projectName == 'consumer-demo'
  * match consumerNote == 'thin project config on top of xq base'
  * match baseUrl == 'https://httpbin.org'
  * def ping = xqEcho.ping('hello')
  * match ping == 'xq:hello'
  * def body = xqEcho.wrap('mochi')
  * match body == { name: 'mochi', source: 'xq-karate-support' }
  * match xqRequestId != null
